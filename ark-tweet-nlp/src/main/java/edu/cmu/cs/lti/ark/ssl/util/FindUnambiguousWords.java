package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cmu.arktweetnlp.util.BasicFileIO;

import edu.berkeley.nlp.util.ArrayUtil;
import edu.cmu.cs.lti.ark.ssl.pos.PennTreeBankPOSSequenceReader;
import edu.cmu.cs.lti.ark.ssl.pos.TabSeparatedFileReader;
import fig.basic.Pair;


public class FindUnambiguousWords {
	public static void main(String[] args) {
		String tbDirectory = "/home/dipanjan/work/spring2011/UnsupPOSTagging/data/forgi/treebanks";
		/*String[] languages = {
				"BULGARIAN",
				"DANISH",
				"DUTCH",
				"GREEK",
				"JAPANESE",
				"PORTUGUESE",
				"SLOVENE",
				"SPANISH",
				"SWEDISH",
				"TURKISH"
		};*/
		String[] languages = {
				"CZECH",
				"ENGLISH",
				"GERMAN",
				"ITALIAN"
		};
		for (int i = 0; i < languages.length; i++) {
			System.out.println(languages[i]);
			convertToTabSeparatedFormat(tbDirectory, languages[i]);
			// findUnambiguousWords(tbDirectory, languages[i]);
			// checkAmbiguity(tbDirectory, languages[i]);
			// findUnambiguousWords2(tbDirectory, languages[i]);
			System.out.println("\n\n");
		}
	}

	public static void checkAmbiguity(String directory, String language) {
		String trainFile = directory + "/" + language + "-train.tb.pos";
		String dictFile = directory + "/" + language + "-pos.dict";
		Collection<Pair<List<String>, List<String>>> sequences =
			TabSeparatedFileReader.readPOSSeqences(trainFile, 
					Integer.MAX_VALUE, 
					Integer.MAX_VALUE);
		Map<String, Set<String>> map = new HashMap<String, Set<String>>();
		BufferedReader bReader = BasicFileIO.openFileToRead(dictFile);
		String line = BasicFileIO.getLine(bReader);
		while (line != null) {
			String[] toks = line.split("\t");
			Set<String> set = new HashSet<String>();
			map.put(toks[0], set);
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		for (Pair<List<String>, List<String>> seq: sequences) {
			List<String> poss = seq.getSecond();
			List<String> words = seq.getFirst();
			Iterator<String> itr1 = poss.iterator();
			Iterator<String> itr2 = words.iterator();
			while (itr1.hasNext()) {
				String pos = itr1.next();
				String word = itr2.next().toLowerCase();
				if (!map.containsKey(word)) {
					continue;
				}
				Set<String> set = map.get(word);
				set.add(pos);
				map.put(word, set);
			}
		}
		
		Set<String> keys = map.keySet();
		for (String key: keys) {
			Set<String> poss = map.get(key);
			if (poss.size() > 1) {
				System.out.print(key + " :");
				for (String pos: poss) {
					System.out.print(pos + " ");
				}
				System.out.println();
			}
		}
	}
	
	public static void findUnambiguousWords2(String directory, String language) {
		String trainFile = directory + "/" + language + "-train.tb.pos";
		String dictFile = directory + "/" + language + "-pos.dict";
		Collection<Pair<List<String>, List<String>>> sequences =
			TabSeparatedFileReader.readPOSSeqences(trainFile, 
					Integer.MAX_VALUE, 
					Integer.MAX_VALUE);
		Set<String> posSet = new HashSet<String>();
		Set<String> wordSet = new HashSet<String>();
		for (Pair<List<String>, List<String>> seq: sequences) {
			List<String> poss = seq.getSecond();
			for (String pos: poss) {
				posSet.add(pos);
			}
			List<String> words = seq.getFirst();
			for (String word: words) {
				wordSet.add(word.toLowerCase());
			}
		}
		String[] posArray = new String[posSet.size()];
		posSet.toArray(posArray);
		Arrays.sort(posArray);
		System.out.println("Number of pos tags:" + posArray.length);
		String[] wordArray = new String[wordSet.size()];
		wordSet.toArray(wordArray);
		Arrays.sort(wordArray);
		System.out.println("Number of words in vocab:" + wordArray.length);
		
		
		double[][] counts = new double[posArray.length][wordArray.length];
		int[][] wordCounts = new int[wordArray.length][posArray.length];	
		
		ArrayUtil.fill(counts, 0);
		ArrayUtil.fill(wordCounts, 0);
		
		Set<String>[] wholeMap = new Set[wordArray.length];
		for (int i = 0; i < wholeMap.length; i++) {
			wholeMap[i] = new HashSet<String>();
		}		
		for (Pair<List<String>, List<String>> seq: sequences) {
			List<String> poss = seq.getSecond();
			List<String> words = seq.getFirst();
			Iterator<String> itr1 = poss.iterator();
			Iterator<String> itr2 = words.iterator();
			while (itr1.hasNext()) {
				String pos = itr1.next();
				String word = itr2.next().toLowerCase();
				int posIndex = Arrays.binarySearch(posArray, pos);
				int wordIndex = Arrays.binarySearch(wordArray, word);
				counts[posIndex][wordIndex]++;
				wordCounts[wordIndex][posIndex]++;
				wholeMap[wordIndex].add(pos);
			}
		}

		Set<String> coveredPOSs = new HashSet<String>();
		for (int i = 0; i < wordCounts.length; i++) {
			int nonzerocount = 0;
			for (int j = 0; j < wordCounts[i].length; j++) {
				if (wordCounts[i][j] > 0) {
					nonzerocount++;
				}
			}
			if (nonzerocount == 1) {
				int nzindex = -1;
				for (int j = 0; j < wordCounts[i].length; j++) {
					if (wordCounts[i][j] > 0) {
						nzindex = j;
					}
				}
				coveredPOSs.add(posArray[nzindex]);
			}
		}
		// for unambiguous POSs
		String[] unambiguousPOSArray = new String[coveredPOSs.size()];
		coveredPOSs.toArray(unambiguousPOSArray);
		Arrays.sort(unambiguousPOSArray);
		Map<String, Integer>[] countArray = new Map[unambiguousPOSArray.length];
		for (int i = 0; i < countArray.length; i++) {
			countArray[i] = new HashMap<String, Integer>();
		}	
		for (int i = 0; i < wordCounts.length; i++) {
			int nonzerocount = 0;
			for (int j = 0; j < wordCounts[i].length; j++) {
				if (wordCounts[i][j] > 0) {
					nonzerocount++;
				}
			}
			if (nonzerocount == 1) {
				int nzindex = -1;
				for (int j = 0; j < wordCounts[i].length; j++) {
					if (wordCounts[i][j] > 0) {
						nzindex = j;
					}
				}
				int count = wordCounts[i][nzindex];
				String pos = posArray[nzindex];
				int uIndex = Arrays.binarySearch(unambiguousPOSArray, pos);
				countArray[uIndex].put(wordArray[i], count);
			}
		}
		Comparator comp = new Comparator<Pair<String, Integer>>() {
			public int compare(Pair<String, Integer> o1,
					Pair<String, Integer> o2) {
				if (o1.getSecond() > o2.getSecond()) { 
					return -1; 
				} else if (o1.getSecond() == o2.getSecond()) {
					return 0;
				} else
					return 1;
			}
		};
		
		Map<String, Set<String>> dictMap = 
			new HashMap<String, Set<String>>();
		Set<String> alreadySeen = new HashSet<String>();
		Set<String> alreadySeenPOS = new HashSet<String>();
		
		for (int i = 0; i < countArray.length; i++) {
			Map<String, Integer> map = countArray[i];
			int size = map.size();
			Pair<String, Integer>[] arr = new Pair[size];
			Set<String> keys = map.keySet();
			int j = 0;
			for (String key: keys) {
				arr[j] = new Pair<String, Integer>(key, map.get(key));
				j++;
			}
			Arrays.sort(arr, comp);
			String maxWord = arr[0].getFirst();
			Set<String> set = new HashSet<String>();
			set.add(unambiguousPOSArray[i]);
			dictMap.put(maxWord, set);
			alreadySeen.add(maxWord);
			alreadySeenPOS.add(unambiguousPOSArray[i]);			
			System.out.println(unambiguousPOSArray[i] + "\t" + maxWord + "\t" + arr[0].getSecond());
		}		
		
		System.out.println("Size of seen POS after first round: " + alreadySeenPOS.size());
		
		// for POSs with only ambiguous words
		for (int i = 0; i < posArray.length; i++) {
			if (alreadySeenPOS.contains(posArray[i])) {
				continue;
			}
			int numNonZero = 0;
			for (int j = 0; j < wordArray.length; j++) {
				if (counts[i][j] > 0) {
					numNonZero++;
				}
			}
			if (numNonZero == 1) {
				int nonzeroIndex = -1;
				for (int j = 0; j < wordArray.length; j++) {
					if (counts[i][j] > 0) {
						nonzeroIndex = j;
					}
				}
				dictMap.put(wordArray[nonzeroIndex], wholeMap[nonzeroIndex]);
				alreadySeen.add(wordArray[nonzeroIndex]);
				alreadySeenPOS.add(posArray[i]);
			}
		}	
		System.out.println("Size of already seen pos:" + dictMap.size());
		
		for (int i = 0; i < posArray.length; i++) {
			if (alreadySeenPOS.contains(posArray[i])) {
				continue;
			}
			double[] arr = counts[i];
			int ambiguity = Integer.MAX_VALUE;
			for (int j = 0; j < arr.length; j++) {
				if (alreadySeen.contains(wordArray[j]) || arr[j] == 0.0) {
					continue;
				}
				if (wholeMap[j].size() < ambiguity) {
					ambiguity = wholeMap[j].size();
				}
			}
			int maxIndex = -1;
			double maxVal = -Double.MAX_VALUE;
			for (int j = 0; j < arr.length; j++) {
				if (alreadySeen.contains(wordArray[j]) || arr[j] == 0.0 || wholeMap[j].size() > ambiguity) {
					continue;
				}
				if (arr[j] > maxVal) {
					maxIndex = j;
					maxVal = arr[j];
				}
			}		
			System.out.println("Pos: " + posArray[i] + " Maxval:" + maxVal);
			if (maxVal == 0 || maxVal == -Double.MAX_VALUE) {
				System.out.println("Max val is zero. Exiting.");
				System.exit(-1);
			}
			String maxWord = wordArray[maxIndex];
			alreadySeen.add(maxWord);
			dictMap.put(wordArray[maxIndex], wholeMap[maxIndex]);
		}
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(dictFile);
		Set<String> keys = dictMap.keySet();
		System.out.println("Size of keys in dictionary map:" + keys.size());
		if (keys.size() != posSet.size()) {
			System.out.println("Warning: problem with the size of keys. Mismatch with the number of POSs");
			// System.exit(-1);
		}
		for (String key: keys) {
			Set<String> poss = dictMap.get(key);
			String line = key + "\t";
			for (String pos: poss) {
				line += pos + " ";
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
		
	}
	
	public static void findUnambiguousWords(String directory, String language) {
		String trainFile = directory + "/" + language + "-train.tb.pos";
		String dictFile = directory + "/" + language + "-pos.dict";
		Collection<Pair<List<String>, List<String>>> sequences =
			TabSeparatedFileReader.readPOSSeqences(trainFile, 
					Integer.MAX_VALUE, 
					Integer.MAX_VALUE);
		Set<String> posSet = new HashSet<String>();
		Set<String> wordSet = new HashSet<String>();
		for (Pair<List<String>, List<String>> seq: sequences) {
			List<String> poss = seq.getSecond();
			for (String pos: poss) {
				posSet.add(pos);
			}
			List<String> words = seq.getFirst();
			for (String word: words) {
				wordSet.add(word.toLowerCase());
			}
		}
		String[] posArray = new String[posSet.size()];
		posSet.toArray(posArray);
		Arrays.sort(posArray);
		System.out.println("Number of pos tags:" + posArray.length);
		String[] wordArray = new String[wordSet.size()];
		wordSet.toArray(wordArray);
		Arrays.sort(wordArray);
		System.out.println("Number of words in vocab:" + wordArray.length);
		double[][] counts = new double[posArray.length][wordArray.length];

		ArrayUtil.fill(counts, 0);
		for (Pair<List<String>, List<String>> seq: sequences) {
			List<String> poss = seq.getSecond();
			List<String> words = seq.getFirst();
			Iterator<String> itr1 = poss.iterator();
			Iterator<String> itr2 = words.iterator();
			while (itr1.hasNext()) {
				String pos = itr1.next();
				String word = itr2.next().toLowerCase();
				int posIndex = Arrays.binarySearch(posArray, pos);
				int wordIndex = Arrays.binarySearch(wordArray, word);
				counts[posIndex][wordIndex]++;
			}
		}

		Map<String, Set<String>> map = 
			new HashMap<String, Set<String>>();
		Set<String> alreadySeen = new HashSet<String>();
		Set<String> alreadySeenPOS = new HashSet<String>();
		for (int i = 0; i < posArray.length; i++) {
			int numNonZero = 0;
			for (int j = 0; j < wordArray.length; j++) {
				if (counts[i][j] > 0) {
					numNonZero++;
				}
			}
			if (numNonZero == 1) {
				int nonzeroIndex = -1;
				for (int j = 0; j < wordArray.length; j++) {
					if (counts[i][j] > 0) {
						nonzeroIndex = j;
					}
				}
				if (map.containsKey(wordArray[nonzeroIndex])) {
					Set<String> set = map.get(wordArray[nonzeroIndex]);
					set.add(posArray[i]);
					map.put(wordArray[nonzeroIndex], set);
				} else {
					Set<String> set = new HashSet<String>();
					set.add(posArray[i]);
					map.put(wordArray[nonzeroIndex], set);
				}
				alreadySeen.add(wordArray[nonzeroIndex]);
				alreadySeenPOS.add(posArray[i]);
			}
		}	
		System.out.println("Size of already seen pos:" + map.size());
		for (int i = 0; i < posArray.length; i++) {
			if (alreadySeenPOS.contains(posArray[i])) {
				continue;
			}
			double[] arr = counts[i];
			int maxIndex = -1;
			double maxVal = -Double.MAX_VALUE;
			for (int j = 0; j < arr.length; j++) {
				if (alreadySeen.contains(wordArray[j])) {
					continue;
				}
				if (arr[j] > maxVal) {
					maxIndex = j;
					maxVal = arr[j];
				}
			}
			System.out.println("Pos: " + posArray[i] + " Maxval:" + maxVal);
			if (maxVal == 0 || maxVal == -Double.MAX_VALUE) {
				System.out.println("Max val is zero. Exiting.");
				System.exit(-1);
			}
			String maxWord = wordArray[maxIndex];
			alreadySeen.add(maxWord);
			if (map.containsKey(maxWord)) {
				Set<String> set = map.get(maxWord);
				set.add(posArray[i]);
				map.put(maxWord, set);
			} else {
				Set<String> set = new HashSet<String>();
				set.add(posArray[i]);
				map.put(maxWord, set);
			}
		}
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(dictFile);
		Set<String> keys = map.keySet();
		System.out.println("Size of keys in dictionary map:" + keys.size());
		if (keys.size() != posSet.size()) {
			System.out.println("Warning: problem with the size of keys. Mismatch with the number of POSs");
			// System.exit(-1);
		}
		for (String key: keys) {
			Set<String> poss = map.get(key);
			String line = key + "\t";
			for (String pos: poss) {
				line += pos + " ";
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}	

	public static void convertToTabSeparatedFormat(String directory, String language) {
		String trainFile = directory + "/" + language + "-train.gi";
		String testFile = directory + "/" + language + "-test.gi";

		String outTrainFile = directory + "/" + language + "-train.tb.pos";
		String outTestFile = directory + "/" + language + "-test.tb.pos";

		convertFile(trainFile, outTrainFile);
		convertFile(testFile, outTestFile);
	}

	public static void convertFile(String inFile, String outFile) {
		BufferedReader bReader = BasicFileIO.openFileToRead(inFile);
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
		String line = BasicFileIO.getLine(bReader);
		while (line != null) {
			line = line.substring(1);
			line = line.substring(0, line.length()-1);
			String[] toks = line.split(",");
			for (int i = 0; i < toks.length; i++) {
				toks[i] = toks[i].trim();
				if (toks[i].equals("")) {
					continue;
				}
				String[] toks1 = toks[i].split("\\^");
				if (toks1.length != 2) {
					System.out.println(line);
				}
				// System.out.println(toks[i]);
				BasicFileIO.writeLine(bWriter, toks1[1] + "\t" + toks1[0]);
			}
			BasicFileIO.writeLine(bWriter, "");
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		BasicFileIO.closeFileAlreadyWritten(bWriter);		
	}
}