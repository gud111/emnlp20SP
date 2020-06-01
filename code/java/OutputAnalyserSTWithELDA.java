package cc.mallet.topics;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import cc.mallet.topics.MyTopicModelDiagnostics.TopicScores;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import ke.CompObj;
import ke.SingleRankScorer;

public class OutputAnalyserSTWithELDA {
	
	static Hashtable<String, Integer> getHTCounts(String htprefix, STWithELDA lda)
	throws Exception
	{
		
		Hashtable<String, Integer> htcounts = new Hashtable<String, Integer>();
		InstanceList continstances = lda.ilist;
		Alphabet alphabet = lda.getAlphabet();
		for (int ix=0; ix<continstances.size(); ix++)
		{
		
			FeatureSequence onedoctokens = (FeatureSequence)continstances.get(ix).getData();
			int docLen = onedoctokens.getLength();
			for (int si = 0; si < docLen; si++) 
			{
				int type = onedoctokens.getIndexAtPosition(si);
				String typestr = (String)alphabet.lookupObject(type);
				if (typestr.startsWith(htprefix) && !typestr.equals(htprefix))
				{
					Integer c = htcounts.get(typestr);
					if (c==null) c= 0;
					c++;
					htcounts.put(typestr, c);
				}
			}
		}
		return htcounts;
	}
	
	static void printDocCounts(STWithELDA lda, String outfile)
	throws Exception
	{
		Hashtable<String, Integer> counts = new Hashtable<String, Integer>();
		InstanceList continstances = lda.ilist;
		for (int ix=0; ix<continstances.size(); ix++)
		{
			String period= ComputeMoodTrends.getWeekString(lda.ilist.get(ix).getTarget()+"");
			
			Integer cnt = counts.get(period);
			if (cnt==null) cnt = 0;
			cnt ++;
			
			counts.put(period, cnt);
		}
		
		PrintWriter pw = new PrintWriter(new FileWriter(outfile), true);
		pw.println("#Period NumTweets");
		for (Enumeration<String> px = counts.keys(); px.hasMoreElements(); )
		{
			String period = px.nextElement();
			pw.println(period +" "+counts.get(period));
		}
		pw.close();
	}
	
	static Hashtable<String, double[]> getHTTopicExclusivityScores(STWithELDA lda, 
			Hashtable<String, Integer> frequencies)
	{
		Hashtable<String, double[]> toret = new Hashtable<String, double[]>();
		for (Enumeration<String> htx = frequencies.keys(); htx.hasMoreElements(); )
		{
			String htag = htx.nextElement();
			int typeindx = lda.getAlphabet().lookupIndex(htag, false);
			
			if (typeindx < 0) {
			//	System.out.println("Cannot find "+htag+" in alphabet");
				continue;
			}
			
			double []temp = new double[lda.numTopics];
			double sumprob = 0.0;
			for (int tx=0; tx<lda.numTopics; tx++)
			{
				sumprob += (lda.typeTopicCounts[typeindx][tx]+ lda.beta)/(lda.tokensPerTopic[tx]+ lda.vBeta);
			}
			for (int tx=0; tx<lda.numTopics; tx++)
			{
				double thisprob = (lda.typeTopicCounts[typeindx][tx]+ lda.beta)/(lda.tokensPerTopic[tx]+ lda.vBeta);
				temp[tx] = thisprob/sumprob;
					
			}	
				
			toret.put(htag, temp);
			
		}
		
		return toret;
	}
	
	static Hashtable<String, double[]> getHTEmoExclusivityScores(
			STWithELDA lda, 
			Hashtable<String, Integer> frequencies)
	throws Exception
	{
		Hashtable<String, double[]> toret = new Hashtable<String, double[]>();
		
		double []emohtcounts = new double[lda.numEmos];
		for (int ix=0; ix<lda.ilist.size(); ix++)
		{
			int emo = lda.gsdas_docsEmoAssignment[ix];
			FeatureSequence onedoctokens = (FeatureSequence)lda.ilist.get(ix).getData();
			int docLen = onedoctokens.getLength();
			for (int si = 0; si < docLen; si++) 
			{
				int type = onedoctokens.getIndexAtPosition(si);
				String typestr = (String)lda.getAlphabet().lookupObject(type);
				if (frequencies.containsKey(typestr))
				{
					
					double []temp = toret.get(typestr);
					if (temp==null)
					{
						temp = new double[lda.numEmos];						
					}
					temp[emo]++;
					toret.put(typestr, temp);
					emohtcounts[emo]++;
				}
			}		
		}
		
		for (Enumeration<String> htx= toret.keys(); htx.hasMoreElements(); )
		{
			String htag = htx.nextElement();
			double []temp = toret.get(htag);
			double sumtemp = 0;
			for (int tx=0; tx<lda.numEmos; tx++)
			{
				sumtemp += (temp[tx] + lda.emoAlpha)/(emohtcounts[tx] + lda.emotAlpha);
			}
			
			for (int tx=0; tx<lda.numEmos; tx++)
			{
				temp [tx] = ((temp[tx] + lda.emoAlpha)/(emohtcounts[tx] + lda.emotAlpha))/sumtemp;		
			}
			toret.put(htag, temp);
		}
		
		return toret;
	}
	
	static Vector<CompObj> getTopHTsByExclusivityForGivenID (Hashtable<String, double[]> exclscores, int lookupid)
	{
		
		Vector<CompObj> temp = new Vector<CompObj>();
		for (Enumeration<String> htx = exclscores.keys(); htx.hasMoreElements(); )
		{
			String htag = htx.nextElement();			
			double score = exclscores.get(htag)[lookupid];
			temp.add(new CompObj(htag, score));			
		}
		
		Collections.sort(temp); Collections.reverse(temp);
		return temp;
	}
	
	static Vector<CompObj> getTopHTsByCountsForGivenTopicID (
			String htprefix,			
			STWithELDA lda,
			int topicid)
	{
		
		Hashtable<String, Integer> htcounts = new Hashtable<String, Integer>();
		
		InstanceList instances = lda.ilist; 
		for (int ix=0; ix<instances.size(); ix++)
		{
			if (lda.gsdas_docsTopicAssignment[ix]!=topicid)
				continue;
			
			FeatureSequence onedoctokens = (FeatureSequence)instances.get(ix).getData();
			int docLen = onedoctokens.getLength();
			for (int si = 0; si < docLen; si++) 
			{
				int type = onedoctokens.getIndexAtPosition(si);
				String typestr = (String)lda.getAlphabet().lookupObject(type);
				if (typestr.startsWith(htprefix) && !typestr.equals(htprefix))
				{
					Integer c = htcounts.get(typestr);
					if (c==null) c= 0;
					c++;
					htcounts.put(typestr, c);
				}
			}
		}
		
		Vector<CompObj> temp = new Vector<CompObj>();
		for (Enumeration<String> htx = htcounts.keys(); htx.hasMoreElements(); )
		{
			String htag = htx.nextElement();			
			double score = htcounts.get(htag);
			temp.add(new CompObj(htag, score));			
		}
		
		Collections.sort(temp); Collections.reverse(temp);
		return temp;
		
		
	}
	
	static Vector<CompObj> getTopHTsByCountsForGivenEmoID (
			String htprefix,			
			STWithELDA lda,
			int emoid)
	{
		
		Hashtable<String, Integer> htcounts = new Hashtable<String, Integer>();
		
		InstanceList instances = lda.ilist; 
		for (int ix=0; ix<instances.size(); ix++)
		{
			if (lda.gsdas_docsEmoAssignment[ix]!=emoid)
				continue;
			
			FeatureSequence onedoctokens = (FeatureSequence)instances.get(ix).getData();
			int docLen = onedoctokens.getLength();
			for (int si = 0; si < docLen; si++) 
			{
				int type = onedoctokens.getIndexAtPosition(si);
				String typestr = (String)lda.getAlphabet().lookupObject(type);
				if (typestr.startsWith(htprefix) && !typestr.equals(htprefix))
				{
					Integer c = htcounts.get(typestr);
					if (c==null) c= 0;
					c++;
					htcounts.put(typestr, c);
				}
			}
		}
		
		Vector<CompObj> temp = new Vector<CompObj>();
		for (Enumeration<String> htx = htcounts.keys(); htx.hasMoreElements(); )
		{
			String htag = htx.nextElement();			
			double score = htcounts.get(htag);
			temp.add(new CompObj(htag, score));			
		}
		
		Collections.sort(temp); Collections.reverse(temp);
		return temp;
		
		
	}
	
	
	static void printKP(String workdir, int topicid, String htprefix, STWithELDA lda) 
	{
	  SingleRankScorer scorer = new
	  SingleRankScorer("/home/sdas/pagerank/pagerank", 0.85, 0, 2, workdir);
	 
	  MaxentTagger postagger = new MaxentTagger("/home/sdas/setups/stanford-postagger-full-2018-10-16/models/english-bidirectional-distsim.tagger");

	  ////	/////////////
	  //
	  String contTempf = workdir +"/temp.content.txt";


	  String postaggedTempf = workdir +"/temp.content.pos.txt";
	  try {

		  lda.getContentForTopic(topicid, 1000, htprefix, contTempf);
		  scorer.runPOSTagger(postagger, contTempf, postaggedTempf);

		  Vector<CompObj> scoredkps = scorer.extractKeyphrases(postaggedTempf, 2, 5);
		  System.out.println("Topic "+topicid);
		  for (int kx=0; kx<scoredkps.size(); kx++)
		  {
			  System.out.println(scoredkps.get(kx));
		  }

	  }catch(Exception e)
	  {
		  System.out.println(e);
	  }

	}

	
	
	
	
	
	public static void main(String []args)throws Exception
	{
		
		String stopwordsf = "/home/sdas/setups/mallet-2.0.8/stoplists/en.txt";
		String workdir = "/home/sdas/cord/covidtweets/panacea/may12emorun/work";
		String outfile = "/home/sdas/submissions/20emnlpshort/results/numdocsperweek.dat";
		String analmodelf = "/home/sdas/cord/covidtweets/panacea/newemnlprun/work/lda.model.dat.10.60";
		String htprefix="gsdasht";
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(analmodelf));
		
		STWithELDA lda = STWithELDA.readObject(ois);
		
		ois.close();
		
		System.out.println("#words in vocabulary "+lda.getAlphabet().size());
		System.out.println("#emojis in vocabulary "+lda.emodict.size());
		
//		double []ecoh = lda.getEmoCoherence(5);
//		Vector<CompObj> sortedemos = new Vector<CompObj>();
//		
//		for (int ex=0; ex<ecoh.length; ex++)
//		{
//			sortedemos.add(new CompObj(ex, ecoh[ex]));
//		}
//		
//		Collections.sort(sortedemos);
//		Collections.reverse(sortedemos);
//		for (int ex=0; ex<ecoh.length; ex++)
//		{
//			System.out.println(sortedemos.get(ex));
//		}
		
//		lda.getPurity2(50, htprefix);
//		
//		System.exit(1);
		
//		printDocCounts(lda, outfile);
		
//		System.out.println("Num topics "+lda.numTopics+" num emos "+lda.numEmos);
//		lda.printTopEmoWords (5, false);
//		PrintWriter pw = new PrintWriter(new FileWriter(outfile), true);
//		pw.println("#EmoId NumDocs");
//		for (int tx=0;  tx<lda.numEmos; tx++)
//		{
//			pw.println(tx+" "+lda.gsdas_docsPerEmos[tx]);
//		}
//		pw.close();
//		

//		MyTopicModelDiagnostics td = new MyTopicModelDiagnostics(lda, 10);
//		double []scores = td.getCoherence().scores; 
//////		//getDistanceFromCorpus(); // 
////		
//		Vector<CompObj> sortedtopics = new Vector<CompObj>();
//		
//		for (int tx=0; tx<lda.numTopics; tx++)
//		{
//			sortedtopics.add(new CompObj(tx, scores[tx]));
//		}
//		
//		Collections.sort(sortedtopics); Collections.reverse(sortedtopics);
//
////		int []topicids = {53, 10, 6, 2, 4, 36};
////		
////		for (int tx=0; tx<sortedtopics.size(); tx++)
////		{
////			CompObj topval = sortedtopics.get(tx);
////			int topicid = new Integer(topval.theobject.toString());
////			
////			for (int tx1=0; tx1<topicids.length; tx1++)
////			{
////				if (topicids[tx1]==topicid)
////				{
////					System.out.println("Coh Rank "+(tx)+ " "+topicid+" Cluster sz "+(lda.gsdas_docsPerTopics[topicid]/(double)lda.ilist.size()));
////					break;
////				}
////			}
////			
////		}
////		
////		System.exit(1);
//		for (int tx=0; tx<lda.numTopics/20; tx++)
//		{
//			CompObj topval = sortedtopics.get(tx);
//			int topicid = new Integer(topval.theobject.toString());
//			System.out.println(topval+" Cluster sz "+(lda.gsdas_docsPerTopics[topicid]/(double)lda.ilist.size()));
//			lda.printTopWords(10, topicid);
//			Vector<CompObj> scoredhts = getTopHTsByCountsForGivenTopicID(htprefix, lda, topicid);
//			for (int cx=0; cx<5 && cx<scoredhts.size(); cx++)
//			{
//				CompObj ht = scoredhts.get(cx);
//				System.out.println(ht.theobject.toString().replaceAll(htprefix,"#")+" "+ht.score);
//			}
//		}
//		
//		sortedtopics = new Vector<CompObj>();
//		
//		for (int tx=0; tx<lda.numTopics; tx++)
//		{
//			sortedtopics.add(new CompObj(tx, new Double(lda.gsdas_docsPerTopics[tx])));
//		}
//		
//		Collections.sort(sortedtopics); Collections.reverse(sortedtopics);
//System.out.println();
//		for (int tx=0; tx<lda.numTopics/20; tx++)
//		{
//			CompObj topval = sortedtopics.get(tx);
//			int topicid = new Integer(topval.theobject.toString());
//			System.out.println(topval+" Cluster sz "+(lda.gsdas_docsPerTopics[topicid]/(double)lda.ilist.size()));
//			lda.printTopWords(10, topicid);
//			Vector<CompObj> scoredhts = getTopHTsByCountsForGivenTopicID(htprefix, lda, topicid);
//			for (int cx=0; cx<5 && cx<scoredhts.size(); cx++)
//			{
//				CompObj ht = scoredhts.get(cx);
//				System.out.println(ht.theobject.toString().replaceAll(htprefix,"#")+" "+ht.score);
//			}
//		}
//		
//		System.out.println();		
//		Collections.sort(sortedtopics);
//		
//		for (int tx=0; tx<lda.numTopics/20; tx++)
//		{
//			CompObj topval = sortedtopics.get(tx);
//			int topicid = new Integer(topval.theobject.toString());
//			System.out.println(topval+" Cluster sz "+(lda.gsdas_docsPerTopics[topicid]/(double)lda.ilist.size()));
//			lda.printTopWords(10, topicid);
//			Vector<CompObj> scoredhts = getTopHTsByCountsForGivenTopicID(htprefix, lda, topicid);
//			for (int cx=0; cx<5 && cx<scoredhts.size(); cx++)
//			{
//				CompObj ht = scoredhts.get(cx);
//				System.out.println(ht.theobject.toString().replaceAll(htprefix,"#")+" "+ht.score);
//			}
//		}
		
		
		//		
		
		//Hashtable<String, Integer> freq = getHTCounts(htprefix, lda);
////		Hashtable<String, double[]> exclscores_topic = getHTTopicExclusivityScores(lda, freq);
//
//		int top10pct = 6;
//		int []topicids = {53, 10, 6, 41, 43, 11}; 
		int []topicids = {53, 10, 6, 2, 4, 36};
		String []moodnames= {
				"Disgust", "Disgust/Anger", "Joy-1", "None", "None",
				"None", "Joy-2", "Sadness", "Joy-3", "None"
		};
		int aggtypes=4;
		int []agginds= {
				0, 0 , 1, 2, 2, 
				2, 1, 3, 1, 2
		};
		String []aggnames = { "Disgust/Anger", "Joy", "Other", "Sadness"};
		System.out.println("TopicID\tNumDocs\tDisgust/Anger\tJoy\tOther\tSadness");
		for (int tx=0; tx<topicids.length; tx++)
		{
			int topicid = topicids[tx]; //new Integer(sortedtopics.get(tx).theobject.toString());
			
			int []counts = new int[aggtypes+1];	
			
			counts[aggtypes] = lda.gsdas_docsPerTopics[topicid];
			
			for (int ix=0; ix<lda.ilist.size(); ix++)
			{
				if (lda.gsdas_docsTopicAssignment[ix]==topicid)
				{
					int emoid = lda.gsdas_docsEmoAssignment[ix]; 
					counts[agginds[emoid]]++;
				}
				
				
			}
			
			String top="";
			for (int cx=0; cx<counts.length-1; cx++)
			{
				top +="\t"+(counts[cx]/(double)counts[aggtypes])*100;
			}
			
			System.out.println(topicid+"\t"+counts[aggtypes]+"\t"+top.trim());
			
////			lda.printTopWords(10, topicid);
//			System.out.println();
			
//			System.out.println("By exclusivity");

			
		}
		
		
		
//		
//		String []moodnames= {
//				"None", "Joy/Cheering", "Joy", "None", "None",
//				"Anger/Disgust", "Disgust/Anger", "None", "None", "Sadness"
//		};
//		
//		Hashtable<String, double[]> exclscores_emo = getHTEmoExclusivityScores(lda, freq);
//		for (int ex=0; ex<lda.numEmos; ex++)
//		{
//			
//			if (!moodnames[ex].equals("None"))
//			{
//				System.out.println("Mood "+moodnames[ex]);
//				Vector<CompObj> scoredhts = getTopHTsByCountsForGivenEmoID(htprefix, lda, ex);
//						//getTopHTsByExclusivityForGivenID(exclscores_emo, ex);
//
//				lda.printTopEmoWords(5, ex);
//				for (int cx=0; cx<5 && cx<scoredhts.size(); cx++)
//				{
//					CompObj ht = scoredhts.get(cx);
//					System.out.println(ht.theobject.toString().replaceFirst(htprefix,"#")+" "+ht.score);
//				}
//				System.out.println();
//			}
//		}
//		
//		
		
		
		
//		lda.printTopEmoWords (5, false);
//		
//		System.out.println("purity 1 "+lda.getPurity(50, "ht"));
//		System.out.println("purity 2 "+lda.getPurity2(50, "ht"));
//	
//		
//		
		
		
		
		
		
//Hashtable<String, Integer> freq = getHTCounts(contmalletf);
		
//		Hashtable<String, double[]> exclscores_topic = getHTTopicExclusivityScores(lda, freq);
//		
//		Vector<CompObj> scoredhts = getTopHTsForGivenID (exclscores_topic, 16);
//		for (int cx=0; cx<10 && cx<scoredhts.size(); cx++)
//			System.out.println(scoredhts.get(cx));
		
//		Hashtable<String, double[]> exclscores_emo = getHTEmoExclusivityScores(lda, freq);
//		
//		
//		for (int ex=0; ex<lda.numEmos; ex++)
//		{
//			Vector<CompObj> scoredhts = getTopHTsForGivenID(exclscores_emo, ex);
//			
//			System.out.println("Emotion "+ex);
//			for (int cx=0; cx<10 && cx<scoredhts.size(); cx++)
//				System.out.println(scoredhts.get(cx));
//			
//			System.out.println();
//		
//		}
//		
//		int numTopEmoji = 5;
//		double aggecoh=0.0;
//		double []ecoh = lda.getEmoCoherence(numTopEmoji);
//		for (int ex=0; ex<ecoh.length; ex++)
//		{
//			System.out.println("Emo-"+ex+" "+ecoh[ex]);
//			aggecoh += ecoh[ex];			
//		}
//		System.out.println("Aggregate Emo coherence "+aggecoh/lda.numEmos);
//	//	lda.printTopWords (10, false);
//		lda.printTopEmoWords (5, false);
//		
//		
//		String contTempf = workdir +"/temp.content.txt";
//		//lda.getContentForTopic(16, lda.continfo, contTempf);
//		lda.getContentForEmo(3, contTempf);
//		Vector<CompObj> scoredhts = getHTs(contTempf); 
//		for (int cx=0; cx<10 && cx<scoredhts.size(); cx++)
//			System.out.println(scoredhts.get(cx));
	}
	
	
}
