package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;

import edu.cmu.cs.lti.ark.ssl.pos.TabSeparatedFileReader;
import fig.basic.Pair;
	
public class ComputeTransitionMultinomials {
	
	public static final int TOTAL_FINE_TAGS = 50;
	public static String[] COARSE_TAGS = {".",
										  "ADJ", 
										  "ADP", 
										  "ADV",
										  "CONJ",
										  "DET", 
										  "NOUN",
										  "NUM",
										  "PRON",
										  "PRT",
										  "VERB",
										   "X",
										   "START",
										   "END"};	
	
	public static void main(String[] args) {
		// withoutPunc(args);
		uniform();
	}	
	
	public static void uniform() {
		int len = COARSE_TAGS.length;
		double[][] arr = new double[len][len];
		for (int i = 0; i < len; i++) {
			for (int j = 0; j < len; j++) {
				arr[i][j] = 1.0 / (double)len;
			}
		}
		String outFile = "/home/dipanjan/work/spring2011/UnsupPOSTagging/data/forgi/multinomials/UNIFORM.gold.mults";
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
		for (int i = 0; i < COARSE_TAGS.length; i++) {
			String line = "";			
			for (int j = 0; j < COARSE_TAGS.length; j++) {
				line += ""+arr[i][j] + " ";
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
		
		bWriter = BasicFileIO.openFileToWrite(outFile + ".explicit");
		for (int i = 0; i < COARSE_TAGS.length; i++) {
			for (int j = 0; j < COARSE_TAGS.length; j++) {
				String line = COARSE_TAGS[i] + " " + COARSE_TAGS[j] + " " + arr[i][j];
				BasicFileIO.writeLine(bWriter, line);
			}		
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}
	
	public static void withoutPunc(String[] args) {
		String language = args[0];
		String trainFile = 
			"/home/dipanjan/work/spring2011/UnsupPOSTagging/data/forgi/treebanks/" + language + "-train.tb.upos";
		String outFile = "/home/dipanjan/work/spring2011/UnsupPOSTagging/data/forgi/multinomials/" + language + ".gold.mults";
		Arrays.sort(COARSE_TAGS);				
		double[][] mults = new double[COARSE_TAGS.length][COARSE_TAGS.length];
		for (int i = 0; i < COARSE_TAGS.length; i++) {
			for (int j = 0; j < COARSE_TAGS.length; j++) {
				mults[i][j] = 0.00001;
			}
		}
		Collection<Pair<List<String>, List<String>>> data = 
			TabSeparatedFileReader.readPOSSeqences(trainFile, Integer.MAX_VALUE, Integer.MAX_VALUE);
		Iterator<Pair<List<String>, List<String>>> itr = data.iterator();
		while (itr.hasNext()) {
			Pair<List<String>, List<String>> dp = itr.next();
			List<String> posTags = dp.getSecond();
			ArrayList<String> listOfTags = new ArrayList<String>(posTags);
			int len = listOfTags.size();
			for (int i = -1; i < len; i++) {
				String curr;
				String next;
				if (i == -1) {
					curr = "START";
				} else {
					curr = listOfTags.get(i);
				}
				if (i == len - 1) {
					next = "END";
				} else {
					next = listOfTags.get(i+1);
				}
				// System.out.println(curr + "\t" + next);
				int currI = Arrays.binarySearch(COARSE_TAGS, curr);
				int nextI = Arrays.binarySearch(COARSE_TAGS, next);
				mults[currI][nextI] += 1.0;
			}
		}
		for (int i = 0; i < COARSE_TAGS.length; i++) {
			double sum = 0.0;
			for (int j = 0; j < COARSE_TAGS.length; j++) {
				sum += mults[i][j];
			}
			for (int j = 0; j < COARSE_TAGS.length; j++) {
				mults[i][j] /= sum;
			}
		}
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
		for (int i = 0; i < COARSE_TAGS.length; i++) {
			String line = "";			
			for (int j = 0; j < COARSE_TAGS.length; j++) {
				line += ""+mults[i][j] + " ";
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
		
		bWriter = BasicFileIO.openFileToWrite(outFile + ".explicit");
		for (int i = 0; i < COARSE_TAGS.length; i++) {
			for (int j = 0; j < COARSE_TAGS.length; j++) {
				String line = COARSE_TAGS[i] + " " + COARSE_TAGS[j] + " " + mults[i][j];
				BasicFileIO.writeLine(bWriter, line);
			}		
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}	
	
	public static void withPunc(String[] args) {
		String language = args[0];
		String trainFile = 
			"/mal2/dipanjan/experiments/SSL/UnsupervisedPOS/data/treebanks/" + language + "-train.tb.upos";
		String outFile = "/mal2/dipanjan/experiments/SSL/UnsupervisedPOS/data/multinomials/" + language + ".gold.mults";
		Arrays.sort(COARSE_TAGS);				
		double[][] mults = new double[COARSE_TAGS.length][COARSE_TAGS.length];
		for (int i = 0; i < COARSE_TAGS.length; i++) {
			for (int j = 0; j < COARSE_TAGS.length; j++) {
				mults[i][j] = 0.00001;
			}
		}
		Collection<Pair<List<String>, List<String>>> data = 
			TabSeparatedFileReader.readPOSSeqences(trainFile, Integer.MAX_VALUE, Integer.MAX_VALUE);
		Iterator<Pair<List<String>, List<String>>> itr = data.iterator();
		while (itr.hasNext()) {
			Pair<List<String>, List<String>> dp = itr.next();
			List<String> posTags = dp.getSecond();
			ArrayList<String> listOfTags = new ArrayList<String>(posTags);
			int len = listOfTags.size();
			for (int i = -1; i < len; i++) {
				String curr;
				String next;
				if (i == -1) {
					curr = "START";
				} else {
					curr = listOfTags.get(i);
				}
				if (i == len - 1) {
					next = "END";
				} else {
					next = listOfTags.get(i+1);
				}
				int currI = Arrays.binarySearch(COARSE_TAGS, curr);
				int nextI = Arrays.binarySearch(COARSE_TAGS, next);
				mults[currI][nextI] += 1.0;
			}
		}
		for (int i = 0; i < COARSE_TAGS.length; i++) {
			double sum = 0.0;
			for (int j = 0; j < COARSE_TAGS.length; j++) {
				sum += mults[i][j];
			}
			for (int j = 0; j < COARSE_TAGS.length; j++) {
				mults[i][j] /= sum;
			}
		}
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
		for (int i = 0; i < COARSE_TAGS.length; i++) {
			String line = "";			
			for (int j = 0; j < COARSE_TAGS.length; j++) {
				line += ""+mults[i][j] + " ";
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);		
	}
}