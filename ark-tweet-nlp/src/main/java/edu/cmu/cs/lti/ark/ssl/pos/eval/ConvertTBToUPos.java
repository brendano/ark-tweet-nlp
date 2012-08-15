package edu.cmu.cs.lti.ark.ssl.pos.eval;

import edu.cmu.cs.lti.ark.ssl.pos.TabSeparatedFileReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import cmu.arktweetnlp.util.BasicFileIO;

import fig.basic.Pair;

public class ConvertTBToUPos {
	
	public static void main(String[] args) {
		String[] languages = {"DUTCH", "GERMAN", "ITALIAN", "PORTUGUESE", "SPANISH", "SWEDISH"};
		String directory = "/usr2/dipanjan/experiments/SSL/data/conllcorpora";
		String outdirectory = "/usr2/dipanjan/experiments/UGI/code_multilingual";
		String autodir = "/usr2/dipanjan/experiments/SSL/data/runOutputs";
		double[] thresholds = {0.2, 0.3, 0.2, 0.1, 0.2, 0.2};
		
		for (int i = 0; i < languages.length; i ++) {
			String treebank = directory + "/" + languages[i] + "-train.conll";
			String goldPOSSet = directory + "/" + languages[i] + "-train.tb.upos";
			String testTreebank = directory + "/" + languages[i] + "-test.conll";
			String testGoldPOSSet = directory + "/" + languages[i] + "-test.tb.upos";
						
			String autoPOSSet = autodir + "/" + languages[i] + "-train-results-tag-dict-lg-fi-"+thresholds[i]+"-preset"; 
			String testAutoPOSSet = autodir + "/" + languages[i] + "-test-results-tag-dict-lg-fi-"+thresholds[i]+"-preset";
			
			
			String gwordsfile = outdirectory + "/words_gold_" + languages[i];
			String gposfile = outdirectory + "/poses_gold_" + languages[i];
			String gdepfile = outdirectory + "/deps_gold_" + languages[i];
			
			String awordsfile = outdirectory + "/words_auto_" + languages[i];
			String aposfile = outdirectory + "/poses_auto_" + languages[i];
			String adepfile = outdirectory + "/deps_auto_" + languages[i];
						
			
			Collection<Pair<List<String>, List<String>>> goldCol = 
				TabSeparatedFileReader.readPOSSeqences(goldPOSSet, 400000, 10);
			Collection<Pair<List<String>, List<String>>> testGoldCol = 
				TabSeparatedFileReader.readPOSSeqences(testGoldPOSSet, 400000, 10);
			goldCol.addAll(testGoldCol);			
			
			Collection<Pair<List<String>, List<String>>> autoCol = 
				TabSeparatedFileReader.readPOSSeqences(autoPOSSet, 400000, 10);
			Collection<Pair<List<String>, List<String>>> testAutoCol = 
				TabSeparatedFileReader.readPOSSeqences(testAutoPOSSet, 400000, 10);
			autoCol.addAll(testAutoCol);		
			
			Collection<List<String>> deps = readDeps(treebank, testTreebank, 400000, 10);
			if (goldCol.size() != autoCol.size()) {
				System.out.println("Problem with language:" + languages[i] + ". Size of gold and auto collections different.");
				System.exit(-1);
			}
			if (goldCol.size() != deps.size()) {
				System.out.println("Problem with language:" + languages[i] + ". Size of gold and treebanks different.");
				System.exit(-1);
			}
			Iterator<Pair<List<String>, List<String>>> git = 
				goldCol.iterator();
			Iterator<Pair<List<String>, List<String>>> ait = 
				autoCol.iterator();
			Iterator<List<String>> dit = deps.iterator();
			int j = 0;
			ArrayList<String> words = new ArrayList<String>();
			ArrayList<String> gposes = new ArrayList<String>();
			ArrayList<String> aposes = new ArrayList<String>();
			ArrayList<String> depss = new ArrayList<String>();
			
			while (git.hasNext()) {
				Pair<List<String>, List<String>> gpair = git.next();
				Pair<List<String>, List<String>> apair = ait.next();
				List<String> depList = dit.next();
				if (gpair.getFirst().size() != apair.getFirst().size()) {
					System.out.println("Problem with language:" + languages[i] + ". Sentence number:" + j);
					System.out.println(gpair.getFirst());
					System.out.println(apair.getFirst());
					System.exit(-1);
				}
				if (gpair.getFirst().size() != depList.size()) {
					System.out.println("Problem with language:" + languages[i] + ". Sentence number:" + j);
					System.out.println(gpair.getFirst());
					System.out.println(depList);
					System.exit(-1);
				}
				int len = depList.size();
				List<String> gwords = new ArrayList<String>();
				List<String> gpos = new ArrayList<String>();
				List<String> apos = new ArrayList<String>();
				List<Integer> newdeps = new ArrayList<Integer>();
				int offset = 0;
				Map<Integer, Integer> omap = new HashMap<Integer, Integer>();
				boolean foundPunctuationParent = false;
				for (int k = 0; k < len; k++) {
					String gp = gpair.getSecond().get(k);
					if (gp.equals(".")) {
						offset++;
					} else {
						gwords.add(gpair.getFirst().get(k));
						gpos.add(gpair.getSecond().get(k));
						apos.add(apair.getSecond().get(k));
						int par = new Integer(depList.get(k));
						par = par - 1;
						if (par != -1) {
							if (gpair.getSecond().get(par).equals(".")) {
								System.out.println("Parent in sent:"+j+" for language:"+languages[i] +" is a punctuation. Continuing.");
								foundPunctuationParent = true;
								break;
							}
						}
						newdeps.add(par);
					}
					omap.put(k, offset);
				}		
				if (foundPunctuationParent) {
					j++;
					continue;
				}				
				String sent = "";
				String gposSent = "";
				String aposSent = "";
				String depSent = "";
				len = gwords.size();
				int root = -1;
				for (int k = 0; k < len; k++) {
					sent += gwords.get(k) + " ";
					gposSent += gpos.get(k) + " ";
					aposSent += apos.get(k) + " ";
					int par = newdeps.get(k);
					if (par == -1) {
						root = k;
						depSent += len + "-" + k + " ";
						continue;
					}
					int off = omap.get(par);
					par = par - off;				
					depSent += par+"-"+k+" ";
				}		
				sent += "#";
				gposSent += "#";
				aposSent += "#";
				if (root == -1) {
					System.out.println("Root not found for sent:"+j+" for language:"+languages[i]);
					j++;
					continue;
				}
				depSent = depSent.trim();
				words.add(sent);
				gposes.add(gposSent);
				aposes.add(aposSent);
				depss.add(depSent);
				j++;
			}
			writeSentencesToFile(gwordsfile, words);
			writeSentencesToFile(gposfile, gposes);
			writeSentencesToFile(gdepfile, depss);
			
			ArrayList<String> awords = new ArrayList<String>();
			ArrayList<String> aposes1 = new ArrayList<String>();
			ArrayList<String> adepss = new ArrayList<String>();
			for (int k = 0; k < words.size(); k ++) {
				String postags = aposes.get(k).trim();
				String[] toks = postags.split(" ");
				Arrays.sort(toks);
				if (Arrays.binarySearch(toks, ".") >= 0) {
					continue;
				} else {
					awords.add(words.get(k));
					aposes1.add(aposes.get(k));
					adepss.add(depss.get(k));
				}
			}			
			writeSentencesToFile(awordsfile, awords);
			writeSentencesToFile(aposfile, aposes1);
			writeSentencesToFile(adepfile, adepss);
		}	
	}	
	
	public static void writeSentencesToFile(String file, ArrayList<String> sentences) {
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(file);
		for (int i = 0; i < sentences.size(); i ++) {
			BasicFileIO.writeLine(bWriter, sentences.get(i));
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}
	
	public static Collection<List<String>> 
		readDeps(String path, String testPath, int numSequences, int maxSequenceLength) {
		Collection<List<String>> sequences = 
			new ArrayList<List<String>>();
		BufferedReader reader = BasicFileIO.openFileToRead(path);
		int countLines = 0;
		while (true) {
			List<String> sequence = getSequenceInfo(reader);
			if (sequence == null) {
				break;
			}
			if (sequence.size() > maxSequenceLength) {
				continue;
			}
			countLines++;
			if (countLines > numSequences) {
				break;
			}
			sequences.add(sequence);
		}
		BasicFileIO.closeFileAlreadyRead(reader);
		reader = BasicFileIO.openFileToRead(testPath);
		while (true) {
			List<String> sequence = getSequenceInfo(reader);
			if (sequence == null) {
				break;
			}
			if (sequence.size() > maxSequenceLength) {
				continue;
			}
			countLines++;
			if (countLines > numSequences) {
				break;
			}
			sequences.add(sequence);
		}
		return sequences;
	}

	public static List<String> 
	getSequenceInfo(BufferedReader reader) {
		List<String> deps = new ArrayList<String>();	
		String line = BasicFileIO.getLine(reader);
		if (line == null) {
			return null;
		}
		line = line.trim();
		while (!line.equals("") && line != null) {
			String[] toks = getToks(line);
			deps.add(toks[6]);
			line = BasicFileIO.getLine(reader);
			if (line != null) {
				line = line.trim();
			}
		}
		return deps;
	}
	
	public static String[] getToks(String line) {
		List<String> tokList = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(line.trim(), " \t", true);
		while (st.hasMoreTokens()) {
			String tok = st.nextToken().trim();
			if (tok.equals("")) {
				continue;
			}
			tokList.add(tok);
		}
		String[] arr = new String[tokList.size()];
		tokList.toArray(arr);
		return arr;
	}
}