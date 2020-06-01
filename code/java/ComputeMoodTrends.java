package cc.mallet.topics;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import ke.CompObj;

public class ComputeMoodTrends {


	static String getWeekString(String date)
	{

		String []ymd= date.split("-");

		int month = new Integer(ymd[1]);
		int day = new Integer(ymd[2]);

		String toret="";
		if (month==1)
		{
			toret="jan";
		}else if (month==2)
		{
			toret="feb";
		}else if (month==3)
		{
			toret="mar";
		}else if (month==4)
		{
			toret="apr";
		}else
		{
			toret="may";
		}

		if (day<=7)
		{
			toret+="wk1";
		}else if (day>7 && day<=14)
		{
			toret+="wk2";
		}else if (day>14 && day<=21)
		{
			toret+="wk3";
		}else if (day>21)
		{
			toret+="wk4";
		}

		return toret;

	}

	static void getHTCounts(String htprefix, int emoid, String wkstr, STWithELDA lda, PrintWriter pw)
			throws Exception
	{

		Hashtable<String, Integer> htcounts = new Hashtable<String, Integer>();
		InstanceList continstances = lda.ilist;
		Alphabet alphabet = lda.getAlphabet();
		Hashtable<String, Vector<String>> htToid = new Hashtable<String, Vector<String>>();
		int count=0;
		for (int ix=0; ix<continstances.size(); ix++)
		{
			String period= getWeekString(lda.ilist.get(ix).getTarget()+"");
			
			if (period.equals(wkstr) && emoid==lda.gsdas_docsEmoAssignment[ix])
			{
				count++;
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
						
						String id = continstances.get(ix).getName()+"";
						String tweet = lda.continfo.get(ix);
						
						Vector<String> t2 = htToid.get(typestr);
						if (t2==null)
							t2 = new Vector<String>();
						
						t2.add(id+" "+tweet);
						htToid.put (typestr, t2);
					}
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
		System.out.println("num qualifying tweets "+count);
		pw.println("num qualifying tweets "+count);
		for (int cx=0; cx<5 && cx<temp.size(); cx++)
		{
			String typestr= temp.get(cx).theobject.toString();
			System.out.println(typestr.replaceFirst(htprefix, "#")+" "+temp.get(cx).score);
			pw.println(temp.get(cx).theobject.toString().replaceFirst(htprefix, "#")+" "+temp.get(cx).score);
			pw.println();
			Vector<String> t2 = htToid.get(typestr);
			for (int tx=0; tx<5 && tx<t2.size(); tx++)
			{
				pw.println(t2.get(tx));
			}
			pw.println();
		}
		
		
	}

	static Hashtable<String, int[]> getAggregateMoodVectors(STWithELDA lda)
	{
		Hashtable<String, int[]> moodmap = new Hashtable<String, int[]>();
		for (int ix=0; ix<lda.ilist.size(); ix++)
		{
			String period= getWeekString(lda.ilist.get(ix).getTarget()+"");
			//			if (ix%1000==0)
			//				System.out.println(period);
			int []moodsthisperiod = moodmap.get(period);
			if (moodsthisperiod == null)
				moodsthisperiod = new int[lda.numEmos];


			int emothisdoc = lda.gsdas_docsEmoAssignment[ix];
			moodsthisperiod[emothisdoc]++;
			moodmap.put(period, moodsthisperiod);			
		}

		return moodmap;

	}
	static Hashtable<String, Integer> getTweetCountsPerPeriod(STWithELDA lda)
	{
		Hashtable<String, Integer> counts = new Hashtable<String, Integer>();
		for (int ix=0; ix<lda.ilist.size(); ix++)
		{
			String period= getWeekString(lda.ilist.get(ix).getTarget()+"");
			Integer tmp = counts.get(period);
			if (tmp==null) tmp =0;
			tmp++;
			counts.put(period, tmp);
		}

		return counts;

	}
	static void printMoodTrendToFile(String outfile, STWithELDA lda, String []moodnames)
			throws Exception
	{
		Hashtable<String, Integer> twcountsperperiod = getTweetCountsPerPeriod(lda);
		PrintWriter pw = new PrintWriter(new FileWriter(outfile), true);
		Hashtable<String, int[]> periodmoodmap = getAggregateMoodVectors(lda);
		String top="";
		for (int mx=0; mx<moodnames.length; mx++)
		{
			if (!moodnames[mx].toLowerCase().equals("none"))
				top +="\t"+moodnames[mx];
		}
		
		System.out.println(top.trim());
		pw.println("Period\tNumTweets\t"+top.trim());
		for (Enumeration<String> px = periodmoodmap.keys(); px.hasMoreElements(); )
		{
			String period = px.nextElement();
			int []moodsthisperiod = periodmoodmap.get(period);
			double tot=0.0;
			for (int mx=0; mx<moodsthisperiod.length; mx++)
			{
				if (!moodnames[mx].toLowerCase().equals("none"))
					tot += moodsthisperiod[mx] + lda.emoAlpha;
			}

			top="";
			for (int mx=0; mx<moodsthisperiod.length; mx++)
			{
				if (!moodnames[mx].toLowerCase().equals("none"))
					top +="\t"+(moodsthisperiod[mx]+lda.emoAlpha)/tot;
			}
			System.out.println(period+"\t"+top.trim());
			pw.println(period+"\t"+twcountsperperiod.get(period)+"\t"+top.trim());
		}
		pw.close();

	}
	public static void main(String []args)throws Exception
	{
		String htprefix="gsdasht";
		String modelf = "/home/sdas/cord/covidtweets/panacea/newemnlprun/work/lda.model.dat.10.60";
		String outfile = "/home/sdas/cord/covidtweets/panacea/newemnlprun/work/moodmap_10_60.dat";
		String httagsfile = "/home/sdas/submissions/20emnlpshort/results/moodhtags_10_60_set2.list";
		
		String []moodnames= {
				"Disgust", "Disgust/Anger", "Joy-1", "None", "None",
				"None", "Joy-2", "Sadness", "Joy-3", "None"
		};
		
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelf));
		STWithELDA lda = STWithELDA.readObject(ois);
		ois.close();
		System.out.println("Num topics "+lda.numTopics+" num emos "+lda.numEmos);
//		
//		printMoodTrendToFile(outfile, lda, moodnames);

		
		PrintWriter pw = new PrintWriter(new FileWriter(httagsfile), true);
		//String []periods= {"febwk2", "marwk3", "marwk3", "aprwk2"};
		String []periods= {"maywk1", "aprwk2"};
		int []minds = { 1, 2} ;//, 2, 2 };
		for (int px=0; px<periods.length; px++)
		{
			System.out.println("\n\nPeriod "+periods[px]+" Mood "+moodnames[minds[px]]);
			pw.println("\n\nPeriod "+periods[px]+" Mood "+moodnames[minds[px]]);
			getHTCounts(htprefix, minds[px], periods[px], lda, pw);
			
		}
		pw.close();
	}

}
