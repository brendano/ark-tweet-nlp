package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import cmu.arktweetnlp.util.BasicFileIO;

import edu.cmu.cs.lti.ark.ssl.pos.TabSeparatedFileReader;
import fig.basic.Pair;

public class WSJTagDictionary {
	
	public static void main(String[] args) {
		String root = "/Users/dipanjand/work/fall2010/SSL/pos_tagging_data";
		String[] files = {"wsj-00-18.MRG.tab", "wsj-19-21.MRG.tab", "wsj-22-24.MRG.tab"};
		
		Map<String, HashSet<String>> dict = 
			new HashMap<String, HashSet<String>>();
		
		for (String file: files) {
			String inFile = root + "/" + file;
			Collection<Pair<List<String>, List<String>>> 
			 sequences = TabSeparatedFileReader.readPOSSeqences(inFile, Integer.MAX_VALUE, Integer.MAX_VALUE);
			for (Pair<List<String>, List<String>> seq: sequences) {
				List<String> words = seq.getFirst();
				List<String> pos = seq.getSecond();
				
				int size = words.size();
				for (int i = 0; i < size; i++) {
					String word = words.get(i).toLowerCase();
					String tag = pos.get(i);
					if (dict.containsKey(word)) {
						HashSet<String> tags = dict.get(word);
						tags.add(tag);
					} else {
						HashSet<String> tags = new HashSet<String>();
						tags.add(tag);
						dict.put(word, tags);
					}
				}
				System.out.println(words);
			}
			System.out.println("Finished with:" + file);
		}
		String outFile = root + "/wsj.dict";
		Set<String> words = dict.keySet();
		String[] wordArr = new String[words.size()];
		words.toArray(wordArr);
		Arrays.sort(wordArr);
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
		for (String word: wordArr) {
			String line = word + "\t";
			Set<String> set = dict.get(word);
			for (String tag: set) {
				line += tag + " ";
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}	
}