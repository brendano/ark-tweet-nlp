package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;

import cmu.arktweetnlp.util.BasicFileIO;

import fig.basic.Pair;

public class ProjectAlignedTags {
	public static void main(String[] args) {
		mxpost(args);
	}	
	
	private static void mxpost (String[] args) {
		String language = args[0]; 
		String directory = "/mal2/dipanjan/experiments/SSL/UnsupervisedPOS/data/epaligned/";
		String englishData = "";
		String foreignData = "";
		String alignments = "";
		if (language.equals("tr")) {
			englishData = directory + 
			"original.filt.en.replaced.tpos";
			foreignData = directory + 
			"original.filt.tr";
			alignments = directory + 
			"original.filt.tr-en.gdfa";
		} else {
			englishData = 
			directory +
			"europarl-v6."+language+"-en.en.tokenized.lc.filt.replaced.tpos";
			foreignData = directory + 
			"europarl-v6."+language+"-en." + language + ".tokenized.lc.filt";
			alignments = 
			directory + 
			"europarl-v6."+language+"-en.gdfa";
		}
		String outFile = directory + language + ".projected.tpos";
		String mapFile = "/mal2/dipanjan/experiments/SSL/data/maps/en-fine-universal.map";
		Map<String, String> tagMap = readMap(mapFile);
		String[] posArr = findMostCommonTags(tagMap, englishData);
		BufferedReader eReader = 
			BasicFileIO.openFileToRead(englishData);
		BufferedReader fReader = 
			BasicFileIO.openFileToRead(foreignData);
		BufferedReader aReader = 
			BasicFileIO.openFileToRead(alignments);
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
		String eLine = BasicFileIO.getLine(eReader);
		Map<Integer, Integer> aMap = new HashMap<Integer, Integer>();
		int countLine = 0;
		while (eLine != null) {
			//System.out.println(eLine);
			ArrayList<String> elist = getTokens(eLine);
			for (int k = 0; k < elist.size(); k++) {
				int ind = elist.get(k).lastIndexOf("_");
				String pos = elist.get(k).substring(ind + 1);
				String mapPos = tagMap.get(pos);
				elist.set(k, mapPos);
			}
			String fLine = BasicFileIO.getLine(fReader);
			//System.out.println(fLine);
			ArrayList<String> fList = getTokens(fLine);
			String aLine = BasicFileIO.getLine(aReader);
			ArrayList<String> aList = getTokens(aLine);
			aMap.clear();
			for (String a: aList) {
				a = a.trim();
				String[] toks = a.split("-");
				int f = new Integer(toks[0]);
				int e = new Integer(toks[1]);
				String pos = elist.get(e);
				if (aMap.containsKey(f)) {
					int ind = aMap.get(f);
					String existingPos = elist.get(ind);
					int oldIndex = search(posArr, existingPos);
					if (oldIndex == -1) {
						System.out.println("Problem, existingPos:" + existingPos + " line: " + eLine);
						System.exit(-1);
					}
					int index = search(posArr, pos);
					if (index == -1) {
						System.out.println("Problem2, pos:" + pos +" line: " + eLine);
						System.exit(-1);
					}
					if (index < oldIndex) {
						aMap.put(f, e);
					}
				} else {
					aMap.put(f, e);
				}
			}
			String out = "";
			for (int i = 0; i < fList.size(); i++) {
				String tok = fList.get(i);
				if (aMap.containsKey(i)) {
					int k = aMap.get(i);
					int ind = elist.get(k).lastIndexOf("_");
					String pos = elist.get(k).substring(ind + 1);
					out += tok + "_" + pos + " ";
				} else {
					out += tok + "_NOUN ";
				}
			}
			out = out.trim();
			BasicFileIO.writeLine(bWriter, out);
			eLine = BasicFileIO.getLine(eReader);
			countLine++;
			if (countLine % 1000 == 0) {
				System.out.print(countLine + " ");
			}
		}			
		System.out.println();
		BasicFileIO.closeFileAlreadyRead(eReader);
		BasicFileIO.closeFileAlreadyRead(fReader);
		BasicFileIO.closeFileAlreadyRead(aReader);
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}
	
	private static int search(String[] posArr, String existingPos) {
		for (int i = 0; i < posArr.length; i ++) {
			if (existingPos.equals(posArr[i])) {
				return i;
			}
		}
		return -1;
	}
	
	private static String[] findMostCommonTags(Map<String, String> tagMap,
											  String englishData) {
		System.out.println("Finding most common tags...");
		Map<String, Integer> tagCountMap = 
			new HashMap<String, Integer>();
		BufferedReader bReader = 
			BasicFileIO.openFileToRead(englishData);
		String line = BasicFileIO.getLine(bReader);
		int count = 0;
		while (line != null) {
			ArrayList<String> list = getTokens(line);
			for (String tok: list) {
				int ind = tok.lastIndexOf("_");
				String pos = tok.substring(ind + 1);
				if (!tagMap.containsKey(pos)) {
					System.out.println("Map does not contain tag:" + pos);
					System.exit(-1);
				}
				String mPos = tagMap.get(pos);
				if (tagCountMap.containsKey(mPos)) {
					int c = tagCountMap.get(mPos);
					tagCountMap.put(mPos, c+1);
				} else {
					tagCountMap.put(mPos, 1);
				}
				count++;
				if (count % 100000 == 0) {
					System.out.print(".");
				}
			}
			line = BasicFileIO.getLine(bReader);
		}
		System.out.println();
		BasicFileIO.closeFileAlreadyRead(bReader);
		System.out.println("Finished finding most common tags...");
		Pair<String, Integer>[] parr = new Pair[tagCountMap.size()];
		Set<String> keys = tagCountMap.keySet();
		System.out.println("Number of pos tags:" + keys.size());
		int i = 0;
		for (String key: keys) {
			count = tagCountMap.get(key);
			parr[i] = new Pair<String, Integer>(key, count);
			i++;     
		}	
		Comparator<Pair<String, Integer>> comp = new Comparator<Pair<String, Integer>>() {
			public int compare(Pair<String, Integer> o1,
					Pair<String, Integer> o2) {
				if (o1.getSecond() > o2.getSecond()) 
					return -1;
				else if (o1.getSecond() == o2.getSecond())
					return 0;
				else 
					return 1;
			}
		};
		Arrays.sort(parr, comp);
		String[] posArray = new String[parr.length];
		i = 0;
		for (Pair<String, Integer> p: parr) {
			posArray[i] = new String(p.getFirst());
			System.out.println(posArray[i]);
			i++;
		}
		return posArray;
	}
	
	public static ArrayList<String> getTokens(String line) {
		StringTokenizer st = new StringTokenizer(line.trim(), " \t", true);
		ArrayList<String> tokList = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String tok = st.nextToken().trim();
			if (tok.equals("")) {
				continue;
			}
			tokList.add(tok);
		}
		return tokList;
	}
	
	private static Map<String, String> readMap(String mapFile) {
		Map<String, String> map = new HashMap<String, String>();
		BufferedReader bReader = 
			BasicFileIO.openFileToRead(mapFile);
		String line = BasicFileIO.getLine(bReader);
		while (line != null) {
			StringTokenizer st = new StringTokenizer(line.trim(), " \t", true);
			ArrayList<String> tokList = new ArrayList<String>();
			while (st.hasMoreTokens()) {
				String tok = st.nextToken().trim();
				if (tok.equals("")) {
					continue;
				}
				tokList.add(tok);
			}
			if (tokList.size() != 2) {
				System.out.println("Problem with line: " + line);
				System.exit(-1);
			}
			map.put(tokList.get(0), tokList.get(1));
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		return map;
	}	
}