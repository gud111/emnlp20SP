/* Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package cc.mallet.topics;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.io.*;

import cc.mallet.topics.MyTopicModelDiagnostics.TopicScores;
import cc.mallet.types.*;
import cc.mallet.util.ArrayUtils;
import cc.mallet.util.Randoms;

import ke.CompObj;
import ke.SingleRankScorer;
import ke.params.SingleRankParams;

/**
 * Latent Dirichlet Allocation.
 * @author Andrew McCallum
 * @deprecated Use ParallelTopicModel instead.
 */

// Think about support for incrementally adding more documents...
// (I think this means we might want to use FeatureSequence directly).
// We will also need to support a growing vocabulary!

public class STWithELDA extends LDAOrg implements Serializable {


	
	int []gsdas_docsPerTopics;
	int []gsdas_docsTopicAssignment;
	
	int []gsdas_docsPerEmos;
	int []gsdas_docsEmoAssignment;
	int [][]gsdas_type2EmoCounts;
	int []gsdas_tokens2PerEmo;
	
	int numEmos;
	int numTypes2;
	int numTokens2;

	double emoAlpha;
	double emotAlpha;
	double emoBeta;
	double emovBeta;
	
	Hashtable<Integer, Vector<String>> emoinfo;
	Hashtable<Integer, String> continfo;
	Hashtable<String, Integer> emodict;
	Hashtable<Integer, String> r_emodict;
	
	public STWithELDA (int numberOfTopics)
	{
		this(numberOfTopics, 0.1, 0.1, 8, 0.1, 0.1);
		System.out.println("Inside STLDA constructor");
	}

	public STWithELDA (int numberOfTopics, int numberOfEmotions)
	{
		this(numberOfTopics, 0.1, 0.1, numberOfEmotions, 0.1, 0.1);
		System.out.println("Inside STLDA constructor");
	}
	
	public STWithELDA (int numberOfTopics, double alphaSum, double beta,
			int numberOfEmos, double emoAlphaSum, double emoBeta)
	{
		
		super(numberOfTopics, alphaSum, beta);
		
		numEmos = numberOfEmos;
		this.emoAlpha = emoAlphaSum / numEmos;
		this.emoBeta = emoBeta;
		
	}

	public void estimate2 (InstanceList documents, 
			Hashtable<Integer, Vector<String>> inst2emowords, Hashtable<String, Integer> emoDict,
			Hashtable<Integer, String> r_emoDict, Hashtable<Integer, String> inst2contentinfo,
			int numIterations, int showTopicsInterval,
                        int outputModelInterval, String outputModelFilename,
                        Randoms r)
	{
		System.out.println("Inside STLDA.estimate GSDAS 1");
		ilist = documents.shallowClone();
		numTypes = ilist.getDataAlphabet().size ();
		int numDocs = ilist.size();
	
		typeTopicCounts = new int[numTypes][numTopics];
		tokensPerTopic = new int[numTopics];
		gsdas_docsPerTopics = new int[numTopics];
		gsdas_docsTopicAssignment = new int[numDocs];
		tAlpha = alpha * numTopics;
		vBeta = beta * numTypes;

		numTypes2 = emoDict.size();
		gsdas_type2EmoCounts = new int[numTypes2][numEmos];
		gsdas_tokens2PerEmo = new int[numEmos];
		gsdas_docsPerEmos = new int[numEmos];
		gsdas_docsEmoAssignment = new int[numDocs];
		emotAlpha = emoAlpha * numEmos;
		emovBeta = emoBeta * numTypes2;
		
		emoinfo = inst2emowords;
		emodict = emoDict;
		r_emodict = r_emoDict;
		continfo = inst2contentinfo;
		
		long startTime = System.currentTimeMillis();

		// Initialize with random assignments of tokens to topics
		// and finish allocating this.topics and this.tokens
		int topic, seqLen, emo, eseqLen;
    FeatureSequence fs;
    Vector<String> emofs;
    for (int di = 0; di < numDocs; di++) {
      try {
        fs = (FeatureSequence) ilist.get(di).getData();
        emofs = inst2emowords.get(di);
      } catch (ClassCastException e) {
        System.err.println ("STLDA and other topic models expect FeatureSequence data, not FeatureVector data.  "
                            +"With text2vectors, you can obtain such data with --keep-sequence or --keep-bisequence.");
        throw e;
      }
      		seqLen = fs.getLength();
			numTokens += seqLen;
		
			// Randomly assign tokens to topics
			topic = r.nextInt(numTopics);
			gsdas_docsPerTopics[topic]++;
			gsdas_docsTopicAssignment[di]=topic;
			tokensPerTopic[topic] += seqLen;
			for (int si = 0; si < seqLen; si++) {
			
				typeTopicCounts[fs.getIndexAtPosition(si)][topic]++;
				
			}
			//for the emotions part
			eseqLen = emofs.size();
			numTokens2 += eseqLen;
		
			// Randomly assign tokens to emos
			emo = r.nextInt(numEmos);
			gsdas_docsPerEmos[emo]++;
			gsdas_docsEmoAssignment[di]=emo;
			gsdas_tokens2PerEmo[emo] += eseqLen;
			for (int si = 0; si < eseqLen; si++) {
			
			//	System.out.println("GSDAS DEBUG "+emofs.get(si));
				Integer emtypeid = emoDict.get(emofs.get(si));
			//	System.out.println("GSDAS DEBUG "+emtypeid+" "+emo);
				gsdas_type2EmoCounts[emtypeid][emo]++;
				
			}
			/* START GSDAS DEBUG
			if (di<10)
			{
				System.out.println("Inside estimate GSDAS , topic assigned to di="+di+" is "+topic);
				System.out.println("gsdas_docsPerTopics,  topic "+topic+" "+gsdas_docsPerTopics[topic]);
				System.out.println("sdas_tokensPerTopic  topic "+topic+" "+tokensPerTopic[topic]);
				
				System.out.println("emo assigned to di="+di+" is "+emo);
				System.out.println("gsdas_docsPerEmos,  emo "+emo+" "+gsdas_docsPerEmos[emo]);
				System.out.println("gsdas_tokens2PerEmo emo "+emo+" "+gsdas_tokens2PerEmo[emo]);
			} END GSDAS DEBUG*/
			
		}
    
    /* START GSDAS DEBUG
    for (int tx=0; tx<numTopics; tx++)
    {
		System.out.println("gsdas_docsPerTopics before estimate,  topic "+tx+" "+gsdas_docsPerTopics[tx]);
    }
    
    for (int tx=0; tx<numEmos; tx++)
    {
		System.out.println("gsdas_docsPerEmos before estimate,  emo "+tx+" "+gsdas_docsPerEmos[tx]);
    }  END GSDAS DEBUG*/
    
    
    this.estimate(0, numDocs, numIterations, showTopicsInterval, outputModelInterval, outputModelFilename, r);
		// 124.5 seconds
		// 144.8 seconds after using FeatureSequence instead of tokens[][] array
		// 121.6 seconds after putting "final" on FeatureSequence.getIndexAtPosition()
		// 106.3 seconds after avoiding array lookup in inner loop with a temporary variable

	}
	
	
	
	/* Perform several rounds of Gibbs sampling on the documents in the given range. */ 
	public void estimate (int docIndexStart, int docIndexLength,
	                      int numIterations, int showTopicsInterval,
                        int outputModelInterval, String outputModelFilename,
                        Randoms r)
	{
		System.out.println("Inside STLDA.estimate GSDAS 2");
		long startTime = System.currentTimeMillis();
		for (int iterations = 0; iterations < numIterations; iterations++) {
			if (iterations % 10 == 0) System.out.print (iterations);	else System.out.print (".");
			System.out.flush();
			if (showTopicsInterval != 0 && iterations % showTopicsInterval == 0 && iterations > 0) {
				System.out.println ();
				printTopWords (5, false);
				printTopEmoWords(5, false);
			}
			if (outputModelInterval != 0 && iterations % outputModelInterval == 0 && iterations > 0) {
				this.write (new File(outputModelFilename+'.'+iterations));
			}

			/* START GSDAS DEBUG
			if (iterations%50==0)
			{
				System.out.println("\nInside estimate iter="+iterations);
				for (int tx=0; tx<numTopics; tx++)
				{
					System.out.println("gsdas_docsPerTopics,  topic "+tx+" "+gsdas_docsPerTopics[tx]);
				}
				for (int tx=0; tx<numEmos; tx++)
				{
					System.out.println("gsdas_docsPerEmos,  emo "+tx+" "+gsdas_docsPerEmos[tx]);
				}

				int []dindx=new int[] {ilist.size()/10, ilist.size()/20 , ilist.size()/50 };
				for (int dx=0; dx<dindx.length; dx++)
				{
					int ttopic = gsdas_docsTopicAssignment[dindx[dx]];
					int temo = gsdas_docsEmoAssignment[dindx[dx]];
					System.out.println("topic assigned to dindx="+dindx[dx]+" "+ttopic);
					System.out.println("emo assigned to dindx="+dindx[dx]+" "+temo);

				}
			} END GSDAS DEBUG*/

			sampleTopicsEmosForDocs(docIndexStart, docIndexLength, r);
		}

		long seconds = Math.round((System.currentTimeMillis() - startTime)/1000.0);
		long minutes = seconds / 60;	seconds %= 60;
		long hours = minutes / 60;	minutes %= 60;
		long days = hours / 24;	hours %= 24;
		System.out.print ("\nTotal time: ");
		if (days != 0) { System.out.print(days); System.out.print(" days "); }
		if (hours != 0) { System.out.print(hours); System.out.print(" hours "); }
		if (minutes != 0) { System.out.print(minutes); System.out.print(" minutes "); }
		System.out.print(seconds); System.out.println(" seconds");
		
		
		
	}

	
	public int[] getDocTopics(int dx)
	{
		int []temp = new int[((FeatureSequence) ilist.get(dx).getData()).getLength()];
		for (int tx=0; tx<temp.length; tx++)
		{
			temp[tx] = gsdas_docsTopicAssignment[dx];
		}
		return temp;
	}
	
	/* One iteration of Gibbs sampling, across all documents. */
	public void sampleTopicsEmosForAllDocs (Randoms r)
	{
		
		double[] topicWeights = new double[numTopics];
		double[] emoWeights = new double[numEmos];
		
		// Loop over every word in the corpus
		for (int di = 0; di < ilist.size(); di++) {
			sampleTopicsForOneDoc (di, (FeatureSequence)ilist.get(di).getData(), topicWeights, r);
			sampleEmosForOneDoc (di, emoinfo.get(di), emoWeights, r);
		}
	}

	/* One iteration of Gibbs sampling, across all documents. */
	public void sampleTopicsEmosForDocs (int start, int length, Randoms r)
	{
		assert (start+length <= ilist.size());
		double[] topicWeights = new double[numTopics];
		double[] emoWeights = new double[numEmos];
		// Loop over every word in the corpus
		for (int di = start; di < start+length; di++) {
		
			sampleTopicsForOneDoc (di, (FeatureSequence)ilist.get(di).getData(), topicWeights, r);
			sampleEmosForOneDoc (di, emoinfo.get(di), emoWeights, r);
		}
	}

	
	private void sampleEmosForOneDoc (int dindx, Vector<String> oneDocEmoTokens, double[] emoWeights, 
			  Randoms r)
		{
			int[] currentType2EmoCounts;
			int type2, oldEmo, newEmo;
			double emoWeightsSum;
			int docLen = oneDocEmoTokens.size();
			double ew;
			
			oldEmo = gsdas_docsEmoAssignment[dindx];
			gsdas_docsPerEmos[oldEmo]--;
			gsdas_tokens2PerEmo[oldEmo]-=docLen;
			
			// Iterate over the positions (words) in the document
			for (int si = 0; si < docLen; si++) {
				type2 = emodict.get(oneDocEmoTokens.get(si));
				gsdas_type2EmoCounts[type2][oldEmo]--;			
			}
		
			Arrays.fill (emoWeights, 0.0);
			emoWeightsSum = 0;
			
			
			Hashtable<Integer, Integer> thisdoctype2counts = new Hashtable<Integer, Integer>();
			for (int si = 0; si < docLen; si++) 
			{
				type2 = emodict.get(oneDocEmoTokens.get(si));
				Integer c = thisdoctype2counts.get(type2);
				if (c==null) c = 0;
				c++;
				thisdoctype2counts.put(type2, c);
			}

			
			for (int ei = 0; ei < numEmos; ei++) {
				
				ew = 0;
			
			
				double num = 1;

				for (Enumeration<Integer> typex=thisdoctype2counts.keys(); typex.hasMoreElements(); )
				{
					type2 = typex.nextElement(); //oneDocTokens.getIndexAtPosition(si);
					currentType2EmoCounts = gsdas_type2EmoCounts[type2];
					Integer c = thisdoctype2counts.get(type2);
					double temp = 1;
					for (int cx=1; cx<=c; cx++)
					{
						temp *= (currentType2EmoCounts[ei] + emoBeta + cx-1 );
					}
					num *= temp;
				}	
				double den = 1;
				for (int si = 0; si < docLen; si++) {
					den *= (gsdas_tokens2PerEmo[ei] + emovBeta + si);
				}

					
				ew = (num/den)*(gsdas_docsPerEmos[ei] + emoAlpha); // (/#docsInCorpus-1+tAlpha); is constant across all topics
				emoWeights[ei] += ew;
				
			}
			
			for (int ti=0; ti<numEmos; ti++)
			{
				emoWeightsSum += emoWeights[ti] ;
			}
			

			// Sample an emo assignment from this distribution
			newEmo = r.nextDiscrete (emoWeights, emoWeightsSum);
			gsdas_docsPerEmos[newEmo]++;
			gsdas_docsEmoAssignment[dindx]=newEmo;
			gsdas_tokens2PerEmo[newEmo] += docLen;
			
			for (int si = 0; si < docLen; si++) {
				type2 = emodict.get(oneDocEmoTokens.get(si));
				// Put that new emo into the counts
				gsdas_type2EmoCounts[type2][newEmo]++;
			}
			
		}

	
	
	
	
//Vector<String> oneDocEmoTokens, double []emoWeights,
	
  private void sampleTopicsForOneDoc (int dindx, FeatureSequence oneDocTokens, double[] topicWeights, 
		  Randoms r)
	{
		int[] currentTypeTopicCounts;
		int type, oldTopic, newTopic;
		double topicWeightsSum;
		int docLen = oneDocTokens.getLength();
		double tw;
		
		oldTopic = gsdas_docsTopicAssignment[dindx];
		gsdas_docsPerTopics[oldTopic]--;
		tokensPerTopic[oldTopic]-=docLen;
		
		// Iterate over the positions (words) in the document
		for (int si = 0; si < docLen; si++) {
			type = oneDocTokens.getIndexAtPosition(si);
			typeTopicCounts[type][oldTopic]--;			
		}
	
		Arrays.fill (topicWeights, 0.0);
		topicWeightsSum = 0;
		
		
		Hashtable<Integer, Integer> thisdoctypecounts = new Hashtable<Integer, Integer>();
		for (int si = 0; si < docLen; si++) 
		{
			type = oneDocTokens.getIndexAtPosition(si);
			Integer c = thisdoctypecounts.get(type);
			if (c==null) c = 0;
			c++;
			thisdoctypecounts.put(type, c);
		}

		
		for (int ti = 0; ti < numTopics; ti++) {
			
			tw = 0;
		
		
			double num = 1;

			for (Enumeration<Integer> typex=thisdoctypecounts.keys(); typex.hasMoreElements(); )
			{
				type = typex.nextElement(); //oneDocTokens.getIndexAtPosition(si);
				currentTypeTopicCounts = typeTopicCounts[type];
				Integer c = thisdoctypecounts.get(type);
				double temp = 1;
				for (int cx=1; cx<=c; cx++)
				{
					temp *= (currentTypeTopicCounts[ti] + beta + cx-1 );
				}
				num *= temp;
			}	
			double den = 1;
			for (int si = 0; si < docLen; si++) {
				den *= (tokensPerTopic[ti] + vBeta + si);
			}



			//tw = (num/den);

				
			tw = (num/den)*(gsdas_docsPerTopics[ti] + alpha); // (/#docsInCorpus-1+tAlpha); is constant across all topics
			topicWeights[ti] += tw;
			
		}
		
		for (int ti=0; ti<numTopics; ti++)
		{
			topicWeightsSum += topicWeights[ti] ;
		}
		
		
		// Sample a topic assignment from this distribution
		newTopic = r.nextDiscrete (topicWeights, topicWeightsSum);
		gsdas_docsPerTopics[newTopic]++;
		gsdas_docsTopicAssignment[dindx]=newTopic;
		tokensPerTopic[newTopic] += docLen;
		
		for (int si = 0; si < docLen; si++) {
			type = oneDocTokens.getIndexAtPosition(si);
			// Put that new topic into the counts
			typeTopicCounts[type][newTopic]++;
		}
		
	}
	
  
  public void printTopEmoWords (int numWords, boolean useNewLines)
	{
		

		WordProb[] wp = new WordProb[numTypes2];
		for (int ei = 0; ei < numEmos; ei++) {
			for (int wi = 0; wi < numTypes2; wi++)
				wp[wi] = new WordProb (wi, ((double)gsdas_type2EmoCounts[wi][ei]) / gsdas_tokens2PerEmo[ei]);
			Arrays.sort (wp);
			if (useNewLines) {
				System.out.println ("\nEmotion "+ei+" #docs "+gsdas_docsPerEmos[ei]);
				for (int i = 0; i < numWords; i++)
					System.out.println (this.r_emodict.get(wp[i].wi).toString() + " " + wp[i].p);
			} else {
				System.out.print ("Emotion "+ei+" #docs "+gsdas_docsPerEmos[ei]+" : ");
				for (int i = 0; i < numWords; i++)
					System.out.print (this.r_emodict.get(wp[i].wi).toString() + " ");
				System.out.println();
			}
		}
	}
  
  
  public void printTopEmoWords (int numWords, int emoid)
	{
	

	  WordProb[] wp = new WordProb[numTypes2];

	  for (int wi = 0; wi < numTypes2; wi++)
		  wp[wi] = new WordProb (wi, ((double)gsdas_type2EmoCounts[wi][emoid]) / gsdas_tokens2PerEmo[emoid]);
	  Arrays.sort (wp);

	  System.out.print ("Emotion "+emoid+" #docs "+gsdas_docsPerEmos[emoid]+" : ");
	  for (int i = 0; i < numWords; i++)
		  System.out.print (this.r_emodict.get(wp[i].wi).toString() + " ");
	  System.out.println();


	}
  
  	public Vector<String> getTopEmoWords (int numWords, int emoid)
  	{
	WordProb[] wp = new WordProb[numTypes2];

  	  for (int wi = 0; wi < numTypes2; wi++)
  		  wp[wi] = new WordProb (wi, ((double)gsdas_type2EmoCounts[wi][emoid]) / gsdas_tokens2PerEmo[emoid]);
  	  Arrays.sort (wp);

  	  Vector<String> temp = new Vector<String>();
  	  for (int i = 0; i < numWords; i++)
  		  temp.add(this.r_emodict.get(wp[i].wi));
  	  
  	  return temp;
  		
  	}
  
	public void printTopWords (int numWords, boolean useNewLines)
	{
	

		WordProb[] wp = new WordProb[numTypes];
		for (int ti = 0; ti < numTopics; ti++) {
			for (int wi = 0; wi < numTypes; wi++)
				wp[wi] = new WordProb (wi, ((double)typeTopicCounts[wi][ti]) / tokensPerTopic[ti]);
			Arrays.sort (wp);
			if (useNewLines) {
				System.out.println ("\nTopic "+ti+" #docs "+gsdas_docsPerTopics[ti]);
				for (int i = 0; i < numWords; i++)
					System.out.println (ilist.getDataAlphabet().lookupObject(wp[i].wi).toString() + " " + wp[i].p);
			} else {
				System.out.print ("Topic "+ti+" #docs "+gsdas_docsPerTopics[ti]+" : ");
				for (int i = 0; i < numWords; i++)
					System.out.print (ilist.getDataAlphabet().lookupObject(wp[i].wi).toString() + " ");
				System.out.println();
			}
		}
	}
  
	public void printTopWords (int numWords, int topicid)
	{
		

		WordProb[] wp = new WordProb[numTypes];

		for (int wi = 0; wi < numTypes; wi++)
			wp[wi] = new WordProb (wi, ((double)typeTopicCounts[wi][topicid]) / tokensPerTopic[topicid]);
		Arrays.sort (wp);

		System.out.print ("Topic "+topicid+" #docs "+gsdas_docsPerTopics[topicid]+" : ");
		for (int i = 0; i < numWords; i++)
			System.out.print (ilist.getDataAlphabet().lookupObject(wp[i].wi).toString() + " ");
		System.out.println();
			
		
	}
	  
  /////////////////////
  
	static Hashtable<Integer, String> loadContentInfo(String contentfile)
  	{
  		Hashtable<Integer,  String> continfo = new Hashtable<Integer,  String>();
  		try {
  			BufferedReader br = new BufferedReader(new FileReader(contentfile));
  			int lx=0;
  			while (true)
  			{
  				String line = br.readLine();
  				if (line==null)
  					break;
  				
  				String []parts = line.trim().split("[ ]+");
  				
  				continfo.put(new Integer(lx), line.replaceFirst(parts[0], "").replaceFirst(parts[1], "").trim());
  				lx++;
  			}
  			br.close();
  			
  		}catch (Exception e)
  		{
  			e.printStackTrace();
  		}
  		
  		return continfo;
  	}
  
	
	
  	static Hashtable<Integer, Vector<String>> loadEmoInfo(String emoinfofile)
  	{
  		Hashtable<Integer,  Vector<String>> eminfo = new Hashtable<Integer,  Vector<String>>();
  		try {
  			BufferedReader br = new BufferedReader(new FileReader(emoinfofile));
  			int lx=0;
  			while (true)
  			{
  				String line = br.readLine();
  				if (line==null)
  					break;
  				
  				String []parts = line.split("[ ]+");
  				Vector<String> emtoks = new Vector<String>();
  				for (int px=2; px<parts.length; px++)
  				{
  					emtoks.add(parts[px].trim());
  				}
  				eminfo.put(new Integer(lx), emtoks);
  				lx++;
  			}
  			br.close();
  			
  		}catch (Exception e)
  		{
  			e.printStackTrace();
  		}
  		
  		return eminfo;
  	}
  
  	static Hashtable[] loadEmoDicts(String emodictfile)
  	{
  		Hashtable<String, Integer> emdict = new Hashtable<String, Integer>();
  		Hashtable<Integer, String> r_emdict = new Hashtable<Integer, String>();
  		try {
  			BufferedReader br = new BufferedReader(new FileReader(emodictfile));
  			int lx=0;
  			while (true)
  			{
  				String line = br.readLine();
  				if (line==null)
  					break;
  				
  				String []parts = line.split("[ ]+");
  				String emtok = parts[0].trim();
  				if (!emdict.containsKey(emtok))
  				{
  					emdict.put(emtok, lx);
  					r_emdict.put(lx, "["+line.replaceFirst(emtok, "").trim()+"]");
  				}else
  				{
  					System.out.println("Duplicated entry "+emtok+" "+line);
  				}
  				
  				lx++;
  			}
  			br.close();
  			
  		}catch (Exception e)
  		{
  			e.printStackTrace();
  		}
  		
  		return new Hashtable[] {emdict, r_emdict};
  	}
  	
  	static void check(Hashtable<Integer,  Vector<String>> emoinfo, Hashtable<String, Integer> emdict)
  	{
  		
  		for (Enumeration<Integer> dx = emoinfo.keys(); dx.hasMoreElements(); )
  		{
  			Vector<String> emtoks = emoinfo.get(dx.nextElement());
  			
  			for (int tok=0; tok<emtoks.size();  tok++)
  			{
  				Integer tokid = emdict.get(emtoks.get(tok));
  				if (tokid==null)
  				{
  					System.out.println("Cannot find "+emtoks.get(tok));
  				}
  			}
  		}
  	}
  	private void writeObject (ObjectOutputStream out) throws IOException {

		out.writeObject (ilist);
		out.writeObject (emoinfo);
		out.writeObject (emodict);
		out.writeObject (r_emodict);
		out.writeObject(continfo);
		out.writeInt (numTopics);
		out.writeInt (numEmos);
		out.writeDouble (alpha);
		out.writeDouble (beta);
		out.writeDouble (tAlpha);
		out.writeDouble (vBeta);
		out.writeDouble (emoAlpha);
		out.writeDouble (emoBeta);
		out.writeDouble (emotAlpha);
		out.writeDouble (emovBeta);
		
		
		for (int ti = 0; ti < gsdas_docsPerTopics.length; ti ++)
			out.writeInt (gsdas_docsPerTopics[ti]);
		
		for (int di = 0; di < gsdas_docsTopicAssignment.length; di ++)
			out.writeInt (gsdas_docsTopicAssignment[di]);
		
		for (int fi = 0; fi < numTypes; fi++)
			for (int ti = 0; ti < numTopics; ti++)
				out.writeInt (typeTopicCounts[fi][ti]);
		
		for (int ti = 0; ti < numTopics; ti++)
			out.writeInt (tokensPerTopic[ti]);
		
		//
		for (int ei = 0; ei < gsdas_docsPerEmos.length; ei ++)
			out.writeInt (gsdas_docsPerEmos[ei]);
		
		for (int di = 0; di < gsdas_docsEmoAssignment.length; di ++)
			out.writeInt (gsdas_docsEmoAssignment[di]);
		
		for (int fi = 0; fi < numTypes2; fi++)
			for (int ei = 0; ei < numEmos; ei++)
				out.writeInt (gsdas_type2EmoCounts[fi][ei]);
		
		for (int ei = 0; ei < numEmos; ei++)
			out.writeInt (gsdas_tokens2PerEmo[ei]);
		
	}

	static public STWithELDA readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int featuresLength;
		
		InstanceList ilist = (InstanceList) in.readObject ();
		Hashtable<Integer, Vector<String>> emoinfo = (Hashtable<Integer, Vector<String>>) in.readObject ();
		Hashtable<String, Integer> emodict = (Hashtable<String, Integer>) in.readObject ();
		Hashtable<Integer, String> r_emodict = (Hashtable<Integer, String>) in.readObject ();
		Hashtable<Integer, String> continfo = (Hashtable<Integer, String>) in.readObject ();
		int numTopics = in.readInt();
		
		STWithELDA lda = new STWithELDA(numTopics);
		lda.ilist = ilist;
		lda.emoinfo = emoinfo;
		lda.emodict = emodict;
		lda.r_emodict = r_emodict;
		lda.continfo = continfo;
		lda.numEmos = in.readInt();		
		lda.alpha = in.readDouble();
		lda.beta = in.readDouble();
		lda.tAlpha = in.readDouble();
		lda.vBeta = in.readDouble();
		lda.emoAlpha = in.readDouble();
		lda.emoBeta = in.readDouble();
		lda.emotAlpha = in.readDouble();
		lda.emovBeta = in.readDouble();
		
		int numDocs = ilist.size();
		lda.gsdas_docsPerTopics = new int[numTopics];
		for (int ti = 0; ti < numTopics; ti++) {
			lda.gsdas_docsPerTopics[ti] = in.readInt();
		}
		lda.gsdas_docsTopicAssignment = new int[numDocs];
		for (int di = 0; di < numDocs; di++)
			lda.gsdas_docsTopicAssignment[di] = in.readInt();
		
		lda.numTypes = ilist.getDataAlphabet().size();
		lda.typeTopicCounts = new int[lda.numTypes][numTopics];
		for (int fi = 0; fi < lda.numTypes; fi++)
			for (int ti = 0; ti < numTopics; ti++)
				lda.typeTopicCounts[fi][ti] = in.readInt();
		lda.tokensPerTopic = new int[numTopics];
		for (int ti = 0; ti < numTopics; ti++)
			lda.tokensPerTopic[ti] = in.readInt();
		
		
		lda.gsdas_docsPerEmos = new int[lda.numEmos];
		for (int ei = 0; ei < lda.numEmos; ei++) {
			lda.gsdas_docsPerEmos[ei] = in.readInt();
		}
		lda.gsdas_docsEmoAssignment = new int[numDocs];
		for (int di = 0; di < numDocs; di++)
			lda.gsdas_docsEmoAssignment[di] = in.readInt();
		
		lda.numTypes2 = emodict.size();
		lda.gsdas_type2EmoCounts = new int[lda.numTypes2][lda.numEmos];
		for (int fi = 0; fi < lda.numTypes2; fi++)
			for (int ei = 0; ei < lda.numEmos; ei++)
				lda.gsdas_type2EmoCounts[fi][ei] = in.readInt();
		lda.gsdas_tokens2PerEmo = new int[lda.numEmos];
		for (int ti = 0; ti < lda.numEmos; ti++)
			lda.gsdas_tokens2PerEmo[ti] = in.readInt();
		
		
		return lda;
		
	}
	public void getContentForTopic (int topicid, int numInstances, String htprefix, String outfile)
		  	throws Exception
			{
		  		Vector<Integer> ids = new Vector<Integer>();
		  		
		  		for (int ax=0; ax<gsdas_docsTopicAssignment.length; ax++)
		  		{
		  			if (gsdas_docsTopicAssignment[ax]==topicid)
		  			{
		  				ids.add(ax);
		  			}
		  		}
			
		  		Collections.shuffle(ids);
		  		
		  		PrintWriter pw = new PrintWriter(new FileWriter(outfile), true);
		  		for (int ix=0; ix<numInstances && ix<ids.size(); ix++)
		  		{
		  			
		  			String  []temp = this.continfo.get(ids.get(ix)).toString().split("[ ]+");
		  			String top="";
		  			for (int wx=0; wx<temp.length; wx++)
		  			{
		  				if (!temp[wx].toLowerCase().startsWith(htprefix))
		  					top +=" "+temp[wx];
		  			}
		  			pw.println(top);
		  		}
		  		pw.close();
			}
		
	public void getContentForEmo(int emoid, String outfile)
		  	throws Exception
			{
		  		
		  		PrintWriter pw = new PrintWriter(new FileWriter(outfile), true);
		  		for (int ax=0; ax<gsdas_docsEmoAssignment.length; ax++)
		  		{
		  			if (gsdas_docsEmoAssignment[ax]==emoid)
		  			{
		  				pw.println(ilist.get(ax).getName()+" "+this.continfo.get(ax));
		  			}
		  		}
			
		  		pw.close();
			}
	
	
	public double[] getEmoCoherence(int numTopEmoji)
	{
		
		double []ecoh = new double[numEmos];
		
		Hashtable<Integer, Integer> ecounts= new Hashtable<Integer, Integer>();
		Hashtable<String, Integer> pcounts= new Hashtable<String, Integer>();
		
		
		for (Enumeration<Integer> ix = emoinfo.keys(); ix.hasMoreElements();)
		{
			Vector<String> slvals = emoinfo.get(ix.nextElement());
			Hashtable<Integer, Integer> unique= new Hashtable<Integer, Integer>();
			for (int vx1=0; vx1<slvals.size(); vx1++)
			{
				int vx1indx = emodict.get(slvals.get(vx1));
				unique.put(vx1indx,0);
			}
			
			for (Enumeration<Integer> ex = unique.keys(); ex.hasMoreElements();)
			{
				int type2id = ex.nextElement();
				
				Integer c = ecounts.get(type2id);
				if (c==null) c=0;
				c++;
				ecounts.put(type2id, c);
				
				for (Enumeration<Integer> ex2 = unique.keys(); ex2.hasMoreElements();)
				{
					int type2id2 = ex2.nextElement();
					if (type2id2!=type2id)
					{
						String key = type2id+":"+type2id2;
						Integer c2 = pcounts.get(key);
						if (c2==null) 
						{
							key = type2id2+":"+type2id;
							c2 = pcounts.get(key);
							if (c2==null) c2=0;
						}
						c2++;
						pcounts.put(key, c2);
					}
				}
				
			}			
		}
		System.out.println("ecounts.sz "+ecounts.size());
		System.out.println("pcounts.sz "+pcounts.size());
		
		
		

		WordProb[] wp = new WordProb[numTypes2];
		for (int ei = 0; ei < numEmos; ei++) {
			for (int wi = 0; wi < numTypes2; wi++)
				wp[wi] = new WordProb (wi, ((double)gsdas_type2EmoCounts[wi][ei]) / gsdas_tokens2PerEmo[ei]);
			
			Arrays.sort (wp); //we need top words per topic 
			
			double coh=0.0;
			for (int topi=0; topi<numTopEmoji; topi++)
			{
				int type2id = wp[topi].wi;
				
				Integer den = ecounts.get(type2id);
				
				for (int top2i=topi+1; top2i<numTopEmoji; top2i++)
				{
					int type2id2 = wp[top2i].wi;
					String key = type2id+":"+type2id2;
					Integer pc = pcounts.get(key);
					if (pc==null)
					{
						key = type2id2+":"+type2id;
						pc = pcounts.get(key);
					}
					
					if (pc==null)
						pc = 0;
						
					if (den == null)
					{
						System.out.println(" den null for key "+type2id); //cannot happen!
					}
					
					coh += Math.log((pc + emoBeta)/(den));
				}
			}
			
			ecoh[ei] = coh;
		}
		
		return ecoh;
	}
  	
	double getPurity2(int cthresh, String htindtag)
	{ 
	
		Hashtable<String, Integer> htcounts = new Hashtable<String, Integer>();
		
		
		for (int ix=0; ix<this.ilist.size(); ix++)
		{
		
			FeatureSequence onedoctokens = (FeatureSequence)this.ilist.get(ix).getData();
			int docLen = onedoctokens.getLength();
			for (int si = 0; si < docLen; si++) 
			{
				int type = onedoctokens.getIndexAtPosition(si);
				String typestr = (String)this.getAlphabet().lookupObject(type);
				if (typestr.startsWith(htindtag))
				{
					Integer c = htcounts.get(typestr);
					if (c==null) c= 0;
					c++;
					htcounts.put(typestr, c);
				}
			}
		}
		
		int qual=0;
		int numpoints=0;
		Hashtable<String, Integer> toconsider = new Hashtable<String, Integer>();
		
		for (Enumeration<String> htx = htcounts.keys(); htx.hasMoreElements(); )
		{
			String htag = htx.nextElement();
			int count = htcounts.get(htag);
			if (count>cthresh)
			{
				qual++;
				numpoints += count;
				toconsider.put(htag, toconsider.size());
			}
		}
		Hashtable<Integer, int[]> tassoc = new Hashtable<Integer, int[]>();
		int []totcounts = new int[numTopics];
		for (int ix=0; ix<this.ilist.size(); ix++)
		{
			int topic = this.gsdas_docsTopicAssignment[ix];
			FeatureSequence onedoctokens = (FeatureSequence)this.ilist.get(ix).getData();
			int docLen = onedoctokens.getLength();
			for (int si = 0; si < docLen; si++) 
			{
				int type = onedoctokens.getIndexAtPosition(si);
				String typestr = (String)this.getAlphabet().lookupObject(type);
				if (typestr.startsWith(htindtag))
				{
					Integer indx = toconsider.get(typestr); 
					if (indx!=null)
					{
						int []temp = tassoc.get(topic);
						if (temp==null) temp = new int[qual];
						temp[indx]++;
						tassoc.put(topic, temp);
						totcounts[topic]++;
					}
				}
			}
		}
		
		double purity = 0;
		for (Enumeration<Integer> tx = tassoc.keys(); tx.hasMoreElements(); )
		{
			Integer topic = tx.nextElement();
			int []thisthtcounts = tassoc.get(topic);
		
			double max = 0;
			for (int htx=0; htx<thisthtcounts.length; htx++)
			{
				if (thisthtcounts[htx]>max)
				{
					max = thisthtcounts[htx];
				}
			}
			purity += max;
			
		}
		
		System.out.println("#qualifying htags "+qual);
		System.out.println("#total points "+numpoints);
		return purity/numpoints;
		
	}
	
	
	double getPurity(int cthresh, String htindtag)
	{ 
	
		Hashtable<String, Integer> htcounts = new Hashtable<String, Integer>();
		Hashtable<String, int[]> tacounts = new Hashtable<String, int[]>();
		
		for (int ix=0; ix<this.ilist.size(); ix++)
		{
			int topic = this.gsdas_docsTopicAssignment[ix];
			FeatureSequence onedoctokens = (FeatureSequence)this.ilist.get(ix).getData();
			int docLen = onedoctokens.getLength();
			for (int si = 0; si < docLen; si++) 
			{
				int type = onedoctokens.getIndexAtPosition(si);
				String typestr = (String)this.getAlphabet().lookupObject(type);
				if (typestr.startsWith(htindtag))
				{
					Integer c = htcounts.get(typestr);
					if (c==null) c= 0;
					c++;
					htcounts.put(typestr, c);
					
					int []tas = tacounts.get(typestr);
					if (tas==null)
					{
						tas = new int[this.numTopics];
					}
					tas[topic]++;
					tacounts.put(typestr, tas);
				}
			}
		}
		int qual=0;
		int numpoints=0;
		double purity = 0;
		for (Enumeration<String> htx = htcounts.keys(); htx.hasMoreElements(); )
		{
			String htag = htx.nextElement();
			int count = htcounts.get(htag);
			if (count>cthresh)
			{
				qual++;
				numpoints += count;

				int []tcounts = tacounts.get(htag);
				double max = 0;
				for (int tx=0; tx<tcounts.length; tx++)
				{
					if (tcounts[tx]> max)
					{
						max = tcounts[tx];
					}
				}

				purity += max;
			}
		}
		
		System.out.println("#qualifying htags "+qual);
		System.out.println("#total points "+numpoints);
		return purity/numpoints;
		
	}
	
	
	public static void main (String[] args)
			throws Exception
	{
		//main2(null);
//	
		String prefix="/home/sdas/cord/covidtweets/panacea/newemnlprun"; ///home/sdas/cord/covidtweets/panacea/may12emorun";
		String contentmalletf = prefix + "/cont.mallet";
		String contentinfof = prefix + "/cont.mallet.in";
		String emoinfof = prefix + "/emos.mallet.in";
		String emodictf = prefix + "/emodict4lda.dat";
		String outfile = prefix + "/work/lda.model.dat";

		InstanceList ilist = InstanceList.load (new File(contentmalletf));
		Hashtable<Integer,  Vector<String>> emoinfo = loadEmoInfo(emoinfof);
		Hashtable<Integer,  String> continfo = loadContentInfo(contentinfof);

		Hashtable[] emdict_remdict = loadEmoDicts(emodictf);

		Hashtable<String, Integer> emdict = emdict_remdict[0];
		Hashtable<Integer, String> r_emdict = emdict_remdict[1];

		//	check(emoinfo, emdict);

		System.out.println("#cont instances "+ilist.size());
		System.out.println("#emo instances "+emoinfo.size());
		System.out.println("#emo tokens "+emdict.size());

		int numIterations = 1000;
		int numTopWords = 10;
		int numTopEmoji=5;
		int htcthresh = 50;
		String htindtag = "gsdasht";
		int numTopics = 60;
		int numEmos = 10;

		System.out.println ("Data loaded.");
		System.out.println ("Nt/NE "+numTopics+" "+numEmos);
		STWithELDA lda = new STWithELDA (numTopics, numEmos);
		long starttime = System.currentTimeMillis();
		lda.estimate2 (ilist, 
				emoinfo, emdict, r_emdict, continfo,
				numIterations, 100, 0, null, new Randoms());  // should be 1100

		System.out.println("Time taken in ms "+(System.currentTimeMillis() - starttime));
		
		
//		lda.printTopEmoWords(numTopEmoji, false);
//		lda.printTopWords(numTopWords, false);
//		try {
//			String outf = outfile+"."+numEmos+"."+numTopics;
//			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outf));
//			lda.writeObject(oos);
//			oos.close();
//			System.out.println("Model info written to "+outf);
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.out.println ("Purity 1 for "+numTopics+" "+numEmos+" "+lda.getPurity(htcthresh, htindtag));
//		System.out.println ("Purity 2 for "+numTopics+" "+numEmos+" "+lda.getPurity2(htcthresh, htindtag));
	}

	
	
	// Recommended to use mallet/bin/vectors2topics instead.
	public static void main2 (String[] args)
	throws Exception
	{
		String prefix="/home/sdas/cord/covidtweets/panacea/newemnlprun"; ///home/sdas/cord/covidtweets/panacea/may12emorun";
		String contentmalletf = prefix + "/cont.mallet";
		String contentinfof = prefix + "/cont.mallet.in";
		String emoinfof = prefix + "/emos.mallet.in";
		String emodictf = prefix + "/emodict4lda.dat";
		String outfile = prefix + "/work/ecohrun.out";
		
		InstanceList ilist = InstanceList.load (new File(contentmalletf));
		Hashtable<Integer,  Vector<String>> emoinfo = loadEmoInfo(emoinfof);
		Hashtable<Integer,  String> continfo = loadContentInfo(contentinfof);
		
		Hashtable[] emdict_remdict = loadEmoDicts(emodictf);
		
		Hashtable<String, Integer> emdict = emdict_remdict[0];
  		Hashtable<Integer, String> r_emdict = emdict_remdict[1];
		
	//	check(emoinfo, emdict);
		
		System.out.println("#cont instances "+ilist.size());
		System.out.println("#emo instances "+emoinfo.size());
		System.out.println("#emo tokens "+emdict.size());
		
		int numIterations = 1000;
		int numTopWords = 10;
		int numTopEmoji=5;
		//for 50 this was -126.19486570009555
		int []ntopics_a = { 60};
		int []nemos_a = { 6, 8, 10, 12, 14 };
		PrintWriter pw = new PrintWriter(new FileWriter(outfile), true);
		int rseed = 0;
		
		for (int ntx=0; ntx<ntopics_a.length; ntx++)
		{
			pw.println("\n\nRseed "+rseed +" #topics "+ntopics_a[ntx]);
			for (int nex=0; nex<nemos_a.length; nex++)
			{
				int numTopics = ntopics_a[ntx];
				int numEmotions = nemos_a[nex];
				pw.println("#emos "+numEmotions);	
				System.out.println ("Data loaded.");
				STWithELDA lda = new STWithELDA (numTopics, numEmotions);
				lda.estimate2 (ilist, 
						emoinfo, emdict, r_emdict, continfo,
						numIterations, 100, 0, null, new Randoms(rseed));  // should be 1100

				//			lda.printTopWords (numTopWords, false);
				//			lda.printTopEmoWords (7, false);
				//
				//			try {
				//				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(analmodeloutf));
				//				lda.writeObject(oos);
				//				oos.close();
				//				System.out.println("Model info written to "+analmodeloutf);
				//			} catch (FileNotFoundException e) {
				//				// TODO Auto-generated catch block
				//				e.printStackTrace();
				//			} catch (IOException e) {
				//				// TODO Auto-generated catch block
				//				e.printStackTrace();
				//			}


				
				
				MyTopicModelDiagnostics diagnostics = new MyTopicModelDiagnostics(lda, numTopWords);
				double meancoh=0.0;
				TopicScores topicscores = diagnostics.getCoherence(); 
				for (int tx=0; tx<topicscores.scores.length; tx++)
				{
					pw.println("Topic-"+tx+" "+topicscores.scores[tx]);
					
					meancoh += topicscores.scores[tx];			
				}
				
				meancoh /= numTopics;
				pw.println("Num Topics "+numTopics+" Average Coherence "+meancoh);
				
				double variance=0;
				for (int tx=0; tx<topicscores.scores.length; tx++)
				{
					variance += Math.pow((topicscores.scores[tx]-meancoh), 2) ;			
				}
				variance /= numTopics;
				pw.println("Topic Coherence Variance "+variance);

				pw.println();
							
				double meanecoh = 0;
				double []ecoh = lda.getEmoCoherence(numTopEmoji);
				for (int ex=0; ex<ecoh.length; ex++)
				{
					pw.println("Emo-"+ex+" "+ecoh[ex]);
					meanecoh += ecoh[ex];			
				}
				meanecoh /= numEmotions;
				pw.println("Num Emotions "+numEmotions+" Average Emo Coherence "+meanecoh);
				
				variance=0;
				for (int tx=0; tx<ecoh.length; tx++)
				{
					variance += Math.pow((ecoh[tx]-meanecoh), 2) ;			
				}
				variance /= numEmotions;
				pw.println("Emotion Coherence Variance "+variance);
				pw.println();
				int []ndocspertopiccluster = lda.gsdas_docsPerTopics;
				int []ndocsperemocluster = lda.gsdas_docsPerEmos;
				
				pw.println("Doc Allocation per Topic ");
				for (int nx=0; nx<ndocspertopiccluster.length; nx++)
				{
					double pct = (double)ndocspertopiccluster[nx]/lda.ilist.size();
					pw.println("#docs for topic "+ndocspertopiccluster[nx] +" "+pct);
				}
				pw.println();
				pw.println("Doc Allocation per Emotion ");
				for (int nx=0; nx<ndocsperemocluster.length; nx++)
				{
					double pct = (double)ndocsperemocluster[nx]/lda.ilist.size();
					pw.println("#docs for emotion "+ndocsperemocluster[nx]+" "+pct);
				}
			
			}
			
			
		}
		
		pw.close();
	}

}
