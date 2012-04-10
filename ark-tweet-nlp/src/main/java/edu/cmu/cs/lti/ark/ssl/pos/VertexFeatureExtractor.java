package edu.cmu.cs.lti.ark.ssl.pos;

import java.io.Serializable;
import java.util.List;

import fig.basic.Pair;

public class VertexFeatureExtractor implements Serializable {
	
	private static final long serialVersionUID = 7379390876691220908L;
	List<Pair<Integer,Double>>[][] activeEmitFeatures;
	
	public VertexFeatureExtractor(List<Pair<Integer,Double>>[][] activeEmitFeatures0) {
		activeEmitFeatures = activeEmitFeatures0;
	}	
	
	public List<Pair<Integer, Double>> extractFeatures(int label, int observation) {
		return activeEmitFeatures[label][observation];
	}
}