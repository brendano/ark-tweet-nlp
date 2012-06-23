package edu.cmu.cs.lti.ark.ssl.pos;

import java.util.Arrays;
import java.util.List;

import pos_tagging.ForwardBackward;

import edu.berkeley.nlp.util.ArrayUtil;
import edu.cmu.cs.lti.ark.ssl.util.SolveEquation;
import fig.basic.LogInfo;
import fig.basic.Pair;

public class GradientSequenceInterpolatedModel extends GradientSequenceModel {

	private int numCalculates;
	private int numLabels;
	private int numObservations;
	private int[][] observations;
	private int startLabel;
	private int stopLabel;
	private double[][] transProbs;
	private double[][] emitProbs;
	private double[] weights;
	private double[] regularizationWeights;
	private double[] regularizationBiases;
	private List<Pair<Integer,Double>>[][] activeEmitFeatures;
	private Pair<Integer,Double>[][] activeInterpolationFeatures;
	private int numFeatures;
	public ForwardBackwardGen forwardBackward;
	private String[] validTagArray;
	private double[][][] transitionMatrices;
	private int numHelperLanguages;
	private double[][] convexCoefficients;
	private double[][][] coefficientProductWithTransProbs;

	public static final String START_TAG = "START";
	public static final String END_TAG = "END";

	public GradientSequenceInterpolatedModel(int[][] observations0,
			int numLabels0, 
			int numObservations0,
			double[][][] tms,
			boolean storePosteriors) {
		this.regularizationBiases = new double[numFeatures];
		this.regularizationWeights = new double[numFeatures];
		this.numCalculates = 0;
		this.numLabels = numLabels0+2;
		this.numObservations = numObservations0;
		this.observations = observations0;
		this.transProbs = new double[numLabels][numLabels];
		this.emitProbs = new double[numLabels][numObservations];
		this.weights = new double[numFeatures];
		this.transitionMatrices = tms;
		this.numHelperLanguages = tms.length;
		this.stopLabel = numLabels-2;
		this.startLabel = numLabels-1;
		this.convexCoefficients = 
			new double[numHelperLanguages][numLabels];
		this.coefficientProductWithTransProbs = new double[numHelperLanguages][numLabels][numLabels];
		this.forwardBackward = 
			new ForwardBackwardGen(observations,
					null,
					numLabels0, 
					numObservations, 
					transProbs, 
					emitProbs,
					null,
					storePosteriors);
	}

	public ForwardBackward getForwardBackward() {
		return forwardBackward;
	}

	public void resetNumCalculates() {
		this.numCalculates = 0;
	}

	public int getNumCalculates() {
		return numCalculates;
	}

	public void setActiveFeatures(
			List<Pair<Integer,Double>>[][] activeTransFeatures0,
			List<Pair<Integer,Double>>[][] activeEmitFeatures0,
			List<Pair<Integer,Double>>[][] stackedFeatures0,
			int numFeatures0, 
			double[] regularizationWeights0, 
			double[] regularizationBiases0) {
	}

	public void setActiveFeatures(
			Pair<Integer,Double>[][] activeInterpolationFeatures0,
			List<Pair<Integer,Double>>[][] activeEmitFeatures0,
			List<Pair<Integer,Double>>[][] stackedFeatures0,
			int numFeatures0, 
			double[] regularizationWeights0, 
			double[] regularizationBiases0) {
		this.regularizationWeights = regularizationWeights0;
		this.regularizationBiases = regularizationBiases0;
		this.numFeatures = numFeatures0;
		this.activeEmitFeatures = activeEmitFeatures0;
		this.activeInterpolationFeatures = activeInterpolationFeatures0;
	}

	public double[][] getTransPotentials() {
		return transProbs;
	}

	public double[][] getEmitPotentials() {
		return emitProbs;
	}

	public double[] getWeights() {
		return weights;
	}

	public int getNumFeatures() {
		return numFeatures;
	}

	public int getNumLabels() {
		return numLabels;
	}

	public int getNumObservations() {
		return numObservations;
	}

	public int getStartLabel() {
		return startLabel;
	}

	public int getStopLabel() {
		return stopLabel;
	}

	public void setWeights(double[] weights0) {
		this.weights = weights0;
	}

	public void computePotentials() {
		// Trans probs
		ArrayUtil.fill(convexCoefficients, 0);
		
		for (int l0 = 0; l0 < numLabels; ++l0) {
			double norm = 0.0;
			for (int h = 0; h < numHelperLanguages; h++) {
				convexCoefficients[h][l0] = 
					Math.exp(computeScore(weights, activeInterpolationFeatures[h][l0]));
				norm += convexCoefficients[h][l0];
			}
			for (int h = 0; h < numHelperLanguages; h++) {
				convexCoefficients[h][l0] /= norm; 
			}
		}
		
		for (int l0 = 0; l0 < numLabels; ++l0) {
			for (int l1 = 0; l1 < numLabels; ++l1) {
				double norm = 0.0;
				for (int h = 0; h < numHelperLanguages; h++) {
					coefficientProductWithTransProbs[h][l0][l1] = 
						Math.exp(computeScore(weights, activeInterpolationFeatures[h][l0])) * transitionMatrices[h][l0][l1];
					norm += coefficientProductWithTransProbs[h][l0][l1];
				}
				for (int h = 0; h < numHelperLanguages; h++) {
					coefficientProductWithTransProbs[h][l0][l1] /= norm; 
				}
			}
		}		
		
		// Trans probs will be automatically normalized
		ArrayUtil.fill(transProbs, 0);
		for (int l0=0; l0<numLabels; ++l0) {
			if (l0 != stopLabel) {
				for (int l1=0; l1<numLabels; ++l1) {
					if (l1 != startLabel) {
						for (int h = 0; h < numHelperLanguages; h++) {
							transProbs[l0][l1] += 
								convexCoefficients[h][l0] * transitionMatrices[h][l0][l1]; 
						}
					}
				}
			}
		}

		// Emit probs
		for (int l=0; l<numLabels; ++l) {
			if (l != startLabel && l != stopLabel) {
				double norm = 0.0;
				for (int i=0; i<numObservations; ++i) {
					emitProbs[l][i] = Math.exp(computeScore(weights, activeEmitFeatures[l][i]));
					norm += emitProbs[l][i];
				}
				for (int i=0; i<numObservations; ++i) {
					emitProbs[l][i] /= norm;
				}
			}
		}
	}

	private static double computeScore(double[] weights, List<Pair<Integer,Double>> activeFeatures) {
		double score = 0.0;
		for (int i=0; i<activeFeatures.size(); ++i) {
			Pair<Integer,Double> feat = activeFeatures.get(i);
			if (feat.getSecond() == Double.NEGATIVE_INFINITY) {
				score = Double.NEGATIVE_INFINITY;
			} else {
				score += weights[feat.getFirst()] * feat.getSecond();
			}
		}
		return score;
	}

	private static double computeScore(double[] weights, Pair<Integer,Double> activeFeature) {
		Pair<Integer,Double> feat = activeFeature;
		double score = 0.0;
		if (feat.getSecond() == Double.NEGATIVE_INFINITY) {
			score = Double.NEGATIVE_INFINITY;
		} else {
			score = weights[feat.getFirst()] * feat.getSecond();
		}
		return score;
	}

	public double calculateRegularizedLogMarginalLikelihood() {
		return forwardBackward.getMarginalLogLikelihood() - calculateRegularizer();
	}

	public double calculateRegularizer() {
		double result = 0.0;
		for (int f=0; f<numFeatures; ++f) {
			result += regularizationWeights[f]*(weights[f] - regularizationBiases[f])*(weights[f] - regularizationBiases[f]);
		}
		return result;
	}

	private double[][][] getDTransDCoefficients() {
		double[][][] res = 
			new double[numHelperLanguages][numLabels][numLabels];
		ArrayUtil.fill(res, 0);
		double[][] temp = new double[numLabels][numLabels];
		ArrayUtil.fill(temp, 0.0);
		for (int l0 = 0; l0 < numLabels; ++l0) {
			for (int l1 = 0; l1 < numLabels; ++l1) {
				temp[l0][l1] += -transProbs[l0][l1];
			}
		}
		for (int l1 = 0; l1 < numLabels; ++l1) {
			temp[l1][l1] += 1.0;
		}		
		double[] constants = new double[numLabels];
		for (int h = 0; h < numHelperLanguages; ++h) {
			for (int l0 = 0; l0 < numLabels; ++l0) {
				Arrays.fill(constants, 0.0);
				for (int l1 = 0; l1 < numLabels; ++l1) {
					constants[l1] = coefficientProductWithTransProbs[h][l0][l1] - convexCoefficients[h][l0];
				}
				res[h][l0] = SolveEquation.solve(temp, constants);
			}
		}		
		return res;
	}

	public Pair<Double, double[]> calculate(double[] x) {
		setWeights(x);
		computePotentials();
		forwardBackward.compute();
		double[][] expectedTransCounts = forwardBackward.getConditionalExpectedTransCounts();
		double[][] expectedEmitCounts = forwardBackward.getConditionalExpectedEmitCounts();
		double[] expectedLabelCounts = forwardBackward.getConditionalExpectedLabelCounts();
		// double[][][] dTransDCoefficients = getDTransDCoefficients();		
		
		double negativeRegularizedLogMarginalLikelihood = -calculateRegularizedLogMarginalLikelihood();
		LogInfo.logss("Calc %d log marginal prob: %.2f", numCalculates, -negativeRegularizedLogMarginalLikelihood);

		// Calculate gradient
		double[] gradient = new double[weights.length];

		// Gradient of interpolationWeights,
		// old method
		/*for (int h = 0; h < numHelperLanguages; ++h) {
			for (int s0 = 0; s0 < numLabels; ++s0) {
				if (s0 != stopLabel) {
					double sum = 0.0;
					for (int s1=0; s1<numLabels; ++s1) {
						if (s1 != startLabel) {
							double grad = -expectedTransCounts[s0][s1];
							grad  -= -expectedLabelCounts[s0]*transProbs[s0][s1];
							sum += grad * dTransDCoefficients[h][s0][s1];
						}
					}
					gradient[activeInterpolationFeatures[h][s0].getFirst()] = sum;
				}
			}
		}*/		
		
		for (int h = 0; h < numHelperLanguages; ++h) {
			for (int s0 = 0; s0 < numLabels; ++s0) {
				if (s0 != stopLabel) {
					double sum = 0.0;
					for (int s1=0; s1<numLabels; ++s1) {
						if (s1 != startLabel) {
							double grad = expectedTransCounts[s0][s1] * 
								(coefficientProductWithTransProbs[h][s0][s1] - convexCoefficients[h][s0]);
							sum -= grad;
						}
					}
					gradient[activeInterpolationFeatures[h][s0].getFirst()] = sum;
				}
			}
		}
		
		// Gradient of emit weights
		for (int s=0; s<numLabels; ++s) {
			if (s != startLabel && s != stopLabel) {
				for (int i=0; i<numObservations; ++i) {
					for (int f=0; f<activeEmitFeatures[s][i].size(); ++f) {
						Pair<Integer,Double> feat = activeEmitFeatures[s][i].get(f);
						if (feat.getSecond() != Double.NEGATIVE_INFINITY) {
							gradient[feat.getFirst()] -= expectedEmitCounts[s][i]*feat.getSecond();
							gradient[feat.getFirst()] -= -expectedLabelCounts[s]*emitProbs[s][i]*feat.getSecond();
						}
					}
				}
			}
		}
		// Add gradient of regularizer
		for (int f=0; f<numFeatures; ++f) {
			gradient[f] -= -2.0*regularizationWeights[f]*(weights[f] - regularizationBiases[f]);
		}

		++numCalculates;
		return Pair.makePair(negativeRegularizedLogMarginalLikelihood, gradient);
	}

	public int dimension() {
		return numFeatures;
	}

	public double[][][] getAllPosteriors() {
		return forwardBackward.getNodePosteriors();
	}
}