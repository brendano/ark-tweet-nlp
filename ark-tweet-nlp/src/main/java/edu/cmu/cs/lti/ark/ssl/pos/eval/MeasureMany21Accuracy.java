package edu.cmu.cs.lti.ark.ssl.pos.eval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.lti.ark.ssl.pos.POSUtil;
import edu.cmu.cs.lti.ark.ssl.pos.TabSeparatedFileReader;
import fig.basic.Pair;

public class MeasureMany21Accuracy {
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
		double score = scoreLabelsManyToOne(goldLabels, autoLabels, numGoldLabels, numAutoLabels);
		System.out.println("Many to one accuracy: " + score);
	}
	
	
	

	public static double scoreLabelsManyToOne(int[][] goldLabels, 
			int[][] guessLabels, 
			int numGoldLabels, 
			int numGuessLabels) {
		double totalInstances = 0.0;
		double[][] labelMapCounts = new double[numGuessLabels][numGoldLabels];
		for (int s=0; s<goldLabels.length; ++s) {
			for (int i=0; i<goldLabels[s].length; ++i) {
				labelMapCounts[guessLabels[s][i]][goldLabels[s][i]]++;
				totalInstances++;
			}
		}
		// Do mapping
		double greedyCorrectInstances = 0.0;
		for (int l0=0; l0<numGuessLabels; ++l0) {
			double bestScore = Double.NEGATIVE_INFINITY;
			for (int l1=0; l1<numGoldLabels; ++l1) {
				bestScore = Math.max(bestScore, labelMapCounts[l0][l1]);
			}
			greedyCorrectInstances += bestScore;
		}
		return greedyCorrectInstances/totalInstances;
	}	
}