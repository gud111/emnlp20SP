package cc.mallet.topics;

import java.io.*;
import java.util.*;
import java.text.*;

import cc.mallet.types.*;
import cc.mallet.util.*;

import gnu.trove.*;

public class MyTopicModelDiagnostics {

	int numTopics;
	int numTopWords;

	public static final int TWO_PERCENT_INDEX = 1;
	public static final int FIFTY_PERCENT_INDEX = 6;
	public static final double[] DEFAULT_DOC_PROPORTIONS = { 0.01, 0.02, 0.05, 0.1, 0.2, 0.3, 0.5 };

	/**  All words in sorted order, with counts */
	ArrayList<TreeSet<IDSorter>> topicSortedWords;
	
	/** The top N words in each topic in an array for easy access */
	String[][] topicTopWords;

	ArrayList<TopicScores> diagnostics; 

	LDAOrg model;
	Alphabet alphabet;

	int[][][] topicCodocumentMatrices;

	int[] numRank1Documents;
	int[] numNonZeroDocuments;
	int[][] numDocumentsAtProportions;

	// This quantity is used in entropy calculation
	double[] sumCountTimesLogCount;

	int[] wordTypeCounts;
	int numTokens = 0;

	public MyTopicModelDiagnostics (LDAOrg model, int numTopWords) {
		numTopics = model.getNumTopics();
		this.numTopWords = numTopWords;

		this.model = model;

		alphabet = model.getAlphabet();
		topicSortedWords = model.getSortedWords();

		topicTopWords = new String[numTopics][numTopWords];

		numRank1Documents = new int[numTopics];
		numNonZeroDocuments = new int[numTopics];
		numDocumentsAtProportions = new int[numTopics][ DEFAULT_DOC_PROPORTIONS.length ];
		sumCountTimesLogCount = new double[numTopics];

		diagnostics = new ArrayList<TopicScores>();

		for (int topic = 0; topic < numTopics; topic++) {

			int position = 0;
			TreeSet<IDSorter> sortedWords = topicSortedWords.get(topic);
                        
			// How many words should we report? Some topics may have fewer than
			//  the default number of words with non-zero weight.
			int limit = numTopWords;
			if (sortedWords.size() < numTopWords) { limit = sortedWords.size(); }

			Iterator<IDSorter> iterator = sortedWords.iterator();
			for (int i=0; i < limit; i++) {
				IDSorter info = iterator.next();
				topicTopWords[topic][i] = (String) alphabet.lookupObject(info.getID());
			}

		}

		collectDocumentStatistics();
			
		diagnostics.add(getCoherence());
		
	}

	public void collectDocumentStatistics () {

		topicCodocumentMatrices = new int[numTopics][numTopWords][numTopWords];
		wordTypeCounts = new int[alphabet.size()];
		numTokens = 0;

		// This is an array of hash sets containing the words-of-interest for each topic,
		//  used for checking if the word at some position is one of those words.
		TIntHashSet[] topicTopWordIndices = new TIntHashSet[numTopics];
		
		// The same as the topic top words, but with int indices instead of strings,
		//  used for iterating over positions.
		int[][] topicWordIndicesInOrder = new int[numTopics][numTopWords];

		// This is an array of hash sets that will hold the words-of-interest present in a document,
		//  which will be cleared after every document.
		TIntHashSet[] docTopicWordIndices = new TIntHashSet[numTopics];
		
		int numDocs = model.getData().size();

		// The count of each topic, again cleared after every document.
		int[] topicCounts = new int[numTopics];

		for (int topic = 0; topic < numTopics; topic++) {
			TIntHashSet wordIndices = new TIntHashSet();

			for (int i = 0; i < numTopWords; i++) {
				if (topicTopWords[topic][i] != null) {
					int type = alphabet.lookupIndex(topicTopWords[topic][i]);
					topicWordIndicesInOrder[topic][i] = type;
					wordIndices.add(type);
				}
			}
			
			topicTopWordIndices[topic] = wordIndices;
			docTopicWordIndices[topic] = new TIntHashSet();
		}

		int doc = 0;

		
		for (int dx=0; dx < model.getData().size(); dx++)
		{	
			FeatureSequence tokens = (FeatureSequence) model.getData().get(dx).getData();
			int []topics =  model.getDocTopics(dx);
			
			for (int position = 0; position < tokens.size(); position++) {
				int type = tokens.getIndexAtPosition(position);
				int topic = topics[position];

				numTokens++;
				wordTypeCounts[type]++;

				topicCounts[topic]++;
				
				if (topicTopWordIndices[topic].contains(type)) {
					docTopicWordIndices[topic].add(type);
				}
			}

			int docLength = tokens.size();

			if (docLength > 0) {
				int maxTopic = -1;
				int maxCount = -1;

				for (int topic = 0; topic < numTopics; topic++) {
					
					if (topicCounts[topic] > 0) {
						numNonZeroDocuments[topic]++;
						
						if (topicCounts[topic] > maxCount) { 
							maxTopic = topic;
							maxCount = topicCounts[topic];
						}

						sumCountTimesLogCount[topic] += topicCounts[topic] * Math.log(topicCounts[topic]);
						
						double proportion = (model.alpha + topicCounts[topic]) / ((model.tAlpha) + docLength);
						for (int i = 0; i < DEFAULT_DOC_PROPORTIONS.length; i++) {
							if (proportion < DEFAULT_DOC_PROPORTIONS[i]) { break; }
							numDocumentsAtProportions[topic][i]++;
						}

						TIntHashSet supportedWords = docTopicWordIndices[topic];
						int[] indices = topicWordIndicesInOrder[topic];

						for (int i = 0; i < numTopWords; i++) {
							if (supportedWords.contains(indices[i])) {
								for (int j = i; j < numTopWords; j++) {
									if (i == j) {
										// Diagonals are total number of documents with word W in topic T
										topicCodocumentMatrices[topic][i][i]++;
									}
									else if (supportedWords.contains(indices[j])) {
										topicCodocumentMatrices[topic][i][j]++;
										topicCodocumentMatrices[topic][j][i]++;
									}
								}
							}
						}
						
						docTopicWordIndices[topic].clear();
						topicCounts[topic] = 0;
					}
				}

				if (maxTopic > -1) {
					numRank1Documents[maxTopic]++;
				}
			}

			doc++;
		}
	}

	public int[][] getCodocumentMatrix(int topic) {
		return topicCodocumentMatrices[topic];
	}

	
	public TopicScores getTokensPerTopic(int[] tokensPerTopic) {
		TopicScores scores = new TopicScores("tokens", numTopics, numTopWords);

		for (int topic = 0; topic < numTopics; topic++) {
			scores.setTopicScore(topic, tokensPerTopic[topic]);
		}

		return scores;
	}

	


	public TopicScores getCoherence() {
        TopicScores scores = new TopicScores("coherence", numTopics, numTopWords);
        scores.wordScoresDefined = true;

		for (int topic = 0; topic < numTopics; topic++) {
			int[][] matrix = topicCodocumentMatrices[topic];

			double topicScore = 0.0;

			for (int row = 0; row < numTopWords; row++) {
				double rowScore = 0.0;
				double minScore = 0.0;
				for (int col = 0; col < row; col++) {
					double score = Math.log( (matrix[row][col] + model.beta) / (matrix[col][col] + model.beta) );
					rowScore += score;
					if (score < minScore) { minScore = score; }
				}
				topicScore += rowScore;
				scores.setTopicWordScore(topic, row, minScore);
			}

			scores.setTopicScore(topic, topicScore);
		}
		
		return scores;
	}

	public TopicScores getDocumentPercent(int i) {
        TopicScores scores = new TopicScores("allocation_count", numTopics, numTopWords);

		if (i > numDocumentsAtProportions[0].length) {
			System.err.println("Invalid proportion indices (max " + (numDocumentsAtProportions[0].length - 1) + ") : " + i);
			return scores;
		}

		for (int topic = 0; topic < numTopics; topic++) {
			scores.setTopicScore(topic, (double) numDocumentsAtProportions[topic][i] / numNonZeroDocuments[topic]);
		}

		return scores;
	}

	/** Low-quality topics may be very similar to the global distribution. */
	public TopicScores getDistanceFromCorpus() {

		int[] tokensPerTopic = model.tokensPerTopic;

		TopicScores scores = new TopicScores("corpus_dist", numTopics, numTopWords);
		scores.wordScoresDefined = true;

		for (int topic = 0; topic < numTopics; topic++) {

			double coefficient = (double) numTokens / tokensPerTopic[topic];

			double topicScore = 0.0;
			int position = 0;
			TreeSet<IDSorter> sortedWords = topicSortedWords.get(topic);

			for (IDSorter info: sortedWords) {
				int type = info.getID();
				double count = info.getWeight();

				double score = (count / tokensPerTopic[topic]) *
					Math.log( coefficient * count / wordTypeCounts[type] );

				if (position < numTopWords) {
					//System.out.println(alphabet.lookupObject(type) + ": " + count + " * " + numTokens + " / " + wordTypeCounts[type] + " * " + tokensPerTopic[topic] + " = " + (coefficient * count / wordTypeCounts[type]));
					scores.setTopicWordScore(topic, position, score);
				}
				
				topicScore += score;

				position++;
			}

			scores.setTopicScore(topic, topicScore);
		}

		return scores;
	}

	public String toString() {

		StringBuilder out = new StringBuilder();
		Formatter formatter = new Formatter(out, Locale.US);

		for (int topic = 0; topic < numTopics; topic++) {
			
			formatter.format("Topic %d", topic);

			for (TopicScores scores: diagnostics) {
				formatter.format("\t%s=%.4f", scores.name, scores.scores[topic]);
			}
			formatter.format("\n");

//			for (int position = 0; position < topicTopWords[topic].length; position++) {
//                if (topicTopWords[topic][position] == null) { break; }
//				
//				formatter.format("  %s", topicTopWords[topic][position]);
//				for(TopicScores scores: diagnostics) {
//					if (scores.wordScoresDefined) {
//						formatter.format("\t%s=%.4f", scores.name, scores.topicWordScores[topic][position]);
//					}
//				}
//				out.append("\n");
//			}
		}
	
		return out.toString();
	}

	

	public class TopicScores {
		public String name;
		public double[] scores;
		public double[][] topicWordScores;
		
		/** Some diagnostics have meaningful values for each word, others do not */
		public boolean wordScoresDefined = false;

		public TopicScores (String name, int numTopics, int numWords) {
			this.name = name;
			scores = new double[numTopics];
			topicWordScores = new double[numTopics][numWords];
		}

		public void setTopicScore(int topic, double score) {
			scores[topic] = score;
		}
		
		public void addToTopicScore(int topic, double score) {
			scores[topic] += score;
		}
		
		public void setTopicWordScore(int topic, int wordPosition, double score) {
			topicWordScores[topic][wordPosition] = score;
			wordScoresDefined = true;
		}
	}
	
	public static void main (String[] args) throws Exception {
		InstanceList instances = InstanceList.load(new File("/home/sdas/cord/EPTM/temp.mallet"));
		int numTopics = 10;
		LDAOrg model = new LDAOrg(numTopics, 5.0, 0.01);
		model.estimate (instances, 200, 50, 0, null, new Randoms());  // should be 1100
		
		MyTopicModelDiagnostics diagnostics = new MyTopicModelDiagnostics(model, 10);

		TopicScores topicscores = diagnostics.getCoherence();
		for (int tx=0; tx<topicscores.scores.length; tx++)
			System.out.println("Topic-"+tx+" "+topicscores.scores[tx]);
		
//		if (args.length == 3) {
//			PrintWriter out = new PrintWriter(args[2]);
//			out.println(diagnostics.toXML());
//			out.close();
//		}
	}
}