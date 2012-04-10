package edu.cmu.cs.lti.ark.ssl.pos.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.berkeley.nlp.util.ArrayUtil;
import edu.cmu.cs.lti.ark.ssl.pos.POSUtil;
import edu.cmu.cs.lti.ark.ssl.pos.TabSeparatedFileReader;
import fig.basic.Pair;

public class VMeasure {
	public static void main(String[] args) {
		String goldFile = args[0];
		String autoFile = args[1];
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
		double score = scoreVMeasure(goldLabels, autoLabels, numGoldLabels, numAutoLabels);
	}
	
	private static double computeEntropy(int[][] labels, int numLabels) {
		double[] probs = new double[numLabels];
		Arrays.fill(probs, 0);
		double total = 0.0;
		for (int i = 0; i < labels.length; i++) {
			for (int j = 0; j < labels[i].length; j++) {
				probs[labels[i][j]]++;
				total ++;
			}
		}
		double entropy = 0.0;
		for (int i = 0; i < numLabels; i++) {
			probs[i] /= total;
			if (probs[i] != 0) {
				entropy -= probs[i] * Math.log(probs[i]);
			}
		}
		return entropy;
	}

	private static double getJointEntropy(int[][] labels1, int[][] labels2, int numLabels1, int numLabels2) {
		
		double[][] probs = new double[numLabels1][numLabels2];
		double total = 0.0;
		ArrayUtil.fill(probs, 0.0);
		for (int i = 0; i < labels1.length; i++) {
			for (int j = 0; j < labels1[i].length; j++) {
				int lab1 = labels1[i][j];
				int lab2 = labels2[i][j];
				probs[lab1][lab2]++;
				total++;
			}
		}	
		double entropy = 0.0;
		for (int i = 0; i < numLabels1; i++) {
			for (int j = 0; j < numLabels2; j++) {
				probs[i][j] /= total;
				if (probs[i][j] != 0) {
					entropy -= probs[i][j] * Math.log(probs[i][j]);
				}
			}
		}
		return entropy;		
	}
	
	private static double scoreVMeasure(int[][] goldLabels, int[][] autoLabels,
										int numGoldLabels, int numAutoLabels) {
		double entropyT = computeEntropy(goldLabels, numGoldLabels);		
		double entropyC = computeEntropy(autoLabels, numAutoLabels);
				
		double jointEntropyCT = getJointEntropy(autoLabels, goldLabels, numAutoLabels, numGoldLabels);
		
		double condTGivenC = jointEntropyCT - entropyC;
		double condCGivenT = jointEntropyCT - entropyT;
		
		double h = 1 - condTGivenC/entropyT;
		double c = 1 - condCGivenT/entropyC;
		
		double vm = 2*h*c / (c + h);
		
		System.out.println("V-Measure: " + vm);
		
		return 0;
	}
}