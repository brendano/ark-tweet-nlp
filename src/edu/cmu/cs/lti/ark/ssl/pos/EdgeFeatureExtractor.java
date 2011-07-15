package edu.cmu.cs.lti.ark.ssl.pos;

import java.io.Serializable;
import java.util.List;

import fig.basic.Pair;

public class EdgeFeatureExtractor implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5646264065971360082L;
	List<Pair<Integer,Double>>[][] activeTransFeatures;
	
	public EdgeFeatureExtractor(List<Pair<Integer,Double>>[][] activeTransFeatures0) {
		activeTransFeatures = activeTransFeatures0;
	}	
	
	public List<Pair<Integer, Double>> extractFeatures(int label1, int label2) {
		return activeTransFeatures[label1][label2];
	}
}