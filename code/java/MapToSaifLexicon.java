package cc.mallet.topics;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import ke.CompObj;

public class MapToSaifLexicon {

	static Hashtable<String, String>  loadEmoMapFromSaif(String saifstatsf)
			throws Exception
	{


		Hashtable<String, String> emoprobs = new Hashtable<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(saifstatsf));

		while (true)
		{
			String line = br.readLine();
			if (line == null)
				break;

			String []split = line.split("\\[");
		//	System.out.println("DEBUG s1 "+split[0]);
		//	System.out.println("DEBUG s2 "+split[1]);
			String emoji = split[0].trim();
			String []emosparts = split[1].replaceAll("\\p{Punct}", "").trim().split("[ ]+");
			double totprob = 0.0;
			for (int px=0; px<emosparts.length; px=px+2)
			{
				//	String emo1 = emosparts[px];
				double prob1 = new Double(emosparts[px+1]);
				totprob += prob1;
			}
			String temp="";
			for (int px=0; px<emosparts.length; px=px+2)
			{
				String emo1 = emosparts[px];
				double prob1 = new Double(emosparts[px+1]);
				temp += " "+emo1+":"+(prob1/totprob);
			}
			emoprobs.put(emoji, temp.trim());

		}

		br.close();

		System.out.println("Size of emoprobs "+emoprobs.size());

		return emoprobs;
	}


	public static void main(String []args)throws Exception
	{
		String saifstatsf = "/home/sdas/cord/covidtweets/panacea/may12data/saifemojis.thresh5.list";

		String analmodelf = "/home/sdas/cord/covidtweets/panacea/newemnlprun/work/lda.model.dat.10.60";

		Hashtable<String, String> emoprobs_emoji = loadEmoMapFromSaif(saifstatsf);

		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(analmodelf));
		STWithELDA lda = STWithELDA.readObject(ois);
		ois.close();
		System.out.println("Num topics "+lda.numTopics+" num emos "+lda.numEmos);

		
		
		for (int ex=0; ex<lda.numEmos; ex++)
		{
			
			System.out.println("\nEmotion "+ex);
			Hashtable<String, Double> scores = new Hashtable<String, Double>();
			Vector<String> emojis = lda.getTopEmoWords(5, ex);
			System.out.println(emojis);
			int found=0;
			
			for (int emx=0; emx<emojis.size(); emx++)
			{
				String t = emojis.get(emx).trim();
				String emoji = t.substring(1,t.length()-1);
			//	System.out.println("Looking up "+emoji);
				if (emoprobs_emoji.containsKey(emoji))
				{
					found++;
					String []probstrparts = emoprobs_emoji.get(emoji).trim().split(" ");
					for (int px=0; px<probstrparts.length; px++)
					{
						String emo1 = probstrparts[px].split(":")[0];
						
						double prob1 = new Double(probstrparts[px].split(":")[1]);						
						Double temp = scores.get(emo1);
						if (temp==null)
							temp=0.0;
						temp += prob1;
						scores.put(emo1, temp);
					}					
				}

			}
			
			if (found>=3)
			{
				System.out.println("Found ("+found+") #emo associated "+scores.size());
				Vector<CompObj> temp = new Vector<CompObj>();
				for (Enumeration<String> htx = scores.keys(); htx.hasMoreElements(); )
				{
					String emo = htx.nextElement();			
					double score = scores.get(emo);
					temp.add(new CompObj(emo, score));			
				}

				Collections.sort(temp); Collections.reverse(temp);

				for (int tx=0; tx<2 && tx<temp.size(); tx++)
				{
					if (temp.get(tx).score>=1.5)
						System.out.println(temp.get(tx));				

				}
			}
			
		}
		
			
	}



}
