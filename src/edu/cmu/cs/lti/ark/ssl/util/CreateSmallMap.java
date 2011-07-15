package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Set;

public class CreateSmallMap {
	public static void main(String[] args) {
		String treebank = 
			"/home/dipanjan/work/spring2011/UnsupPOSTagging/data/corpora/" +
			args[0] + "_train.conll";
		String mapFile = "/home/dipanjan/work/spring2011/UnsupPOSTagging/data/maps/" + 
			args[1] + "-fine-universal.map";
		Map<String, String> map = 
			getMap(mapFile);
		Map<String, Set<String>> treebankMap = getTreebankMap(treebank);
		Set<String> keys = treebankMap.keySet();
		for (String key: keys) {
			Set<String> set = treebankMap.get(key);
			System.out.print(key + ":\t");
			Set<String> mappedSet = new HashSet<String>();
			for (String val: set) {
				System.out.print(val + " ");
				mappedSet.add(map.get(val));
				System.out.print("("+map.get(val)+") ");
			}
			System.out.println();
//			if (mappedSet.size() > 1) {
//				System.out.println("Problem with: " + key);
//				System.exit(-1);
//			}
		}
	}
	
	private static Map<String, Set<String>> getTreebankMap(String treebank) {
		Map<String, Set<String>> tbMap = new HashMap<String, Set<String>>();
		try {
			BufferedReader bReader = new BufferedReader(new FileReader(treebank));
			String line = null;
			while ((line = bReader.readLine()) != null) {
				line = line.trim();
				if (line.equals("")) {
					continue;
				}
				String[] arr = getTokens(line);
				String cpos = arr[3];
				String fpos = arr[4];
				if (tbMap.containsKey(cpos)) {
					Set<String> set = tbMap.get(cpos);
					set.add(fpos);
					tbMap.put(cpos, set);
				} else {
					Set<String> set = new HashSet<String>();
					set.add(fpos);
					tbMap.put(cpos, set);
				}
			}
			bReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return tbMap;
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