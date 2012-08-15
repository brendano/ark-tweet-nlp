package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import cmu.arktweetnlp.util.BasicFileIO;

public class ComputeInitialTransitionFeatures {
	
	public static Random baseRand = new Random(43569);
	public static Random[] rands;

	static {
		rands = new Random[10];
		for (int i=0; i<10; ++i) {
			rands[i] = new Random(baseRand.nextInt());
		}
	}
	
	public static void main(String[] args) {
		// computationWithMap(args);
		computationWithMapDictionary(args);
		// sanityCheck(args);
	}
	
	public static void sanityCheck(String[] args) {
		String avgMultFile = args[0];
		String expandedFile = args[0] + ".expanded";
		String language = args[1];		
		String mapFile = 
			"/home/dipanjan/work/spring2011/UnsupPOSTagging/data/maps/" 
			+ AverageMultinomials.languageMap.get(language) + "-fine-universal.map";
		System.out.println("Mapfile: " + mapFile);
		Set<String> validTags = AverageMultinomials.getValidTags(mapFile);		
		validTags.add("START");
		validTags.add("END");
		String[] validTagArray = new String[validTags.size()];
		validTags.toArray(validTagArray);
		Arrays.sort(validTagArray);
		System.out.println("Valid tags:");
		for (String tag: validTagArray) {
			System.out.println(tag);
		}		
		System.out.println("End of array");
		Map<String, String> map = getMap(mapFile);
		Map<String, String> fineToCoarseMap = new HashMap<String, String>();
		Map<Integer, Integer> coarseNumMap = new HashMap<Integer, Integer>();
		
		Set<String> keys = map.keySet();
		int keySize = keys.size();
		String[] keyArray = new String[keySize];
		keys.toArray(keyArray);
		Arrays.sort(keyArray);
		int count = 0;
		for (String key: keyArray) {
			String coarseTag = map.get(key);
			int cIndex = Arrays.binarySearch(validTagArray, coarseTag);
			String fineTag = "T"+count;
			fineToCoarseMap.put(fineTag, coarseTag);
			if (coarseNumMap.containsKey(cIndex)) {
				int num = coarseNumMap.get(cIndex);
				coarseNumMap.put(cIndex, num + 1);
			} else {
				coarseNumMap.put(cIndex, 1);
			}
			count++;
		}
		int numFineTags = map.size();
		int cIndex = Arrays.binarySearch(validTagArray, "START");
		coarseNumMap.put(cIndex, 1);
		int numTag = numFineTags + 1;
		String fineTag = "T"+numTag;
		fineToCoarseMap.put(fineTag, "START");
		
		cIndex = Arrays.binarySearch(validTagArray, "END");
		coarseNumMap.put(cIndex, 1);
		numTag = numFineTags;
		fineTag = "T"+numTag;
		fineToCoarseMap.put(fineTag, "END");
		
		double[][] expandedMults = 
			AverageMultinomials.readGoldMultinomials(expandedFile, numFineTags + 2, language);
		String query1 = "VERB";
		String query2 = ".";
		
		int eQuery1 = -1;
		int eQuery2 = -1;
		for (int i = 0; i < numFineTags + 2; i++) {
			String tag = "T" + i;
			if (fineToCoarseMap.get(tag).equals(query1)) {
				eQuery1 = i;
			}
			if (fineToCoarseMap.get(tag).equals(query2)) {
				eQuery2 = i;
			}
		}		
		System.out.println("Transition prob:" + expandedMults[eQuery1][eQuery2]);		
	}
	
	public static void computationWithMapDictionary(String[] args) {
		String avgMultFile = args[0];
		String language = args[1];		
		String mapFile = 
			"/usr2/dipanjan/experiments/SSL/UnsupervisedPOS/data/forgi/maps/" 
			+ AverageMultinomials.languageMap.get(language) + "-fine-universal.map";
		System.out.println("Mapfile: " + mapFile);
		Set<String> validTags = AverageMultinomials.getValidTags(mapFile);		
		validTags.add("START");
		validTags.add("END");
		String[] validTagArray = new String[validTags.size()];
		validTags.toArray(validTagArray);
		Arrays.sort(validTagArray);
		System.out.println("Valid tags:");
		for (String tag: validTagArray) {
			System.out.println(tag);
		}		
		System.out.println("\n\n");
		double[][] coarseMults = AverageMultinomials.readGoldMultinomials(avgMultFile, validTagArray.length, language);
		Map<String, String> map = getMap(mapFile);
		Map<String, String> fineToCoarseMap = new HashMap<String, String>();
		Map<Integer, Integer> coarseNumMap = new HashMap<Integer, Integer>();
		
		Set<String> keys = map.keySet();
		int keySize = keys.size();
		String[] keyArray = new String[keySize];
		keys.toArray(keyArray);
		Arrays.sort(keyArray);
		int count = 0;
		for (String key: keyArray) {
			String coarseTag = map.get(key);
			int cIndex = Arrays.binarySearch(validTagArray, coarseTag);
			String fineTag = key;
			fineToCoarseMap.put(fineTag, coarseTag);
			if (coarseNumMap.containsKey(cIndex)) {
				int num = coarseNumMap.get(cIndex);
				coarseNumMap.put(cIndex, num + 1);
			} else {
				coarseNumMap.put(cIndex, 1);
			}
			count++;
		}
		int numFineTags = map.size();
		int cIndex = Arrays.binarySearch(validTagArray, "START");
		coarseNumMap.put(cIndex, 1);
		int numTag = numFineTags + 1;
		String fineTag = "START";
		fineToCoarseMap.put(fineTag, "START");
		
		cIndex = Arrays.binarySearch(validTagArray, "END");
		coarseNumMap.put(cIndex, 1);
		numTag = numFineTags;
		fineTag = "END";
		fineToCoarseMap.put(fineTag, "END");
		
		keys = fineToCoarseMap.keySet();
		keySize = keys.size();
		keyArray = new String[keySize];
		keys.toArray(keyArray);
		Arrays.sort(keyArray);
		for (String key: keyArray) {
			System.out.println(key + "\t" + fineToCoarseMap.get(key));
		}
		
		double[][] res = new double[numFineTags + 2][numFineTags + 2];
		String outFile = avgMultFile + ".expanded";
		String outFile2 = outFile + ".explicit";
		Random rand = rands[0];
		double range = 0.0002;
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
		BufferedWriter bWriter2 = BasicFileIO.openFileToWrite(outFile2);
		for (int i = 0; i < numFineTags + 2; i++) {
			String line = "";
			double sum = 0.0;
			for (int j = 0; j < numFineTags + 2; j++) {
				String firstC =  fineToCoarseMap.get(keyArray[i]);
				String secondC =  fineToCoarseMap.get(keyArray[j]);	
				int firstIndex = 
					Arrays.binarySearch(validTagArray, firstC);
				int secondIndex = 
					Arrays.binarySearch(validTagArray, secondC);
				double val = coarseMults[firstIndex][secondIndex];
				int numMaps = coarseNumMap.get(secondIndex);
				val = val / (double)numMaps;
				// double randVal = rand.nextDouble();
				// val = val + (range*randVal);
				res[i][j] = val;
				sum += res[i][j];
			}			
			for (int j = 0; j < numFineTags + 2; j++) {
				res[i][j] = res[i][j] / sum;
				String line2 = keyArray[i] + " " + keyArray[j] + " " + res[i][j];
				BasicFileIO.writeLine(bWriter2, line2);
				line += res[i][j] + " ";
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
		BasicFileIO.closeFileAlreadyWritten(bWriter2);
	}
	
	public static void computationWithMap(String[] args) {
		String avgMultFile = args[0];
		String language = args[1];		
		String mapFile = 
			"/usr2/dipanjan/experiments/SSL/data/maps/" 
			+ AverageMultinomials.languageMap.get(language) + "-fine-universal.map";
		System.out.println("Mapfile: " + mapFile);
		Set<String> validTags = AverageMultinomials.getValidTags(mapFile);		
		validTags.add("START");
		validTags.add("END");
		String[] validTagArray = new String[validTags.size()];
		validTags.toArray(validTagArray);
		Arrays.sort(validTagArray);
		System.out.println("Valid tags:");
		for (String tag: validTagArray) {
			System.out.println(tag);
		}		
		double[][] coarseMults = AverageMultinomials.readGoldMultinomials(avgMultFile, validTagArray.length, language);
		Map<String, String> map = getMap(mapFile);
		Map<String, String> fineToCoarseMap = new HashMap<String, String>();
		Map<Integer, Integer> coarseNumMap = new HashMap<Integer, Integer>();
		
		Set<String> keys = map.keySet();
		int keySize = keys.size();
		String[] keyArray = new String[keySize];
		keys.toArray(keyArray);
		Arrays.sort(keyArray);
		int count = 0;
		for (String key: keyArray) {
			String coarseTag = map.get(key);
			int cIndex = Arrays.binarySearch(validTagArray, coarseTag);
			String fineTag = "T"+count;
			fineToCoarseMap.put(fineTag, coarseTag);
			if (coarseNumMap.containsKey(cIndex)) {
				int num = coarseNumMap.get(cIndex);
				coarseNumMap.put(cIndex, num + 1);
			} else {
				coarseNumMap.put(cIndex, 1);
			}
			count++;
		}
		int numFineTags = map.size();
		int cIndex = Arrays.binarySearch(validTagArray, "START");
		coarseNumMap.put(cIndex, 1);
		int numTag = numFineTags + 1;
		String fineTag = "T"+numTag;
		fineToCoarseMap.put(fineTag, "START");
		
		cIndex = Arrays.binarySearch(validTagArray, "END");
		coarseNumMap.put(cIndex, 1);
		numTag = numFineTags;
		fineTag = "T"+numTag;
		fineToCoarseMap.put(fineTag, "END");
		
		keys = fineToCoarseMap.keySet();
		keySize = keys.size();
		keyArray = new String[keySize];
		keys.toArray(keyArray);
		Arrays.sort(keyArray);
		for (String key: keyArray) {
			System.out.println(key + "\t" + fineToCoarseMap.get(key));
		}
		
		double[][] res = new double[numFineTags + 2][numFineTags + 2];
		String outFile = avgMultFile + ".expanded";
		Random rand = rands[0];
		double range = 0.0002;
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);		
		for (int i = 0; i < numFineTags + 2; i++) {
			String line = "";
			double sum = 0.0;
			for (int j = 0; j < numFineTags + 2; j++) {
				String firstC = fineToCoarseMap.get("T"+i);	
				String secondC = fineToCoarseMap.get("T"+j);	
				int firstIndex = 
					Arrays.binarySearch(validTagArray, firstC);
				int secondIndex = 
					Arrays.binarySearch(validTagArray, secondC);
				double val = coarseMults[firstIndex][secondIndex];
				int numMaps = coarseNumMap.get(secondIndex);
				val = val / (double)numMaps;
				// double randVal = rand.nextDouble();
				// val = val + (range*randVal);
				res[i][j] = val;
				sum += res[i][j];
			}			
			for (int j = 0; j < numFineTags + 2; j++) {
				res[i][j] = res[i][j] / sum;
				line += res[i][j] + " ";
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);		
	}	
	
	public static void plainComputation(String[] args) {
		String avgMultFile = args[0];
		int numFineTags = new Integer(args[1]);
		String outFile = args[2];
		
		int numActualCoarseTags = ComputeTransitionMultinomials.COARSE_TAGS.length - 2;
		int ftagPerCTag = numFineTags / numActualCoarseTags;
		int leftOverTags = numFineTags % numActualCoarseTags;
		
		Arrays.sort(ComputeTransitionMultinomials.COARSE_TAGS);		
		Map<String, String> fineToCoarseMap = new HashMap<String, String>();
		Map<Integer, Integer> coarseNumMap = new HashMap<Integer, Integer>();
		int ftagCount = 0;
		for (int i = 0; i < ComputeTransitionMultinomials.COARSE_TAGS.length; i++) {
			if (ComputeTransitionMultinomials.COARSE_TAGS[i].equals("START")) {
				int numTag = numFineTags + 1;
				fineToCoarseMap.put("T"+numTag, ""+ComputeTransitionMultinomials.COARSE_TAGS[i]);
				coarseNumMap.put(i, 1);
			} else if (ComputeTransitionMultinomials.COARSE_TAGS[i].equals("END")) {
				int numTag = numFineTags;
				fineToCoarseMap.put("T"+numTag, ""+ComputeTransitionMultinomials.COARSE_TAGS[i]);
				coarseNumMap.put(i, 1);
			} else if (ComputeTransitionMultinomials.COARSE_TAGS[i].equals("VERB")) {
				for (int k = 0; k < ftagPerCTag + leftOverTags; k++) {
					int numTag = ftagCount;
					fineToCoarseMap.put("T"+numTag, ""+ComputeTransitionMultinomials.COARSE_TAGS[i]);
					ftagCount++;
				}
				coarseNumMap.put(i, ftagPerCTag + leftOverTags);
			} else {
				for (int k = 0; k < ftagPerCTag; k++) {
					int numTag = ftagCount;
					fineToCoarseMap.put("T"+numTag, ""+ComputeTransitionMultinomials.COARSE_TAGS[i]);
					ftagCount++;
				}
				coarseNumMap.put(i, ftagPerCTag);
			}
		}
		double[][] arr = readMultinomials(avgMultFile);
		double[][] res = new double[numFineTags + 2][numFineTags + 2];
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
		for (int i = 0; i < numFineTags + 2; i++) {
			double sum = 0.0;
			String line = "";
			for (int j = 0; j < numFineTags + 2; j++) {
				String firstC = fineToCoarseMap.get("T"+i);	
				String secondC = fineToCoarseMap.get("T"+j);	
				int firstIndex = 
					Arrays.binarySearch(ComputeTransitionMultinomials.COARSE_TAGS, firstC);
				int secondIndex = 
					Arrays.binarySearch(ComputeTransitionMultinomials.COARSE_TAGS, secondC);
				double val = arr[firstIndex][secondIndex];
				int numMaps = coarseNumMap.get(secondIndex);
				val = val / (double)numMaps;
				res[i][j] = val;
				line += res[i][j] + " ";
				sum += res[i][j];
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
			System.out.println(sum);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}
	
	
	private static double[][] readMultinomials(String file) {
		int multlen = ComputeTransitionMultinomials.COARSE_TAGS.length;
		double[][] res = new double[multlen][multlen];
		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < res.length; j++) {
				res[i][j] = 0.0;
			}
		}
		BufferedReader bReader = BasicFileIO.openFileToRead(file);
		String line = BasicFileIO.getLine(bReader);
		int count = 0;
		while (line != null) {
			String[] toks = line.trim().split(" ");
			if (toks.length != multlen) {
				System.out.println("Problem. Line:" + line);
				System.exit(-1);
			}
			for (int j = 0; j < toks.length; j++) {
				res[count][j] += new Double(toks[j]);
			}
			count++;
			line = BasicFileIO.getLine(bReader);
		}
		if (count != multlen) {
			System.out.println("Problem.");
			System.exit(-1);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		return res;
	}
	
	public static Map<String, String> getMap(String file) {
		Map<String, String> map = new HashMap<String, String>();
		try {
			BufferedReader bReader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = bReader.readLine()) != null) {
				String[] arr = getTokens(line);
				if (arr.length != 2) {
					System.out.println("Length of array is not 2:" + line +". Exiting.");
					System.exit(-1);
				}
				map.put(arr[0], arr[1]);
			}
			bReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return map;
	}
	
	public static String[] getTokens(String line) {
		StringTokenizer st = new StringTokenizer(line, "\t", true);
		ArrayList<String> list = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String tok = st.nextToken().trim();
			if (tok.equals("")) {
				continue;
			}
			list.add(tok);
		}
		int size = list.size();
		String[] arr = new String[size];
		list.toArray(arr);
		return arr;
	}
}