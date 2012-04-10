package edu.cmu.cs.lti.ark.ssl.pos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

public class POSModel implements Serializable {

	private static final long serialVersionUID = 130261216869102354L;
	
	private ArrayList<String> indexToPOS;
	private ArrayList<String> indexToWord;
	private ArrayList<String> indexToFeature;
	private Map<String, Integer> posToIndex;
	private Map<String, Integer> wordToIndex;
	private Map<String, Integer> featureToIndex;
	private ArrayList<Integer> featureIndexCounts;
	private double[] weights;
	
	public void setIndexToPOS(ArrayList<String> indexToPOS) {
		this.indexToPOS = indexToPOS;
	}
	public ArrayList<String> getIndexToPOS() {
		return indexToPOS;
	}
	public void setIndexToWord(ArrayList<String> indexToWord) {
		this.indexToWord = indexToWord;
	}
	public ArrayList<String> getIndexToWord() {
		return indexToWord;
	}
	public void setIndexToFeature(ArrayList<String> indexToFeature) {
		this.indexToFeature = indexToFeature;
	}
	public ArrayList<String> getIndexToFeature() {
		return indexToFeature;
	}
	public void setPosToIndex(Map<String, Integer> posToIndex) {
		this.posToIndex = posToIndex;
	}
	public Map<String, Integer> getPosToIndex() {
		return posToIndex;
	}
	public void setWordToIndex(Map<String, Integer> wordToIndex) {
		this.wordToIndex = wordToIndex;
	}
	public Map<String, Integer> getWordToIndex() {
		return wordToIndex;
	}
	public void setFeatureToIndex(Map<String, Integer> featureToIndex) {
		this.featureToIndex = featureToIndex;
	}
	public Map<String, Integer> getFeatureToIndex() {
		return featureToIndex;
	}
	public void setFeatureIndexCounts(ArrayList<Integer> featureIndexCounts) {
		this.featureIndexCounts = featureIndexCounts;
	}
	public ArrayList<Integer> getFeatureIndexCounts() {
		return featureIndexCounts;
	}
	public void setWeights(double[] weights) {
		this.weights = weights;
	}
	public double[] getWeights() {
		return weights;
	}
}