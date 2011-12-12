package edu.cmu.cs.lti.ark.ssl.pos;

import java.util.List;

import pos_tagging.ForwardBackward;

import edu.berkeley.nlp.math.CachingDifferentiableFunction;
import fig.basic.Pair;

public abstract class GradientSequenceModel extends CachingDifferentiableFunction {

	abstract public ForwardBackward getForwardBackward();
	
	abstract public void resetNumCalculates();
	
	abstract public int getNumCalculates();
	
	abstract public void setActiveFeatures(List<Pair<Integer,Double>>[][] activeTransFeatures0, 
			List<Pair<Integer,Double>>[][] activeEmitFeatures0,
			List<Pair<Integer,Double>>[][] activeStackedFeatures0,
			int numFeatures0, double[] regularizationWeights0, double[] regularizationBiases0);
	
	abstract public void setActiveFeatures(Pair<Integer,Double>[][] activeInterpolationFeatures0, 
			List<Pair<Integer,Double>>[][] activeEmitFeatures0,
			List<Pair<Integer,Double>>[][] activeStackedFeatures0,
			int numFeatures0, double[] regularizationWeights0, double[] regularizationBiases0);
	
	abstract public double[][] getTransPotentials();
	
	abstract public double[][] getEmitPotentials();
	
	abstract public double[] getWeights();
	
	abstract public int getNumFeatures();
	
	abstract public int getNumLabels();
	
	abstract public int getNumObservations();
	
	abstract public int getStartLabel();
	
	abstract public int getStopLabel();
	
	abstract public void setWeights(double[] weights0);
	
	abstract public void computePotentials();
	
	abstract public double calculateRegularizedLogMarginalLikelihood();
	
	abstract public double calculateRegularizer();
	
	abstract public Pair<Double, double[]> calculate(double[] x);
	
	abstract public int dimension();
	
	abstract public double[][][] getAllPosteriors();

}
