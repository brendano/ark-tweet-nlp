package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import cmu.arktweetnlp.util.BasicFileIO;

import edu.cmu.cs.lti.ark.ssl.pos.TabSeparatedFileReader;
import fig.basic.Pair;

public class BuildTagDictionaries {
	
	public static void main(String[] args) {
//		String tbDirectory = "/home/dipanjan/work/spring2011/UnsupPOSTagging/data/forgi/treebanks";
//		String[] languages = {
//				"BULGARIAN",
//				"DANISH",
//				"DUTCH",
//				"GREEK",
//				"JAPANESE",
//				"PORTUGUESE",
//				"SLOVENE",
//				"SPANISH",
//				"SWEDISH",
//				"TURKISH"
//		};
//		int[] sizes = {10, 50, 100, 500, 200000};		
//		
//		for (int i = 0; i < languages.length; i++) {
//			for (int s = 0; s < sizes.length; s++) {
//				System.out.println(languages[i] + " " + sizes[s]);
//				buildDictionary(tbDirectory, languages[i], sizes[s]);
//				System.out.println("\n\n");
//			}
//		}	
		
		String dir = "/home/dipanjan/Downloads";
		buildDictionary(dir, "ENGLISH", 100000);
		buildDictionary(dir, "DANISH", 100000);
		buildDictionary(dir, "GREEK", 100000);
	}

	private static void buildDictionary(String directory, String language, int size) {
		// String trainFile = directory + "/" + language + "-train.tb.pos";
		// String dictFile = directory + "/" + language + "-pos.dict." + size;
		
		String trainFile = directory + "/" + language + "-all.tb.upos";
		String dictFile = directory + "/" + language + "-upos.dict." + size;
		
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
		String[] wordArray = new String[wordSet.size()];
		wordSet.toArray(wordArray);
		Arrays.sort(wordArray);
		
		Set<Integer>[] wholeMap = new Set[wordArray.length];
		for (int i = 0; i < wholeMap.length; i++) {
			wholeMap[i] = new HashSet<Integer>();
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
				wholeMap[wordIndex].add(posIndex);
			}
		}
		
		Set<Integer>[] posThresholdArray = new Set[posArray.length];
		for (int i = 0; i < posArray.length; i++) {
			posThresholdArray[i] = new HashSet<Integer>();
		}
		Set<Integer> wordIndices = new HashSet<Integer>();
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
				if (posThresholdArray[posIndex].size() < size) {
					posThresholdArray[posIndex].add(wordIndex);
					wordIndices.add(wordIndex);
				}
			}
		}
		
		String[] dictArray = new String[wordIndices.size()];
		int count = 0;
		for (int index: wordIndices) {
			dictArray[count] = wordArray[index];
			count++;
		}
		Arrays.sort(dictArray);
		System.out.println("Size of dictionary:" + wordIndices.size());
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(dictFile);
		for (String word: dictArray) {
			int index = Arrays.binarySearch(wordArray, word);
			String line = wordArray[index] + "\t";
			Set<Integer> poss = wholeMap[index];
			for (Integer pos: poss) {
				line += posArray[pos] + " ";
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}
}