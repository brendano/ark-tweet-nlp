package edu.cmu.cs.lti.ark.ssl.pos.eval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.berkeley.nlp.optimize.BipartiteMatchings;
import edu.cmu.cs.lti.ark.ssl.pos.POSUtil;
import edu.cmu.cs.lti.ark.ssl.pos.TabSeparatedFileReader;
import fig.basic.Pair;

public class Measure121Accuracy {
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
		double score = scoreLabelsOneToOne(goldLabels, autoLabels, numGoldLabels, numAutoLabels);
	}


	public static double scoreLabelsOneToOne(int[][] goldLabels, int[][] guessLabels, int numGoldLabels, int numGuessLabels) {
		System.out.println("Numgoldlabels:" + numGoldLabels);
		System.out.println("Numguesslabels:" + numGoldLabels);
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

		// Do greedy mapping
		boolean[] guessMapped = new boolean[numGuessLabels];
		for (int i=0; i<numGuessLabels; ++i) guessMapped[i] = false;
		boolean[] goldMapped = new boolean[numGoldLabels];
		for (int i=0; i<numGoldLabels; ++i) goldMapped[i] = false;
		int guessRemaining = numGuessLabels;
		int goldRemaining = numGoldLabels;

		double greedy = 0.0;
		while (guessRemaining > 0 && goldRemaining > 0) {
			double bestCount = Double.NEGATIVE_INFINITY;
			int bestGuess = 0;
			int bestGold = 0;
			for (int guess=0; guess<numGuessLabels; ++guess) {
				if (!guessMapped[guess]) {
					for (int gold=0; gold<numGoldLabels; ++gold) {
						if (!goldMapped[gold]) {
							if (labelMapCounts[guess][gold] > bestCount) {
								bestCount = labelMapCounts[guess][gold];
								bestGuess = guess;
								bestGold = gold;
							}
						}
					}
				}
			}
			guessMapped[bestGuess] = true;
			goldMapped[bestGold] = true;
			greedy += bestCount;
			guessRemaining--;
			goldRemaining--;
		}

		System.out.println("Greedy: " + greedy / totalInstances);
		System.out.println("Bipartite: " + bipartite / totalInstances);
		return bipartite / totalInstances;
	}
}