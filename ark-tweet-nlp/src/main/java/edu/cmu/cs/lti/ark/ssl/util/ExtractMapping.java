package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import edu.berkeley.nlp.optimize.BipartiteMatchings;
import edu.cmu.cs.lti.ark.ssl.pos.POSUtil;
import edu.cmu.cs.lti.ark.ssl.pos.TabSeparatedFileReader;
import fig.basic.Pair;


public class ExtractMapping {
	public static void main(String[] args) {
		String goldFile = "/home/dipanjan/work/spring2011/UnsupPOSTagging/samples/SLOVENE-test.tb.pos";
		String autoFile = "/home/dipanjan/work/spring2011/UnsupPOSTagging/samples/SLOVENE-int.output";
		Collection<Pair<List<String>, List<String>>>  gSequences =
			TabSeparatedFileReader.readPOSSeqences(goldFile, 
					Integer.MAX_VALUE, 
					Integer.MAX_VALUE);

		Collection<Pair<List<String>, List<String>>>  aSequences =
			TabSeparatedFileReader.readPOSSeqences(autoFile, 
					Integer.MAX_VALUE, 
					Integer.MAX_VALUE);
		ArrayList<String> indexToPOSGold = new ArrayList<String>();
		ArrayList<String> indexToPOSAuto = new ArrayList<String>();
		Map<String, Integer> posToIndexGold = new HashMap<String, Integer>();
		Map<String, Integer> posToIndexAuto = new HashMap<String, Integer>();

		int gSize = gSequences.size();
		int[][] goldLabels = new int[gSize][];
		int count = 0;
		for (Pair<List<String>, List<String>> gSeq: gSequences) {
			List<String> gPOS = gSeq.getSecond();
			int posSize = gPOS.size();
			goldLabels[count] = new int[posSize];
			int posCount = 0;
			for (String pos: gPOS) {
				int wordIndex = POSUtil.indexString(pos, indexToPOSGold, posToIndexGold);
				goldLabels[count][posCount] = wordIndex;
				posCount++;
			}
			count++;
		}

		int aSize = aSequences.size();
		int[][] autoLabels = new int[aSize][];
		count = 0;
		for (Pair<List<String>, List<String>> aSeq: aSequences) {
			List<String> aPOS = aSeq.getSecond();
			int posSize = aPOS.size();
			autoLabels[count] = new int[posSize];
			int posCount = 0;
			for (String pos: aPOS) {
				int wordIndex = POSUtil.indexString(pos, indexToPOSAuto, posToIndexAuto);
				autoLabels[count][posCount] = wordIndex;
				posCount++;
			}
			count++;
		}

		int numGoldLabels = indexToPOSGold.size();
		int numAutoLabels = indexToPOSAuto.size();
		int[] matching = scoreLabelsOneToOne(goldLabels, autoLabels, numGoldLabels, numAutoLabels);		
		System.out.println("Mapping from HMM states to coarse tags:");
		System.out.println("Matching length:" + matching.length);
		int autoLen = indexToPOSAuto.size();
		for (int i = 0; i < autoLen; i++) {
			String tag = "T"+i;
			int index = posToIndexAuto.get(tag);
			if (matching[index] != -1) {
				System.out.println(tag + "\t" + indexToPOSGold.get(matching[index]));
			}
		}
		printInitialMapping();
	}
	
	public static void printInitialMapping() {
		System.out.println("\n\nInitial Mapping..");
		String mapFile = "/home/dipanjan/work/spring2011/UnsupPOSTagging/data/maps/sl-fine-universal.map";
		Set<String> validTags = AverageMultinomials.getValidTags(mapFile);		
		validTags.add("START");
		validTags.add("END");
		String[] validTagArray = new String[validTags.size()];
		validTags.toArray(validTagArray);
		Arrays.sort(validTagArray);
		Map<String, String> map = ComputeInitialTransitionFeatures.getMap(mapFile);
		Map<String, String> fineToCoarseMap = new HashMap<String, String>();
		Map<Integer, Integer> coarseNumMap = new HashMap<Integer, Integer>();
		
		Set<String> keys = map.keySet();
		int keySize = keys.size();
		String[] keyArray = new String[keySize];
		keys.toArray(keyArray);
		Arrays.sort(keyArray);
		int count = 0;
		for (String key: keyArray) {
			String coarseTag = map.get(key);
			int cIndex = Arrays.binarySearch(validTagArray, coarseTag);
			String fineTag = "T"+count;
			fineToCoarseMap.put(fineTag, coarseTag);
			if (coarseNumMap.containsKey(cIndex)) {
				int num = coarseNumMap.get(cIndex);
				coarseNumMap.put(cIndex, num + 1);
			} else {
				coarseNumMap.put(cIndex, 1);
			}
			count++;
		}
		int numFineTags = map.size();
		int cIndex = Arrays.binarySearch(validTagArray, "START");
		coarseNumMap.put(cIndex, 1);
		int numTag = numFineTags + 1;
		String fineTag = "T"+numTag;
		fineToCoarseMap.put(fineTag, "START");
		
		cIndex = Arrays.binarySearch(validTagArray, "END");
		coarseNumMap.put(cIndex, 1);
		numTag = numFineTags;
		fineTag = "T"+numTag;
		fineToCoarseMap.put(fineTag, "END");
		
		keys = fineToCoarseMap.keySet();
		keySize = keys.size();
		keyArray = new String[keySize];
		keys.toArray(keyArray);
		Arrays.sort(keyArray);
		for (int i = 0; i < keyArray.length - 2; i++) {
			System.out.println("T"+i + "\t" + fineToCoarseMap.get("T"+i));
		}
		
//		double[][] res = new double[numFineTags + 2][numFineTags + 2];
//		String outFile = avgMultFile + ".expanded";
//		String outFile2 = outFile + ".explicit";
//		Random rand = rands[0];
//		double range = 0.0002;
//		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
//		BufferedWriter bWriter2 = BasicFileIO.openFileToWrite(outFile2);
//		for (int i = 0; i < numFineTags + 2; i++) {
//			String line = "";
//			double sum = 0.0;
//			for (int j = 0; j < numFineTags + 2; j++) {
//				String firstC =  fineToCoarseMap.get(keyArray[i]);
//				String secondC =  fineToCoarseMap.get(keyArray[j]);	
//				int firstIndex = 
//					Arrays.binarySearch(validTagArray, firstC);
//				int secondIndex = 
//					Arrays.binarySearch(validTagArray, secondC);
//				double val = coarseMults[firstIndex][secondIndex];
//				int numMaps = coarseNumMap.get(secondIndex);
//				val = val / (double)numMaps;
//				// double randVal = rand.nextDouble();
//				// val = val + (range*randVal);
//				res[i][j] = val;
//				sum += res[i][j];
//			}			
//			for (int j = 0; j < numFineTags + 2; j++) {
//				res[i][j] = res[i][j] / sum;
//				String line2 = keyArray[i] + " " + keyArray[j] + " " + res[i][j];
//				BasicFileIO.writeLine(bWriter2, line2);
//				line += res[i][j] + " ";
//			}
//			line = line.trim();
//			BasicFileIO.writeLine(bWriter, line);
//		}
	}	
	
	public static int[] scoreLabelsOneToOne(int[][] goldLabels, int[][] guessLabels, int numGoldLabels, int numGuessLabels) {
		System.out.println("Numgoldlabels:" + numGoldLabels);
		System.out.println("Numguesslabels:" + numGuessLabels);
		double totalInstances = 0.0;
		double[][] labelMapCounts = new double[numGuessLabels][numGoldLabels];
		for (int s=0; s<goldLabels.length; ++s) {
			for (int i=0; i<goldLabels[s].length; ++i) {
				labelMapCounts[guessLabels[s][i]][goldLabels[s][i]]++;
				totalInstances++;
			}
		}

		// Do bipartite matching
		BipartiteMatchings matcher = new BipartiteMatchings();
		double[][] negLabelMapCounts = new double[numGuessLabels][numGoldLabels];
		for (int s0=0; s0< numGuessLabels; ++s0) {
			for (int s1=0; s1<numGoldLabels; ++s1) {
				negLabelMapCounts[s0][s1] = -labelMapCounts[s0][s1];
			}
		}
		int[] matching = matcher.getMaxMatching(negLabelMapCounts);
		double bipartite = matcher.getMatchingCost(labelMapCounts, matching);
		System.out.println("Score:" + bipartite / totalInstances);
		return matching;
	}
}