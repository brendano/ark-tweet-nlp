package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import cmu.arktweetnlp.util.BasicFileIO;

public class ExpandCoarseTagDictionary {
	public static void main(String[] args) {
		String directory = "/home/dipanjan/Downloads";
		expand(directory, "ENGLISH");
		expand(directory, "DANISH");
		expand(directory, "GREEK");
	}
	
	public static void expand(String directory, String language) {
		String dictionary = directory + "/" + language + "-upos.dict.100000";
		String outDictionary = directory + "/" + language + "-pos.dict";
		String map = directory + "/" + language + "-fine-universal.map";
		
		Map<String, Set<String>> mapping = readMap(map);
		BufferedReader bReader = BasicFileIO.openFileToRead(dictionary);
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outDictionary);
		String line = BasicFileIO.getLine(bReader);
		while (line != null) {
			System.out.println(line);
			ArrayList<String> toks = getTokens(line);
			String outLine = toks.get(0) + "\t";
			for (int i = 1; i < toks.size(); i++) {
				String pos = toks.get(i);
				Set<String> set = mapping.get(pos);
				if (set == null) {
					System.out.println("Problem. Exiting.");
					System.exit(-1);
				}
				for (String fpos: set) {
					outLine += fpos + " ";
				}
			}
			outLine = outLine.trim();
			BasicFileIO.writeLine(bWriter, outLine);
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}

	private static Map<String, Set<String>> readMap(String mapFile) {
		Map<String, Set<String>> map = new HashMap<String, Set<String>>();
		BufferedReader bReader = BasicFileIO.openFileToRead(mapFile);
		String line = BasicFileIO.getLine(bReader);
		while (line != null) {
			ArrayList<String> toks = getTokens(line);
			if (toks.size() != 2) {
				System.out.println("Problem.");
				System.exit(-1);
			}
			String f = toks.get(0);
			String c = toks.get(1);
			if (map.containsKey(c)) {
				Set<String> set = map.get(c);
				set.add(f);
				map.put(c, set);
			} else {
				Set<String> set = new HashSet<String>(); 
				set.add(f);
				map.put(c, set);
			}
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		return map;
	}
	
	public static ArrayList<String> getTokens(String line) {
		ArrayList<String> res = new ArrayList<String>();
		line = line.trim();
		StringTokenizer st = new StringTokenizer(line);
		while (st.hasMoreTokens()) {
			String tok = st.nextToken().trim();
			if (tok.equals("")) {
				continue;
			}
			res.add(tok);
		}
		return res;
	}
	
}
 

