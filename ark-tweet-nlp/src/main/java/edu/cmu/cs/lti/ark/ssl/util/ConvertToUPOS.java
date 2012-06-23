package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class ConvertToUPOS {
	public static final String pathToCorpora = 
		"/home/dipanjan/work/spring2011/UnsupPOSTagging/data/forgi/treebanks";
	public static final String mapDirectory = 
		"/home/dipanjan/work/spring2011/UnsupPOSTagging/data/forgi/maps";	
	public static final String[] languages = 
	{   "bulgarian",
		"danish",
		"czech",
		"dutch", 
		"english", 
		"german",
		"greek",
		"italian",
		"japanese", 
		"portuguese", 
		"slovene",
		"spanish",
		"swedish",
		"turkish"};
	public static final String[] abbvs = 
	{"bg", "da", "cs", "nl", "en", "de", "el", "it", "ja", "pt", "sl", "es", "sv", "tr"};
	
	public static void main(String[] args) {
		// convertToUPOSTreebanks();
		String file = "/mal2/dipanjan/experiments/SSL/data/cameraready/output/"+
					  "ENGLISH-fhmm-reg.1.0.tag.dict.test.output";
		String map = "/mal2/dipanjan/experiments/SSL/data/cameraready/data/"+
		  	"ENGLISH-fine-universal.map";
		convertToUPOSOutput(file, map);
	}	
	
	public static void convertToUPOSOutput(String file, String map) {
		Map<String, String> fcMap = getMap(map);
		String outFile = file + ".upos";
		convertFile(file, outFile, fcMap, 1);
	}

	private static void convertToUPOSTreebanks() {
		int len = languages.length;
		for (int i = 0; i < len; i++) {
			System.out.println("Language: " + languages[i]);
			String trainingFile = pathToCorpora + "/" + languages[i].toUpperCase() + "-train.tb.pos";
			String testFile = pathToCorpora + "/" + languages[i].toUpperCase() + "-test.tb.pos";
			String mapFile = mapDirectory + "/" + abbvs[i] + "-fine-universal.map";
			Map<String, String> map = getMap(mapFile);
			System.out.println(map.size());
			String trainOutFile = pathToCorpora + "/" + languages[i].toUpperCase() + "-train.tb.upos";
			String testOutFile = pathToCorpora + "/" + languages[i].toUpperCase() + "-test.tb.upos";
			File f1 = new File(trainingFile);
			File f2 = new File(testFile);
			if (f1.exists()) {
				System.out.println("Train:");
				convertFile(trainingFile, trainOutFile, map, i);
			} else {
				System.out.println(trainingFile + " does not exist.");
			}
			if (f2.exists()) {
				System.out.println("Test:");
				convertFile(testFile, testOutFile, map, i);
			} else {
				System.out.println(testFile + " does not exist.");
			}
		}
	}
	
	public static void convertFile(String file, 
			String outFile, 
			Map<String, String> map,
			int index) {
		try {
			BufferedReader bReader = new BufferedReader(new FileReader(file));
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(outFile));
			String line = null;
			while ((line = bReader.readLine()) != null) {
				if (line.trim().equals("")) {
					bWriter.write("\n");
					continue;
				}
				String[] toks = getTokens(line);
				String word = toks[0];
				String POS = toks[1];
				if (!map.containsKey(POS)) {
					System.out.println("Problem with file. " + POS + " not found. " + line);
					System.exit(-1);
				}
				String cpos = map.get(POS);
				bWriter.write(word + "\t" + cpos + "\n");
			}
			bReader.close();
			bWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
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