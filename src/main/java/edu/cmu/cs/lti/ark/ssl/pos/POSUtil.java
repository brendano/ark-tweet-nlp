package edu.cmu.cs.lti.ark.ssl.pos;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;
import fig.basic.Pair;

/**
 * Various utilities for POS tagging
 * @author dipanjand
 */
public class POSUtil {

	private static Logger log = Logger.getLogger(POSUtil.class.getCanonicalName());
	
	/**
	 * Gets the observations and the gold labels
	 * @param sequences
	 * @param indexToWord
	 * @param wordToIndex
	 * @param indexToPOS
	 * @param posToIndex
	 * @return
	 */
	public static Pair<int[][], int[][]> getObservationsAndGoldLabels(
			Collection<Pair<List<String>, List<String>>> sequences,
			ArrayList<String> indexToWord,
			Map<String, Integer> wordToIndex,
			ArrayList<String> indexToPOS,
			Map<String, Integer> posToIndex
			) {
		int[][] observations = new int[sequences.size()][];
		int[][] goldLabels = new int[sequences.size()][];
		int i=0;
		for (Pair<List<String>, List<String>> sequence : sequences) {
			observations[i] = new int[sequence.getFirst().size()];
			for (int j=0; j<sequence.getFirst().size(); ++j) {
				observations[i][j] = indexString(sequence.getFirst().get(j), indexToWord, wordToIndex);
			}
			goldLabels[i] = new int[sequence.getSecond().size()];
			for (int j=0; j<sequence.getSecond().size(); ++j) {
				goldLabels[i][j] = indexString(sequence.getSecond().get(j), indexToPOS, posToIndex);
			}
			++i;
		}
		return Pair.makePair(observations, goldLabels);
	}
	
	public static int[][] getObservationsFromUnlabeledSet(
			Collection<List<String>> sequences,
			ArrayList<String> indexToWord,
			Map<String, Integer> wordToIndex
			) {
		int[][] observations = new int[sequences.size()][];
		int i=0;
		for (List<String> sequence : sequences) {
			observations[i] = new int[sequence.size()];
			for (int j=0; j < sequence.size(); ++j) {
				observations[i][j] = indexString(sequence.get(j), indexToWord, wordToIndex);
			}
			++i;
		}
		return observations;
	}
	
	public static int indexString(String name, 
								 ArrayList<String> indexToName, 
								 Map<String, Integer> nameToIndex) {
		Integer index = nameToIndex.get(name);
		if (index == null) {
			index = indexToName.size();
			nameToIndex.put(name, index);
			indexToName.add(name);
		}
		return index;
	}
	
}