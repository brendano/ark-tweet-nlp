package edu.cmu.cs.lti.ark.ssl.pos;

import edu.berkeley.nlp.math.DifferentiableFunction;
import edu.berkeley.nlp.math.DoubleArrays;
import fig.basic.Pair;

public class SupervisedGenSequenceModelDiscObjective implements DifferentiableFunction {

	private SupervisedGenSequenceModel numerator;
	private GradientGenSequenceModel denominator ;
	private int numFeatures;
	
	private double lastValue;
	private double[] lastDerivative;
	private double[] lastX;
	
	private double regularizationWeight;
	
	public SupervisedGenSequenceModelDiscObjective(int[][] observations0, 
			int[][] goldLabels0,
			int numLabels0, 
			int numObservations0,
			int numFeatures0,
			double regularizationWeight0,
			boolean storePosteriors) {
		numerator = 
			new SupervisedGenSequenceModel(observations0, 
					goldLabels0,
					numLabels0, 
					numObservations0);
		denominator = 
			new GradientGenSequenceModel(observations0, 
					null,
					numLabels0, 
					numObservations0,
					storePosteriors);
		numFeatures = numFeatures0;
		regularizationWeight = regularizationWeight0;
	}

	public SupervisedGenSequenceModel getNumeratorModel() {
		return numerator;
	}
	
	public GradientGenSequenceModel getDenominatorModel() {
		return denominator;
	}	
	
	public double[] derivativeAt(double[] x) {
		ensureCache(x);
		return lastDerivative;
	}

	public int dimension() {
		return numFeatures;
	}

	public double valueAt(double[] x) {
		ensureCache(x);
		return lastValue;
	}
	
	private void ensureCache(double[] x) {
		if (requiresUpdate(lastX, x)) {
			Pair<Double, double[]> currentValueAndDerivative = calculate(x);
			lastValue = currentValueAndDerivative.getFirst();
			lastDerivative = currentValueAndDerivative.getSecond();
			lastX = DoubleArrays.clone(x);
		}
	}
	
	private boolean requiresUpdate(double[] lastX, double[] x) {
		if (lastX == null) return true;
		for (int i = 0; i < x.length; i++) {
			if (lastX[i] != x[i]) return true;
		}
		return false;
	}
	
	public double calculateRegularizer(double[] weights) {
		double result = 0.0;
		for (int f=0; f<numFeatures; ++f) {
			result += regularizationWeight*(weights[f])*(weights[f]);
		}
		return result;
	}
	
	private Pair<Double, double[]> calculate(double[] x) {
		Pair<Double, double[]> calcNum = numerator.calculate(x);
		Pair<Double, double[]> calcDenom = denominator.calculate(x);
		
		double totalValue = calcNum.getFirst() - calcDenom.getFirst() + calculateRegularizer(x);
		
		int size = calcNum.getSecond().length;
		double[] derivatives = new double[size];
		for (int i = 0; i < derivatives.length; i ++) {
			derivatives[i] = calcNum.getSecond()[i] - calcDenom.getSecond()[i] + 2 * regularizationWeight * x[i];
		}				
		return Pair.makePair(totalValue, derivatives);
	}
}