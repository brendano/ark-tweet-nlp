package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AverageMultinomials {
	
	public static Map<String, String> languageMap;
	static {
		languageMap = new HashMap<String, String>();
		languageMap.put("BULGARIAN", "bg");
		languageMap.put("CHINESE", "zh");
		languageMap.put("CZECH", "cs");
		languageMap.put("DANISH", "da");
		languageMap.put("DUTCH", "nl");
		languageMap.put("ENGLISH", "en");
		languageMap.put("GERMAN", "de");
		languageMap.put("ITALIAN", "it");
		languageMap.put("GREEK", "el");
		languageMap.put("PORTUGUESE", "pt");
		languageMap.put("SPANISH", "es");
		languageMap.put("SWEDISH", "sv");
		languageMap.put("SLOVENE", "sl");
		languageMap.put("TURKISH", "tr");
		languageMap.put("JAPANESE", "ja");
	}	
	
	public static void main(String[] args) {
		//combinationWithMapWithoutPunc(args);
		combinationWithMap(args);
	}
	
	public static Set<String> getValidTags(String mapFile) {
		Set<String> map = new HashSet<String>();
		try {
			BufferedReader bReader = new BufferedReader(new FileReader(mapFile));
			String line = null;
			while ((line = bReader.readLine()) != null) {
				String[] arr = CreateSmallMap.getTokens(line);
				if (arr.length != 2) {
					System.out.println("Length of array is not 2:" + line +". Exiting.");
					System.exit(-1);
				}
				map.add(arr[1]);
			}
			bReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return map;
	}
	
	public static void combinationWithMapWithoutPunc(String[] args) {
		int argslen = args.length;
		String language = args[argslen - 1];
		System.out.println("Combining for: " + language);
		if (!languageMap.containsKey(language)) {
			System.out.println("Language: " + language + " not in map. Exiting.");
			System.exit(-1);
		}
		String mapFile = 
			"/home/dipanjan/work/spring2011/UnsupPOSTagging/data/forgi/maps/" + languageMap.get(language) + "-fine-universal.map";
		Set<String> validTags = getValidTags(mapFile);		
		validTags.add("START");
		validTags.add("END");
		String[] validTagArray = new String[validTags.size()];
		validTags.toArray(validTagArray);
		Arrays.sort(validTagArray);
		System.out.println("Valid tags:");
		for (String tag: validTagArray) {
			System.out.println(tag);
		}
		
		String[] coarseTagArray = ComputeTransitionMultinomials.COARSE_TAGS;
		Arrays.sort(coarseTagArray);
		
		double[][] res = new double[validTagArray.length][validTagArray.length];
		String outPrefix = "";
		for (int l = 0; l < args.length - 1; l++) {
			System.out.println(args[l]);
			outPrefix += args[l] + "-";
			String file = 
				"/home/dipanjan/work/spring2011/UnsupPOSTagging/data/multinomials/" + 
				args[l] + ".gold.mults";
			double[][] gold = readGoldMultinomials(file, coarseTagArray.length, args[l]);
			for (int i = 0; i < res.length; i++) {
				for (int j = 0; j < res.length; j++) {
					String from = validTagArray[i];
					String to = validTagArray[j];
					int frmIndex = Arrays.binarySearch(coarseTagArray, from);
					int toIndex = Arrays.binarySearch(coarseTagArray, to);
					res[i][j] += gold[frmIndex][toIndex];
				}
			}
		}
		for (int i = 0; i <res.length; i++) {
			double sum = 0.0;
			for (int j = 0; j < res.length; j++) {
				sum += res[i][j];
			}
			for (int j = 0; j < res.length; j++) {
				res[i][j] /= sum;
			}
		}
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(
				"/home/dipanjan/work/spring2011/UnsupPOSTagging/data/forgi/multinomials/" + outPrefix + "for-" + language + ".avg.multinomials");
		for (int i = 0; i < res.length; i++) {
			String line = "";			
			for (int j = 0; j < res.length; j++) {
				line += ""+res[i][j] + " ";
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
		bWriter = BasicFileIO.openFileToWrite(
				"/home/dipanjan/work/spring2011/UnsupPOSTagging/data/forgi/multinomials/" + outPrefix + "for-" + language + ".avg.multinomials.explicit");
		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < res.length; j++) {
				String line = validTagArray[i] + " " + validTagArray[j] + " " + res[i][j];
				BasicFileIO.writeLine(bWriter, line);
			}
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}
	
	private static void combinationWithMap(String[] args) {
		int argslen = args.length;
		String language = args[argslen - 1];
		System.out.println("Combining for: " + language);
		if (!languageMap.containsKey(language)) {
			System.out.println("Language: " + language + " not in map. Exiting.");
			System.exit(-1);
		}
		String mapFile = 
			"/home/dipanjan/work/spring2011/UnsupPOSTagging/data/maps/" + languageMap.get(language) + "-fine-universal.map";
		Set<String> validTags = getValidTags(mapFile);		
		validTags.add("START");
		validTags.add("END");
		String[] validTagArray = new String[validTags.size()];
		validTags.toArray(validTagArray);
		Arrays.sort(validTagArray);
		System.out.println("Valid tags:");
		for (String tag: validTagArray) {
			System.out.println(tag);
		}
		
		String[] coarseTagArray = ComputeTransitionMultinomials.COARSE_TAGS;
		Arrays.sort(coarseTagArray);
		
		double[][] res = new double[validTagArray.length][validTagArray.length];
		String outPrefix = "";
		for (int l = 0; l < args.length - 1; l++) {
			System.out.println(args[l]);
			outPrefix += args[l] + "-";
			String file = 
				"/home/dipanjan/work/spring2011/UnsupPOSTagging/data/multinomials/" + 
				args[l] + ".gold.mults";
			double[][] gold = readGoldMultinomials(file, coarseTagArray.length, args[l]);
			for (int i = 0; i < res.length; i++) {
				for (int j = 0; j < res.length; j++) {
					String from = validTagArray[i];
					String to = validTagArray[j];
					int frmIndex = Arrays.binarySearch(coarseTagArray, from);
					int toIndex = Arrays.binarySearch(coarseTagArray, to);
					res[i][j] += gold[frmIndex][toIndex];
				}
			}
		}
		for (int i = 0; i <res.length; i++) {
			double sum = 0.0;
			for (int j = 0; j < res.length; j++) {
				sum += res[i][j];
			}
			for (int j = 0; j < res.length; j++) {
				res[i][j] /= sum;
			}
		}
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(
				"/home/dipanjan/work/spring2011/UnsupPOSTagging/data/multinomials/" + outPrefix + "for-" + language + ".avg.multinomials");
		for (int i = 0; i < res.length; i++) {
			String line = "";			
			for (int j = 0; j < res.length; j++) {
				line += ""+res[i][j] + " ";
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}
	
	public static double[][] readGoldMultinomials(String file, int multlen, String lang) {
		double[][] gold = new double[multlen][multlen];
		BufferedReader bReader = BasicFileIO.openFileToRead(file);
		String line = BasicFileIO.getLine(bReader);
		int count = 0;
		while (line != null) {
			String[] toks = line.trim().split(" ");
			if (toks.length != multlen) {
				System.out.println("Problem. Line:" + line + " lang=" + lang);
				System.exit(-1);
			}
			for (int j = 0; j < toks.length; j++) {
				gold[count][j] = new Double(toks[j]);
			}
			count++;
			line = BasicFileIO.getLine(bReader);
		}
		if (count != multlen) {
			System.out.println("Problem. lang=" + lang);
			System.exit(-1);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		return gold;
	}
	
	private static void plainCombination(String[] args) {
		int argslen = args.length;
		System.out.println("Combining...");
		int multlen = ComputeTransitionMultinomials.COARSE_TAGS.length;
		double[][] res = new double[multlen][multlen];
		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < res.length; j++) {
				res[i][j] = 0.0;
			}
		}
		String outPrefix = "";
		for (int i = 0; i < argslen; i++) {
			System.out.println(args[i]);
			outPrefix += args[i] + "-";
			String file = 
				"/usr2/dipanjan/experiments/SSL/UnsupervisedPOS/data/multinomials/" + 
				args[i] + ".gold.mults";
			BufferedReader bReader = BasicFileIO.openFileToRead(file);
			String line = BasicFileIO.getLine(bReader);
			int count = 0;
			while (line != null) {
				String[] toks = line.trim().split(" ");
				if (toks.length != multlen) {
					System.out.println("Problem. Line:" + line + " lang=" + args[i]);
					System.exit(-1);
				}
				for (int j = 0; j < toks.length; j++) {
					res[count][j] += new Double(toks[j]);
				}
				count++;
				line = BasicFileIO.getLine(bReader);
			}
			if (count != multlen) {
				System.out.println("Problem. lang=" + args[i]);
				System.exit(-1);
			}
			BasicFileIO.closeFileAlreadyRead(bReader);
		}		
		for (int i = 0; i <multlen; i++) {
			double sum = 0.0;
			for (int j = 0; j < multlen; j++) {
				sum += res[i][j];
			}
			for (int j = 0; j < multlen; j++) {
				res[i][j] /= sum;
			}
		}
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(
				"/usr2/dipanjan/experiments/SSL/UnsupervisedPOS/data/multinomials/" + outPrefix + "avg.multinomials");
		for (int i = 0; i < multlen; i++) {
			String line = "";			
			for (int j = 0; j < multlen; j++) {
				line += ""+res[i][j] + " ";
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}
}