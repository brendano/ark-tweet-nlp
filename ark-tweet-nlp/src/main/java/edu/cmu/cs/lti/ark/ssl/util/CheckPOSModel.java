package edu.cmu.cs.lti.ark.ssl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cmu.arktweetnlp.util.BasicFileIO;

import edu.cmu.cs.lti.ark.ssl.pos.POSModel;

public class CheckPOSModel {
	public static final String[] languages = {"ENGLISH", "CZECH", "ITALIAN", "GERMAN"};
	public static final String[] tags = {"ADJ", "ADP", "ADV", "CONJ", "DET", "NOUN", "NUM", "PRON", "PRT", "PUNC", "VERB", "X"};
	public static final String[] targets = {"Bulgarian", "Danish", "Dutch", "Greek", "Japanese", "Portuguese", "Slovene", "Spanish", "Swedish", "Turkish"};
	public static void main(String[] args) {
		ArrayList<String> lines = new ArrayList<String>();
		for (String target: targets) {
			System.out.println("Language: " + target);
			String modelFile = "/home/dipanjan/work/spring2011/UnsupPOSTagging/data/forgi/models/"+target.toUpperCase()+"-fhmm-reg.en.de.cs.it.int.tag.dict.200000.1.0.model";
			System.out.println("Modelfile:" + modelFile);
			POSModel model = (POSModel) BasicFileIO.readSerializedObject(modelFile);
			ArrayList<String> indexToPOS;
			ArrayList<String> indexToWord;
			ArrayList<String> indexToFeature;
			Map<String, Integer> posToIndex;
			Map<String, Integer> wordToIndex;
			Map<String, Integer> featureToIndex;
			ArrayList<Integer> featureIndexCounts;
			featureIndexCounts = model.getFeatureIndexCounts();
			featureToIndex = model.getFeatureToIndex();
			indexToFeature = model.getIndexToFeature();
			indexToPOS = model.getIndexToPOS();
			posToIndex = model.getPosToIndex();
			double[] weights = model.getWeights();
			
			ArrayList<String> feats = new ArrayList<String>();
			ArrayList<Double> wts = new ArrayList<Double>();
			Set<Integer> uniqueIds = new HashSet<Integer>();
			Set<Integer> uniqueLabels = new HashSet<Integer>();
			for (String feat: indexToFeature) {
				if (feat.startsWith("iind")) {
					String[] toks = feat.split("\\|");
					int id = new Integer(toks[1]);
					int label = new Integer(toks[2]);
					uniqueIds.add(id);
					uniqueLabels.add(label);
				}
			}
			System.out.println("Size of unique ids:" + uniqueIds.size());
			System.out.println("Size of unique labels:" + uniqueLabels.size());
			
			Integer[] idArray = new Integer[uniqueIds.size()];
			uniqueIds.toArray(idArray);
			Integer[] labArray = new Integer[uniqueLabels.size()];
			uniqueLabels.toArray(labArray);
			Arrays.sort(labArray);
			String outLine = "";
			for (int l = 0; l < tags.length; ++l) {
				double sum = 0.0;
				System.out.println("\n\nLabel:"+tags[l]);
				if (!posToIndex.containsKey(tags[l])) {
					for (int h = 0; h < idArray.length; ++h) {
						System.out.println("Weight for helper language: " + languages[idArray[h]] + " = 0.25");
						outLine += "0.25\t";
					}
				} else {
					int index = posToIndex.get(tags[l]);
					for (int h = 0; h < idArray.length; ++h) {
						String feat = "iind|"+idArray[h] + "|"+index;
						if (!featureToIndex.containsKey(feat)) {
							System.out.println("Problem with:" + feat + " Not present.");
							System.exit(-1);
						}
						int fIndex = featureToIndex.get(feat);
						double wt = Math.exp(weights[fIndex]);
						sum += wt;
					}
					for (int h = 0; h < idArray.length; ++h) {
						String feat = "iind|"+idArray[h] + "|"+index;
						int fIndex = featureToIndex.get(feat);
						double wt = Math.exp(weights[fIndex]);
						wt /= sum;
						System.out.println("Weight for helper language: " + languages[idArray[h]] + " = " + wt);
						outLine += wt +"\t";
					}
				}
			}
			outLine = outLine.trim();
			// System.out.println(outLine);
			lines.add(outLine);
		}
		for (String outLine: lines) {
			System.out.println(outLine);
		}
	}
}