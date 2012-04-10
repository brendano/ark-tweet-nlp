package edu.cmu.cs.lti.ark.ssl.pos;

import pos_tagging.ForwardBackward;
import util.StationaryForwardBackward;
import edu.berkeley.nlp.sequence.stationary.StationarySequenceInstance;
import edu.berkeley.nlp.sequence.stationary.StationarySequenceModel;
import fig.basic.LogInfo;

public class ForwardBackwardGen implements ForwardBackward {
	
	private int numLabels;
	private int numObservations;
	private int[][] observations;
	private int[][] stackedLabels;
	private int startLabel;
	private int stopLabel;
	private double[][] transProbs;
	private double[][] emitProbs;
	private double[][] stackedEmitProbs;
	private double[] condExpectedLabelCounts;
	private double[][] condExpectedTransCounts;
	private double[][] condExpectedEmitCounts;
	private double[][] condExpectedStackedEmitCounts;
	private int[][] posteriorDecoding;
	private double marginalLogLikelihood;
	private boolean underflow;
	private boolean storePosteriors;
	private double[][][] allPosteriors;
	
	private class SequenceModel implements StationarySequenceModel {
		
		private int maxSeqLength;
		private int[][] allowableBackwardTrans;
		private int[][] allowableForwardTrans;
		private double[][] backwardEdgePotentials;
		private double[][] forwardEdgePotentials;
		
		public SequenceModel() {
			maxSeqLength=0;
			for (int s=0; s<observations.length; ++s) {
				maxSeqLength = Math.max(observations[s].length, maxSeqLength);
			}
			
			allowableBackwardTrans = new int[numLabels-2][numLabels-2];
			allowableForwardTrans = new int[numLabels-2][numLabels-2];
			for(int l0=0; l0<numLabels-2; ++l0) {
				for(int l1=0; l1<numLabels-2; ++l1) {
					allowableBackwardTrans[l0][l1] = l1;
					allowableForwardTrans[l0][l1] = l1;
				}
			}

			backwardEdgePotentials = new double[numLabels-2][numLabels-2];
			forwardEdgePotentials = new double[numLabels-2][numLabels-2];
			for (int l0=0; l0<numLabels-2; ++l0) {
				for (int l1=0; l1<numLabels-2; ++l1) {
					backwardEdgePotentials[l1][l0] = transProbs[l0][l1];
					forwardEdgePotentials[l0][l1] = transProbs[l0][l1];
				}
			}
		}

		public int[][] getAllowableBackwardTransitions() {
			return allowableBackwardTrans;
		}

		public int[][] getAllowableForwardTransitions() {
			return allowableForwardTrans;
		}

		public double[][] getBackwardEdgePotentials() {
			return backwardEdgePotentials;
		}

		public double[][] getForwardEdgePotentials() {
			return forwardEdgePotentials;
		}

		public int getMaximumSequenceLength() {
			return maxSeqLength;
		}

		public int getNumStates() {
			return numLabels-2;
		}
		
	}
	
	private class SequenceInstance implements StationarySequenceInstance {

		private int[] observationSequence;
		private int[] stackedLabels;
				
		public SequenceInstance(int[] observationSequence, int[] stackedLabels) {
			this.observationSequence = observationSequence;
			this.stackedLabels = stackedLabels;
		}
		
		public void fillNodePotentials(double[][] potentials) {
			for (int i=0; i<observationSequence.length; ++i) {
				for (int l=0; l<numLabels-2; ++l) {
					if (i == 0) {
						potentials[i][l] = emitProbs[l][observationSequence[i]] * transProbs[startLabel][l];
						if (stackedLabels != null) {
							potentials[i][l] *= stackedEmitProbs[l][stackedLabels[i]];
						}
					} else if (i == observationSequence.length-1) {
						potentials[i][l] = emitProbs[l][observationSequence[i]] * transProbs[l][stopLabel];
						if (stackedLabels != null) {
							potentials[i][l] *= stackedEmitProbs[l][stackedLabels[i]];
						}
					} else {
						potentials[i][l] = emitProbs[l][observationSequence[i]];
						if (stackedLabels != null) {
							potentials[i][l] *= stackedEmitProbs[l][stackedLabels[i]];
						}
					}
				}
			}
		}

		public int getSequenceLength() {
			return observationSequence.length;
		}
		
	}
	
	public ForwardBackwardGen(int[][] observations0, 
			int[][] stackedLabels0,
			int numLabels0, 
			int numObservations0, 
			double[][] transProbs0, 
			double[][] emitProbs0,
			double[][] stackedEmitProbs0,
			boolean storePosteriors) {
		this.numLabels = numLabels0+2;
		this.numObservations = numObservations0;
		this.observations = observations0;
		this.transProbs = transProbs0;
		this.emitProbs = emitProbs0;
		this.stackedEmitProbs = stackedEmitProbs0;
		this.stackedLabels = stackedLabels0;
		this.startLabel = numLabels-1;
		this.stopLabel = numLabels-2;
		this.condExpectedLabelCounts = new double[numLabels];
		this.condExpectedTransCounts = new double[numLabels][numLabels];
		this.condExpectedEmitCounts = new double[numLabels][numObservations];
		this.condExpectedStackedEmitCounts = new double[numLabels][numLabels - 2];
		this.posteriorDecoding = new int[observations.length][];
		this.underflow = false;
		this.storePosteriors = storePosteriors;
		if (storePosteriors) {
			this.allPosteriors = new double[observations.length][][];
		}
	}

	public int[][] posteriorDecode() {
		return posteriorDecoding;
	}
	
	public double getMarginalLogLikelihood() {
		return marginalLogLikelihood;
	}
	
	public boolean underflow() {
		return underflow;
	}	
	
	public void compute() {
		underflow = false;
		for (int s=0; s<numLabels; ++s) {
			condExpectedLabelCounts[s] = 0.0;
			for (int s0=0; s0<numLabels; ++s0) {
				condExpectedTransCounts[s][s0] = 0.0;
			}
			for (int e=0; e<numObservations; ++e) {
				condExpectedEmitCounts[s][e] = 0.0;
			}
			if (stackedLabels != null)  {
				for (int e=0; e<numLabels-2; ++e) {
					condExpectedStackedEmitCounts[s][e] = 0.0;
				}
			}
		}
		posteriorDecoding = new int[observations.length][];
		marginalLogLikelihood = 0.0;

		
		SequenceModel seqModel = new SequenceModel();
		for (int s=0; s<observations.length; ++s) {
			if (observations[s].length == 0) {
				condExpectedLabelCounts[startLabel] += 1.0;
				condExpectedLabelCounts[stopLabel] += 1.0;
				condExpectedTransCounts[startLabel][stopLabel] += 1.0;
				posteriorDecoding[s] = new int[0];
				if (storePosteriors) {
					allPosteriors[s] = new double[0][];
				}
				marginalLogLikelihood += Math.log(transProbs[startLabel][stopLabel]);
				continue;
			}
			
			int[] observationSequence = observations[s];
			int[] stackedSequence = null;
			if (stackedLabels != null) {
				stackedSequence = stackedLabels[s];
			}
			SequenceInstance seqInstance = new SequenceInstance(observationSequence, stackedSequence);
			StationaryForwardBackward forwardBackward = new StationaryForwardBackward(seqModel);
			try {
				forwardBackward.setInput(seqInstance);
			} catch (RuntimeException e) {
				// If forward backward has problems, maybe it's because our weights are bad, try again line searcher
				underflow = true;
				LogInfo.logss(e.getMessage());
				marginalLogLikelihood = Double.NEGATIVE_INFINITY;
				return;
			}
			posteriorDecoding[s] = forwardBackward.nodePosteriorDecode();
			marginalLogLikelihood += forwardBackward.getLogNormalizationConstant();
			if (storePosteriors) {
				allPosteriors[s] = new double[observationSequence.length][numLabels - 2];
			}
			// Label counts and emission counts
			condExpectedLabelCounts[startLabel] += 1.0;
			for (int i=0; i<observationSequence.length; ++i) {
				for (int l=0; l<numLabels-2; ++l) {
					condExpectedLabelCounts[l] += forwardBackward.getNodeMarginals()[i][l];
					condExpectedEmitCounts[l][observationSequence[i]] += forwardBackward.getNodeMarginals()[i][l];
					if (stackedLabels != null) {
						condExpectedStackedEmitCounts[l][stackedSequence[i]] += forwardBackward.getNodeMarginals()[i][l];
					}
					if (storePosteriors) {
						allPosteriors[s][i][l] = forwardBackward.getNodeMarginals()[i][l];
					}
				}
			}
			condExpectedLabelCounts[stopLabel] += 1.0;
			
			// Trans counts
			for (int l=0; l<numLabels-2; ++l) {
				condExpectedTransCounts[startLabel][l] += forwardBackward.getNodeMarginals()[0][l];
			}
			for (int l0=0; l0<numLabels-2; ++l0) {
				for (int l1=0; l1<numLabels-2; ++l1) {
					condExpectedTransCounts[l0][l1] += forwardBackward.getEdgeMarginalSums()[l0][l1];
				}
			}
			for (int l=0; l<numLabels-2; ++l) {
				condExpectedTransCounts[l][stopLabel] += forwardBackward.getNodeMarginals()[observationSequence.length-1][l];
			}		
		}
	}
	
	public double[][] getConditionalExpectedTransCounts() {
		return condExpectedTransCounts;
	}
	
	public double[] getConditionalExpectedLabelCounts() {
		return condExpectedLabelCounts;
	}
	
	public double[][] getConditionalExpectedEmitCounts() {
		return condExpectedEmitCounts;
	}
	
	public double[][] getConditionalExpectedStackedEmitCounts() {
		return condExpectedStackedEmitCounts;
	}
	
	public double[][][] getNodePosteriors() {
		return allPosteriors;
	}
}