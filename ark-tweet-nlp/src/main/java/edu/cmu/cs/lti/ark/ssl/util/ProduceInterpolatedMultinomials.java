package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import edu.berkeley.nlp.util.ArrayUtil;
import edu.cmu.cs.lti.ark.ssl.pos.POSModel;


public class ProduceInterpolatedMultinomials {
	public static void main(String[] args) {
		String modelFile = args[0];
		String fineToCoarseMapFile = args[1];
		String outputFile = args[2];
		String pathToTransitionFiles=args[3];
		
		Set<String> validTags = AverageMultinomials.getValidTags(fineToCoarseMapFile);
		String[] temp = new String[validTags.size()];
		validTags.toArray(temp);
		Arrays.sort(temp);		
		ArrayList<String> validTagList = new ArrayList<String>();
		for (String str: temp) {
			validTagList.add(str);
		}
		validTagList.add("END");
		validTagList.add("START");
		System.out.println("Size of valid tag list:" + validTagList.size());
		String[] validTagArray = new String[validTagList.size()];
		validTagList.toArray(validTagArray);
		System.out.println("Valid tags:");
		for (String tag: validTagArray) {
			System.out.println(tag);
		}		
		String[] coarseTagArray = ComputeTransitionMultinomials.COARSE_TAGS;
		Arrays.sort(coarseTagArray);
		
		String[] paths = pathToTransitionFiles.split(",");
		System.out.println("Path to supervised multinomials:");
		for (String path: paths) {
			System.out.println(path);
		}
		int numHelperLanguages = paths.length;
		System.out.println("Number of helper languages:" + numHelperLanguages);
		double[][][] transitionMatrices = new double[numHelperLanguages][validTagArray.length][validTagArray.length];
		for (int i = 0; i < numHelperLanguages; i++) {
			double[][] gold = 
				AverageMultinomials.readGoldMultinomials(paths[i], coarseTagArray.length, "language" + i);
			for (int j = 0; j < validTagArray.length; j++) {
				double sum = 0.0;
				Arrays.fill(transitionMatrices[i][j], 0);
				for (int k = 0; k < validTagArray.length; k++) {
					String from = validTagArray[j];
					String to = validTagArray[k];
					int frmIndex = Arrays.binarySearch(coarseTagArray, from);
					int toIndex = Arrays.binarySearch(coarseTagArray, to);
					transitionMatrices[i][j][k] = gold[frmIndex][toIndex];
					sum += transitionMatrices[i][j][k];
				}
				for (int k = 0; k < validTagArray.length; k++) {
					transitionMatrices[i][j][k] /= sum;
				}
			}
		}
		
		POSModel model = (POSModel) BasicFileIO.readSerializedObject(modelFile);
		ArrayList<String> indexToPOS;
		ArrayList<String> indexToFeature;
		Map<String, Integer> posToIndex;
		Map<String, Integer> featureToIndex;
		ArrayList<Integer> featureIndexCounts;
		featureIndexCounts = model.getFeatureIndexCounts();
		featureToIndex = model.getFeatureToIndex();
		indexToFeature = model.getIndexToFeature();
		indexToPOS = model.getIndexToPOS();
		posToIndex = model.getPosToIndex();
		double[] weights = model.getWeights();
		
		double[][] mixtureWeights = new double[numHelperLanguages][validTagArray.length];
		for (String feat: indexToFeature) {
			if (feat.startsWith("iind")) {
				String[] toks = feat.split("\\|");
				int id = new Integer(toks[1]);
				int label = new Integer(toks[2]);
				int featIndex = featureToIndex.get(feat);
				double wt = Math.exp(weights[featIndex]);
				mixtureWeights[id][label] = wt;
			}
		}
		for (int l0 = 0; l0 < validTagArray.length; ++l0) {
			double norm = 0.0;
			for (int h = 0; h < numHelperLanguages; ++h) {
				norm += mixtureWeights[h][l0];
			}
			for (int h = 0; h < numHelperLanguages; ++h) {
				mixtureWeights[h][l0] /= norm;
			}
		}
		System.out.println("Number of tags in valid array:" + validTagArray.length);
		double[][] interpolatedMultinomials = new double[validTagArray.length][validTagArray.length];
		ArrayUtil.fill(interpolatedMultinomials, 0.0);
		for (int l0 = 0; l0 < validTagArray.length; ++l0) {
			double sum = 0.0;
			for (int l1 = 0; l1 < validTagArray.length; ++l1) {
				for (int h = 0; h < numHelperLanguages; ++h) {
					interpolatedMultinomials[l0][l1] += mixtureWeights[h][l0] * transitionMatrices[h][l0][l1];
				}	
				sum += interpolatedMultinomials[l0][l1];
			}
		}
		
		String[] sortedArray = new String[validTagArray.length];
		for (int s = 0; s < validTagArray.length; s++) {
			sortedArray[s] = new String(validTagArray[s]);
		}
		Arrays.sort(sortedArray);
		System.out.println("Sorted array:");
		for (String str: sortedArray) {
			System.out.println(str);
		}
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outputFile);
		for (int l0 = 0; l0 < sortedArray.length; ++l0) {
			String tag1 = sortedArray[l0];
			int tag1Index = linearSearch(validTagArray, tag1);
			String line = "";
			for (int l1 = 0; l1 < sortedArray.length; ++l1) {
				String tag2 = sortedArray[l1];
				int tag2Index = linearSearch(validTagArray, tag2);
				line += interpolatedMultinomials[tag1Index][tag2Index] + " ";
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}
	
	private static int linearSearch(String[] array, String elem) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(elem)) {
				return i;
			} 
		} 
		return -1;
	}
}

