package edu.cmu.cs.lti.ark.ssl.pos.crf;

import java.io.Serializable;

import edu.berkeley.nlp.math.DoubleArrays;
import edu.berkeley.nlp.math.DoubleMatrices;
import edu.berkeley.nlp.util.ArrayUtil;
import edu.berkeley.nlp.util.PriorityQueue;
import edu.cmu.cs.lti.ark.ssl.pos.EdgeFeatureExtractor;
import edu.cmu.cs.lti.ark.ssl.pos.VertexFeatureExtractor;
import fig.basic.Pair;

public class Inference implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2853207437887775591L;
	private ScoreCalculator scoreCalculator;
	private int numLabels;
	
	// SCALING
	public static final double SCALE = Math.exp(100);
	public static final double LOG_SCALE = Math.log(SCALE);
	// Note: e^709 is the largest double java can handle.
	private static final int MAX_LOGSCALE = 100;
	
	public Inference(int numLabels, 
					 VertexFeatureExtractor vertexExtractor, 
					 EdgeFeatureExtractor edgeExtractor) {
		scoreCalculator = new ScoreCalculator(vertexExtractor, edgeExtractor, numLabels);
		this.numLabels = numLabels;
	}
	
	// returns the alphas and the scaling factors
	public Pair<double[][], double[]> getAlphas(int[] sequence, double[] w) {
		int n = sequence.length;
		double[][] alpha = new double[n][];
		double max = Double.NEGATIVE_INFINITY;
		double[] alphaScalingFactors = new double[n];
		for (int i = 0; i < n; i ++) {
			if (i == 0) {
				alpha[i] = scoreCalculator.getVertexScores(sequence, i, w);
			} else {
				double[][] scoreMatrix = scoreCalculator.getScoreMatrix(sequence, i, w);
				alpha[i] = DoubleMatrices.product(alpha[i-1], scoreMatrix);
			}
			double maxInArray = DoubleArrays.max(alpha[i]);
			if (maxInArray > max)  max = maxInArray;
			if (max == 0.0 || Double.isInfinite(max)) {
				throw new RuntimeException(String.format("The alphas[%d] has max=%.3f",0,max));
			}	
			int logScale = 0;
			double scale = 1.0;
			while (max > SCALE) {
				max /= SCALE;
				scale *= SCALE;
				logScale += 1;
				if (logScale > MAX_LOGSCALE) throw new RuntimeException("Max log scale exceeded.");
			}
			while (max > 0.0 && max < 1.0 / SCALE) {
				max *= SCALE;
				scale /= SCALE;
				logScale -= 1;
			}
			if (logScale != 0) {
				for (int label=0; label < numLabels; ++label) {
					alpha[i][label] /= scale;
				}
			}
			if (i == 0) {
				alphaScalingFactors[i] = logScale;
			} else {
				alphaScalingFactors[i] = alphaScalingFactors[i-1] + logScale;
			}
		}
		return Pair.makePair(alpha, alphaScalingFactors);
	}
	
	public Pair<double[][], double[]> getBetas(int[] sequence, double[] w) {
		int n = sequence.length;
		double[][] beta = new double[n][];
		double[] betaScalingFactors = new double[n];
		for (int i = n - 1; i >= 0; i--) {
			double max = 0.0;
			if (i == n - 1) {				
				beta[i] = DoubleArrays.constantArray(1.0, numLabels);
			} else {
				double[][] scoreMatrix = scoreCalculator.getScoreMatrix(sequence, i+1, w);
				beta[i] = DoubleMatrices.product(scoreMatrix, beta[i+1]);
			}
			double maxInArray = DoubleArrays.max(beta[i]);
			if (maxInArray > max)  max = maxInArray;
			int logScale = 0;
			double scale = 1.0;
			while (max > SCALE) {
				max /= SCALE;
				scale *= SCALE;
				logScale += 1;
				if (logScale > MAX_LOGSCALE) throw new RuntimeException("Max log scale exceeded.");
			}
			while (max > 0.0 && max < 1.0 / SCALE) {
				max *= SCALE;
				scale /= SCALE;
				logScale -= 1;
			}
			if (logScale != 0) {
				for (int label=0; label < numLabels; ++label) {
					beta[i][label] /= scale;
				}
			}
			if (i == n-1) {
				betaScalingFactors[i] = logScale;
			}  else {
				betaScalingFactors[i] = betaScalingFactors[i+1] + logScale;
			}
		}
		return Pair.makePair(beta, betaScalingFactors);
	}
	
	public Pair<int[][][][], double[][][]> getKBestChartAndBacktrace(int[] sequence, double[] w, int k) {
		int n = sequence.length;
		int[][][][] bestLabels = new int[n][numLabels][][];
		double[][][] bestScores = new double[n][numLabels][];
		double[] startScores = scoreCalculator.getLinearVertexScores(sequence, 0, w);
		for (int l=0; l<numLabels; l++) {
			bestScores[0][l] = new double[] { startScores[l] };
			bestLabels[0][l] = new int[][] { new int[] {-1, 0 } };
		}
		for (int i=1; i<n; i++) {
			double[][] scoreMatrix = scoreCalculator.getLinearScoreMatrix(sequence, i, w);
			for (int l=0; l<numLabels; l++) {
				PriorityQueue<Pair<Integer, Integer>> pq = new PriorityQueue<Pair<Integer, Integer>>();
				for (int pl=0; pl<numLabels; pl++) {
					double edgeScore = scoreMatrix[pl][l];
					for (int c=0; c<bestScores[i-1][pl].length; c++) {
						double totalScore = edgeScore + bestScores[i-1][pl][c];
						pq.add(Pair.makePair(pl, c), totalScore);
					}
				}
				int cands = Math.min(k, pq.size());
				bestScores[i][l] = new double[cands];
				bestLabels[i][l] = new int[cands][2];
				for (int c=0; c<cands; c++) {
					bestScores[i][l][c] = pq.getPriority();
					Pair<Integer, Integer> backtrace = pq.next();
					bestLabels[i][l][c][0] = backtrace.getFirst();
					bestLabels[i][l][c][1] = backtrace.getSecond();
				}
			}
		}
		return Pair.makePair(bestLabels, bestScores);
	}
	
	public double[][] getVertexPosteriors(
			Pair<double[][], double[]> alphaAndFactors, 
			Pair<double[][], double[]> betaAndFactors) {
		double[][] alpha = alphaAndFactors.getFirst();
		double[] alphaScalingFactors = alphaAndFactors.getSecond();
		
		double[][] beta = betaAndFactors.getFirst();
		double[] betaScalingFactors = betaAndFactors.getSecond();
		
		int obsLength = alpha.length;
		double z_scale = alphaScalingFactors[obsLength-1];
		double z = DoubleArrays.add(alpha[obsLength-1]);
		
		double[][] p = new double[alpha.length][numLabels];
		for (int i=0; i<p.length; i++) {
			double beta_scale = betaScalingFactors[i];
			double alpha_scale = alphaScalingFactors[i];
			double posterior_scale = alpha_scale + beta_scale - z_scale;
			double exp_scale = getScaleFactor(posterior_scale);
			assert Math.abs(posterior_scale) <= 3.0 : "Exp scale is " + posterior_scale;
			for (int l=0; l<p[i].length; l++) {
				double unscaled_posterior = alpha[i][l] * beta[i][l] / z;
//				System.out.println("alpha["+i+"]["+l+"]="+alpha[i][l]);
//				System.out.println("beta["+i+"]["+l+"]="+beta[i][l]);
//				System.out.println("z="+z);
//				System.out.println("unscaled_posterior:"+unscaled_posterior);
//				System.out.println("exp_scale:"+exp_scale);
				p[i][l] = unscaled_posterior * exp_scale;
//				System.out.println("p["+i+"]["+l+"]="+p[i][l]);
			}
			//ArrayUtil.normalize(p[i]);
		}
		return p;
	}
	
	public double[][][] getEdgePosteriors(
			int[] sequence, double[] w, 
			Pair<double[][], double[]> alphaAndFactors, 
			Pair<double[][], double[]> betaAndFactors) {
		int n = sequence.length;
		double[][] alpha = alphaAndFactors.getFirst();
		double[] alphaScalingFactors = alphaAndFactors.getSecond();
		
		double[][] beta = betaAndFactors.getFirst();
		double[] betaScalingFactors = betaAndFactors.getSecond();
		
		int obsLength = alpha.length;
		double z_scale = alphaScalingFactors[obsLength-1];
		double z = DoubleArrays.add(alpha[obsLength-1]);
		
		double[][][] p = new double[n][numLabels][numLabels];
		for (int i=1; i<p.length; i++) {
			double beta_scale = betaScalingFactors[i];
			double alpha_scale = alphaScalingFactors[i-1];
			double posterior_scale = alpha_scale + beta_scale - z_scale;
			double exp_scale = getScaleFactor(posterior_scale);
			assert Math.abs(posterior_scale) <= 3.0 : "Exp scale is " + posterior_scale;		
			double[][] scoreMatrix = scoreCalculator.getScoreMatrix(sequence, i, w);
			for (int lp=0; lp<numLabels; lp++) {
				for (int lc=0; lc<numLabels; lc++) {
//					System.out.println("alpha["+(i-1)+"]["+lp+"]="+alpha[i-1][lp]);
//					System.out.println("beta["+i+"]["+lc+"]="+beta[i][lc]);
//					System.out.println("z="+z);
//					System.out.println("exp_scale:"+exp_scale);
//					System.out.println("scoreMatrix["+lp+"]["+lc+"]="+scoreMatrix[lp][lc]);
					double unscaled_posterior = 
						alpha[i-1][lp] * scoreMatrix[lp][lc] * beta[i][lc] / z;
//					System.out.println("unscaled_posterior:"+unscaled_posterior);
					p[i][lp][lc] = unscaled_posterior * exp_scale;
//					System.out.println("p["+i+"]["+lp+"]["+lc+"]="+p[i][lp][lc]);
				}
			}
			//ArrayUtil.normalize(p[i]);
		}
		return p;
	}
	
	private static double getScaleFactor(double logScale) {
		if (logScale == 0.0) return 1.0;
		if (logScale == 1.0) return SCALE;
		if (logScale == 2.0) return SCALE * SCALE;
		if (logScale == 3.0) return SCALE * SCALE * SCALE;		
		if (logScale == -1.0) return 1.0 / SCALE;
		if (logScale == -2.0) return 1.0 / SCALE / SCALE;
		if (logScale == -3.0) return 1.0 / SCALE / SCALE / SCALE;		
		return Math.pow(SCALE, logScale);
	}
	

	public double getLogNormalizationConstant(
			Pair<double[][], double[]> alphaAndFactors, 
			Pair<double[][], double[]> betaAndFactors) {
		
		double[][] alpha = alphaAndFactors.getFirst();
		double[] alphaScalingFactors = alphaAndFactors.getSecond();
		
		int obsLength = alpha.length;
		double z = DoubleArrays.add(alpha[obsLength-1]);
		double z_scale = alphaScalingFactors[obsLength-1];
		
		double normConstant = z_scale * LOG_SCALE + Math.log(z);
		
		//System.out.println("Norm constant:" + normConstant);
		
		return normConstant;
	}
}
