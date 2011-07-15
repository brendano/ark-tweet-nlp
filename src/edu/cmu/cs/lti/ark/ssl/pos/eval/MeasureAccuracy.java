package edu.cmu.cs.lti.ark.ssl.pos.eval;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.lti.ark.ssl.pos.TabSeparatedFileReader;
import fig.basic.Pair;

public class MeasureAccuracy {
	
	public static final String corporaDir = 
		"/mal2/dipanjan/experiments/SSL/data/conllcorpora";
	public static final String outputDir = "/mal2/dipanjan/experiments/SSL/data/runOutputs";
	public static final String[] languages = {"DUTCH", "GERMAN", "ITALIAN", "PORTUGUESE", "SPANISH", "SWEDISH"};	
	public static final String[] pd = {"0.3", "0.2", "0.1", "0.1", "0.1", "0.1"};
	public static final String[] lg = {"0.2", "0.3", "0.2", "0.1", "0.2", "0.2"};
	
	public static void main(String[] args) {
		String lang = "DANISH";
		String clusters = "-clusters-25";
		String dir = "/home/dipanjan/Downloads";
		String testGoldCorpus = dir + "/" + lang + "-test.tb.upos";
		String type = "pd";
		String thresh = "0.2";
		String testCorpus1 = dir + "/" + lang + "-test-results-" + type +"-" + thresh + clusters + "-preset";
		System.out.println("Language:" + lang);
		System.out.println("Accuracy w/o LP: " + getAccuracy(testGoldCorpus, testCorpus1));
		type = "lp";
		thresh = "0.2";
		String testCorpus2 = dir + "/" + lang + "-test-results-" + type +"-" + thresh + clusters + "-preset";
		System.out.println("Accuracy w/ LP: " + getAccuracy(testGoldCorpus, testCorpus2));
		writeIndicatorFiles(lang, testGoldCorpus, testCorpus1, testCorpus2);
		System.out.println();
	}	
	
	public static void oldLanguages() {
		String gold = "/mal2/dipanjan/experiments/SSL/data/cameraready/data/"+
					  "DANISH-test.tb.upos";
		String output =  "/mal2/dipanjan/experiments/SSL/data/cameraready/output/"+
		  			  "DANISH-fhmm-reg.1.0.tag.dict.test.output.upos";
		double acc = getAccuracy(gold, output);
		System.out.println("Accuracy:" + acc);
	}	
	
	public static void aclpaper() {
		for (int i = 0; i < languages.length; i ++) {
			String lang = languages[i];
			String testGoldCorpus = corporaDir + "/" + lang + "-test.tb.upos";
			String type = "pd";
			String thresh = pd[i];
			String testCorpus1 = outputDir + "/" + lang + "-test-results-tag-dict-" + type +"-" + thresh + "-preset";
			System.out.println("Language:" + lang);
			System.out.println("Accuracy w/o LP: " + getAccuracy(testGoldCorpus, testCorpus1));
			type = "lg-fi";
			thresh = lg[i];
			String testCorpus2 = outputDir + "/" + lang + "-test-results-tag-dict-" + type +"-" + thresh + "-preset";
			System.out.println("Accuracy w/ LP: " + getAccuracy(testGoldCorpus, testCorpus2));
			writeIndicatorFiles(lang, testGoldCorpus, testCorpus1, testCorpus2);
			System.out.println();
		}
	}
	
	public static void writeIndicatorFiles(String language,
										   String goldCorpus,
										   String woLPCorpus,
										   String wLPCorpus) {
		Collection<Pair<List<String>, List<String>>>  gSequences =
			TabSeparatedFileReader.readPOSSeqences(goldCorpus, 
					Integer.MAX_VALUE, 
					200);
		
		Collection<Pair<List<String>, List<String>>>  woSequences =
			TabSeparatedFileReader.readPOSSeqences(woLPCorpus, 
					Integer.MAX_VALUE, 
					200);
		
		Collection<Pair<List<String>, List<String>>>  wSequences =
			TabSeparatedFileReader.readPOSSeqences(wLPCorpus, 
					Integer.MAX_VALUE, 
					200);
		signTestSentenceLevel(gSequences, woSequences, wSequences, "../sigtests/" + language + "-wolp-sl", "../sigtests/" + language + "-wlp-sl");
	}
	
	public static double getAccuracy(String testGoldCorpus, String testCorpus) {
		Collection<Pair<List<String>, List<String>>>  gSequences =
			TabSeparatedFileReader.readPOSSeqences(testGoldCorpus, 
					Integer.MAX_VALUE, 
					200);
		
		Collection<Pair<List<String>, List<String>>>  aSequences =
			TabSeparatedFileReader.readPOSSeqences(testCorpus, 
					Integer.MAX_VALUE, 
					200);
		Iterator<Pair<List<String>, List<String>>> aItr = 
			aSequences.iterator();
		double total = 0.0;
		double correct = 0.0;
		for (Pair<List<String>, List<String>> gSeq: gSequences) {
			List<String> gPOS = gSeq.getSecond();
			List<String> aPOS = aItr.next().getSecond();
			for (int i = 0; i < gPOS.size(); i++) {
				if (gPOS.get(i).equals(aPOS.get(i))) {
					correct++;
				}
				total++;
			}
		}
		return correct / total;
	}	 
	
	public static double signTestSentenceLevel(Collection<Pair<List<String>, List<String>>>  gSequences, 
			Collection<Pair<List<String>, List<String>>> seq1, 
			Collection<Pair<List<String>, List<String>>> seq2,
			String file1,
			String file2) {
		int size = gSequences.size();
		ArrayList<Pair<List<String>, List<String>>> gList = 
			new ArrayList<Pair<List<String>, List<String>>>(gSequences);
		ArrayList<Pair<List<String>, List<String>>> col1 = 
			new ArrayList<Pair<List<String>, List<String>>>(seq1);
		ArrayList<Pair<List<String>, List<String>>> col2 = 
			new ArrayList<Pair<List<String>, List<String>>>(seq2);		
		
		ArrayList<String> ind1 = new ArrayList<String>();
		ArrayList<String> ind2 = new ArrayList<String>();
		int total = 0;
		for(int i = 0; i < size; i ++)
		{
			List<String> gPOS = gList.get(i).getSecond();
			List<String> pos1 = col1.get(i).getSecond();
			List<String> pos2 = col2.get(i).getSecond();
			double sentCorrect1 = 0.0;
			double sentCorrect2 = 0.0;
			for (int j = 0; j < gPOS.size(); j ++) {
				if(pos1.get(j).equals(gPOS.get(j)))
				{
					sentCorrect1 ++;
				}
				if(pos2.get(j).equals(gPOS.get(j)))
				{
					sentCorrect2 ++;
				}
				total ++;
			}
			double sa1 = sentCorrect1 / gPOS.size();
			double sa2 = sentCorrect2 / gPOS.size();
			ind1.add("" + sa1);
			ind2.add("" + sa2);
		}
		writeFile(ind1, file1);
		writeFile(ind2, file2);
		
//		int n = 0;
//		int Y = 0;
//		for(int i = 0; i < size; i ++)
//		{
//			if(!ind1.get(i).equals(ind2.get(i)))
//			{
//				n++;
//			}
//			double A = new Double(ind1.get(i));
//			double B = new Double(ind2.get(i));
//			if(A - B > 0)
//			{
//				Y++;
//			}
//		}
//		System.out.println("n="+n);
//		System.out.println("Y="+Y);
//		double z = (double)(Y - 0.5*n)/(0.5*Math.sqrt((double)n));
//		System.out.println("z="+z);		
		return 0;
	}
	
	public static double signTest(Collection<Pair<List<String>, List<String>>>  gSequences, 
			Collection<Pair<List<String>, List<String>>> seq1, 
			Collection<Pair<List<String>, List<String>>> seq2,
			String file1,
			String file2) {
		int size = gSequences.size();
		ArrayList<Pair<List<String>, List<String>>> gList = 
			new ArrayList<Pair<List<String>, List<String>>>(gSequences);
		ArrayList<Pair<List<String>, List<String>>> col1 = 
			new ArrayList<Pair<List<String>, List<String>>>(seq1);
		ArrayList<Pair<List<String>, List<String>>> col2 = 
			new ArrayList<Pair<List<String>, List<String>>>(seq2);		
		
		ArrayList<String> ind1 = new ArrayList<String>();
		ArrayList<String> ind2 = new ArrayList<String>();
		int countA = 0;
		int countB = 0;
		int total = 0;
		for(int i = 0; i < size; i ++)
		{
			List<String> gPOS = gList.get(i).getSecond();
			List<String> pos1 = col1.get(i).getSecond();
			List<String> pos2 = col2.get(i).getSecond();
			for (int j = 0; j < gPOS.size(); j ++) {
				if(pos1.get(j).equals(gPOS.get(j)))
				{
					ind1.add("1");
					countA++;
				}
				else {
					ind1.add("0");
				}
				if(pos2.get(j).equals(gPOS.get(j)))
				{
					ind2.add("1");
					countB++;
				}
				else {
					ind2.add("0");
				}
				total ++;
			}
		}
		writeFile(ind1, file1);
		writeFile(ind2, file2);
		
		System.out.println("B accuracy="+((double)countA)/total);
		System.out.println("A accuracy="+((double)countB)/total);
//		int n = 0;
//		int Y = 0;
//		for(int i = 0; i < size; i ++)
//		{
//			if(!ind1.get(i).equals(ind2.get(i)))
//			{
//				n++;
//			}
//			double A = new Double(ind1.get(i));
//			double B = new Double(ind2.get(i));
//			if(A - B > 0)
//			{
//				Y++;
//			}
//		}
//		System.out.println("n="+n);
//		System.out.println("Y="+Y);
//		double z = (double)(Y - 0.5*n)/(0.5*Math.sqrt((double)n));
//		System.out.println("z="+z);		
		return 0;
	}
	
	public static void writeFile(ArrayList<String> lines, String outputFile)
	{
		int size = lines.size();
		try {
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(outputFile));
			for(int i = 0; i < size; i ++)
			{
				bWriter.write(lines.get(i)+"\n");
			}
			bWriter.close();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
}