package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CheckUniquenessOfMaps {
	public static void main(String[] args) {
		// uniqueness();
		checkPresenceInTrainingData();
	}
	
	public static void checkPresenceInTrainingData() {
		String[] languages = ConvertToUPOS.languages;
		String[] abbvs = ConvertToUPOS.abbvs;
		for (int i = 0; i < languages.length; i++) {
			String trainingFile = 
				ConvertToUPOS.pathToCorpora + "/" + languages[i].toUpperCase() + "-train.tb.pos";
			System.out.println("Language: " + abbvs[i]);
			String mapFile = ConvertToUPOS.mapDirectory + "/" + abbvs[i] + "-fine-universal.map";
			Map<String, String> map = ConvertToUPOS.getMap(mapFile);
			Set<String> keys = new HashSet<String>(map.keySet());
			try {
				BufferedReader bReader = new BufferedReader(new FileReader(trainingFile));
				String line = null;
				while ((line = bReader.readLine()) != null) {
					if (line.trim().equals("")) {
						continue;
					}
					String[] toks = ConvertToUPOS.getTokens(line);
					String POS = toks[1];
					if (keys.contains(POS)) {
						keys.remove(POS);
					}
				}
				bReader.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			if (keys.size() > 0) {
				System.out.println("Size of remaining keys more than 0.");
				String[] arr = new String[keys.size()];
				keys.toArray(arr);
				Arrays.sort(arr);
				for (String key: arr) {
					System.out.println(key);
				}
				System.exit(-1);
			}
			System.out.println("\n\n");
		}
	}
	
	public static void uniqueness() {
		String dir = "/home/dipanjan/work/spring2011/UnsupPOSTagging/data/forgi/maps";
		File f = new File(dir);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File arg0, String arg1) {
				return arg1.endsWith("map");
			}			
		};		
		String[] files = f.list(filter);
		for (String file: files) {
			file = dir + "/" + file;
			System.out.println("File: " + file);
			BufferedReader bReader = BasicFileIO.openFileToRead(file);
			String line = BasicFileIO.getLine(bReader);
			Set<String> fpos = new HashSet<String>();
			while (line != null) {
				String[] toks = ConvertToUPOS.getTokens(line);
				if (fpos.contains(toks[0])) {
					System.out.println(toks[0] + " already in map. Exiting.");
					System.exit(-1);
				}
				fpos.add(toks[0]);
				line = BasicFileIO.getLine(bReader);
			}
			BasicFileIO.closeFileAlreadyRead(bReader);
		}
	}
} 