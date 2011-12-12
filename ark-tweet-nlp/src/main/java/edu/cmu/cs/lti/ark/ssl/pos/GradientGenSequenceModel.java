package edu.cmu.cs.lti.ark.ssl.pos;

import java.util.List;

import pos_tagging.ForwardBackward;

import fig.basic.LogInfo;
import fig.basic.Pair;

public class GradientGenSequenceModel extends GradientSequenceModel {
	
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
	private List<Pair<Integer,Double>>[][] activeTransFeatures;
	private List<Pair<Integer,Double>>[][] activeEmitFeatures;
	private List<Pair<Integer,Double>>[][] activeStackedFeatures;
	private int numFeatures;
	public ForwardBackwardGen forwardBackward;
	private int[][] stackedLabels;
	private double[][] stackedEmitProbs;
	
	public GradientGenSequenceModel(int[][] observations0,
									int[][] stackedLabels0,
								    int numLabels0, 
								    int numObservations0,
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
		this.stopLabel = numLabels-2;
		this.startLabel = numLabels-1;
		if (stackedLabels0 != null) {
			stackedLabels = stackedLabels0;
			stackedEmitProbs = new double[numLabels][numLabels - 2];
		} else {
			stackedLabels = stackedLabels0;
			stackedEmitProbs = null;
		}
		this.forwardBackward = 
			new ForwardBackwardGen(observations, 
					stackedLabels0,
					numLabels0, 
					numObservations, 
					transProbs, 
					emitProbs,
					stackedEmitProbs,
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
			List<Pair<Integer,Double>>[][] activeStackedFeatures0,
			int numFeatures0, 
			double[] regularizationWeights0, 
			double[] regularizationBiases0) {
		this.regularizationWeights = regularizationWeights0;
		this.regularizationBiases = regularizationBiases0;
		this.numFeatures = numFeatures0;
		this.activeTransFeatures = activeTransFeatures0;
		this.activeEmitFeatures = activeEmitFeatures0;
		activeStackedFeatures = activeStackedFeatures0;
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
		for (int l0=0; l0<numLabels; ++l0) {
			if (l0 != stopLabel) {
				double norm = 0.0;
				for (int l1=0; l1<numLabels; ++l1) {
					if (l1 != startLabel) {
						transProbs[l0][l1] = Math.exp(computeScore(weights, activeTransFeatures[l0][l1]));
						norm += transProbs[l0][l1];
					}
				}
				for (int l1=0; l1<numLabels; ++l1) {
					if (l1 != startLabel) {
						transProbs[l0][l1] /= norm;
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
		
		// stacked emit probabilities
		if (stackedLabels != null) {
			for (int l=0; l<numLabels; ++l) {
				if (l != startLabel && l != stopLabel) {
					double norm = 0.0;
					for (int i=0; i < (numLabels - 2); ++i) {
						stackedEmitProbs[l][i] = Math.exp(computeScore(weights, activeStackedFeatures[l][i]));
						norm += stackedEmitProbs[l][i];
					}
					for (int i=0; i < (numLabels - 2); ++i) {
						stackedEmitProbs[l][i] /= norm;
					}
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
	
	public Pair<Double, double[]> calculate(double[] x) {
		setWeights(x);
		computePotentials();
		forwardBackward.compute();
		double[][] expectedTransCounts = forwardBackward.getConditionalExpectedTransCounts();
		double[][] expectedEmitCounts = forwardBackward.getConditionalExpectedEmitCounts();
		double[] expectedLabelCounts = forwardBackward.getConditionalExpectedLabelCounts();

		double negativeRegularizedLogMarginalLikelihood = -calculateRegularizedLogMarginalLikelihood();
		LogInfo.logss("Calc %d log marginal prob: %.2f", numCalculates, -negativeRegularizedLogMarginalLikelihood);

		// Calculate gradient
		double[] gradient = new double[weights.length];

		// Gradient of trans weights
		for (int s0=0; s0<numLabels; ++s0) {
			if (s0 != stopLabel) {
				for (int s1=0; s1<numLabels; ++s1) {
					if (s1 != startLabel) {
						for (int f=0; f<activeTransFeatures[s0][s1].size(); ++f) {
							Pair<Integer,Double> feat = activeTransFeatures[s0][s1].get(f);
							if (feat.getSecond() != Double.NEGATIVE_INFINITY) {
								gradient[feat.getFirst()] -= expectedTransCounts[s0][s1]*feat.getSecond();
								gradient[feat.getFirst()] -= -expectedLabelCounts[s0]*transProbs[s0][s1]*feat.getSecond();
							}
						}
					}
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
		
		// Gradient of stacked emit weights
		if (stackedLabels != null) {
			double[][] expectedStackedEmitCounts = 
				forwardBackward.getConditionalExpectedStackedEmitCounts();
			for (int s=0; s<numLabels; ++s) {
				if (s != startLabel && s != stopLabel) {
					for (int i=0; i<numLabels-2; ++i) {
						for (int f=0; f<activeStackedFeatures[s][i].size(); ++f) {
							Pair<Integer,Double> feat = activeStackedFeatures[s][i].get(f);
							if (feat.getSecond() != Double.NEGATIVE_INFINITY) {
								gradient[feat.getFirst()] -= expectedStackedEmitCounts[s][i]*feat.getSecond();
								gradient[feat.getFirst()] -= -expectedLabelCounts[s]*stackedEmitProbs[s][i]*feat.getSecond();
							}
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

	@Override
	public void setActiveFeatures(
			Pair<Integer, Double>[][] activeInterpolationFeatures0,
			List<Pair<Integer, Double>>[][] activeEmitFeatures0,
			List<Pair<Integer, Double>>[][] activeStackedFeatures0,
			int numFeatures0, double[] regularizationWeights0,
			double[] regularizationBiases0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double[][][] getAllPosteriors() {
		return forwardBackward.getNodePosteriors();
	}
	
}