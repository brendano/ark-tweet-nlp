package cmu.arktweetnlp;

import java.io.IOException;
import java.util.ArrayList;

import util.Arr;
import util.LBFGS;
import util.LBFGS.Status;
import util.U;

import cmu.arktweetnlp.impl.Model;
import cmu.arktweetnlp.impl.ModelSentence;
import cmu.arktweetnlp.impl.Sentence;
import cmu.arktweetnlp.impl.features.FeatureExtractor;
import cmu.arktweetnlp.io.CoNLLReader;

public class Train {

	public double penalty = 2;   // "lambda" in glmnet terminology
	public double l1mix = 0.01;  // "alpha" in glmnet terminology
	double l2penalty() { return penalty * (1-l1mix); }
	double l1penalty() { return penalty * l1mix; } 
	public double tol = 1e-7;   // 1e-5 seems ok for development, but 1e-7 is pretty reliable
	public int maxIter = 500;  // we usually finish in 100-200 iters
	public String modelLoadFilename = null;
	public String examplesFilename = null;
	public String modelSaveFilename = null;
	public boolean dumpFeatures = false;

	// Data structures
	private ArrayList<Sentence> lSentences;
	private ArrayList<ModelSentence> mSentences;
	private int numTokens = 0;
	private Model model;


	Train() {
		lSentences = new ArrayList<Sentence>();
		mSentences = new ArrayList<ModelSentence>();
		model = new Model();
	}

	public void doFeatureDumping() throws IOException {
		readTrainingSentences(examplesFilename);
		constructLabelVocab();
		extractFeatures();
		dumpFeatures();
	}

	public void doTraining() throws IOException {
		readTrainingSentences(examplesFilename);
		constructLabelVocab();
		extractFeatures();

		model.lockdownAfterFeatureExtraction();
		if (modelLoadFilename != null) {
			readWarmStartModel();
		}
		optimizationLoop();
		System.out.println("Saving model to " + modelSaveFilename);
		model.saveModelAsText(modelSaveFilename);
	}

	public void readTrainingSentences(String filename) throws IOException {
		lSentences = CoNLLReader.readFile(filename);
		for (Sentence sent : lSentences)
			numTokens += sent.T();
	}

	public void constructLabelVocab() {
		for (Sentence s : lSentences) {
			for (String l : s.labels) {
				model.labelVocab.num(l);
			}
		}
		model.labelVocab.lock();
		model.numLabels = model.labelVocab.size();
	}

	public void dumpFeatures() throws IOException {
		FeatureExtractor fe = new FeatureExtractor(model, true);
		fe.dumpMode = true;
		for (Sentence lSent : lSentences) {
			ModelSentence mSent = new ModelSentence(lSent.T());
			fe.computeFeatures(lSent, mSent);
		}
	}

	public void extractFeatures() throws IOException {
		System.out.println("Extracting features");
		FeatureExtractor fe = new FeatureExtractor(model, true);
		for (Sentence lSent : lSentences) {

			ModelSentence mSent = new ModelSentence(lSent.T());
			fe.computeFeatures(lSent, mSent);
			mSentences.add(mSent);
		}
	}

	public void readWarmStartModel() throws IOException {
		assert model.featureVocab.isLocked();
		Model warmModel = Model.loadModelFromText(modelLoadFilename);
		Model.copyCoefsForIntersectingFeatures(warmModel, model);
	}

	public void optimizationLoop() {
		double[] flatCoefs = model.convertCoefsToFlat();
		LBFGS.Params params = new LBFGS.Params();
		params.max_iterations = maxIter;
		params.orthantwise_c = l1penalty();
		params.orthantwise_start = model.observationFeature_to_flatID(0,0);
		params.delta = params.epsilon = tol;   // only 'delta' will matter
		
		LBFGS.Result r = LBFGS.lbfgs(flatCoefs, new GradientCalculator(),
			new LBFGS.ProgressCallback() {
				@Override
				public int apply(double[] x, double[] g, double fx,
						double xnorm, double gnorm, double step, int n, int iterNum,
						Status ls) {
					int numActive = 0;
					for (double coef : x) numActive += (coef != 0) ? 1 : 0;
					U.pf("iter %d: obj=%g active=%d gnorm=%g\n", iterNum, fx, numActive, gnorm);
//					if (false && iterNum % 5 == 0) {
//						try {
//							model.setCoefsFromFlat(x);
//							model.saveModelAsText(modelSaveFilename + ".iter" + k);
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
//					}
					return 0;
				}
		}, params);
		System.out.println("Finished status=" + r.status);
		model.setCoefsFromFlat(flatCoefs);
	}

	private class GradientCalculator implements LBFGS.Function {
		@Override
		public double evaluate(double[] flatCoefs, double[] g, int n, double step) {
			model.setCoefsFromFlat(flatCoefs);
			Arr.fill(g,0);
			double loglik = 0;
			for (ModelSentence s : mSentences) {
				loglik += model.computeGradientAndLL_CRF(s, g);
//				loglik += model.computeGradientAndLL_MEMM(s, g);
			}
			Arr.multiplyInPlace(g, -1);
			addL2regularizerGradient(g, flatCoefs);
			return -loglik + regularizerValue(flatCoefs);
		}
	}

	private void addL2regularizerGradient(double[] grad, double[] flatCoefs) {
		double l2pen = l2penalty();
		assert grad.length == flatCoefs.length;
		for (int f=0; f < flatCoefs.length; f++) {
			grad[f] += l2pen * flatCoefs[f]; 
		}
	}

	/**
	 * lambda_2 * (1/2) sum (beta_j)^2  +  lambda_1 * sum |beta_j|
	 * the library only wants the first term
	 */
	 private double regularizerValue(double[] flatCoefs) {
		double l2_term = 0;
		for (int f=0; f < flatCoefs.length; f++) {
			l2_term += Math.pow(flatCoefs[f], 2);
		}
		return 0.5*l2penalty()*l2_term;
	}

	 //////////////////////////////////////////////////////////////


	public static void main(String[] args) throws IOException {
		Train trainer = new Train();

		if (args.length < 2 || args[0].equals("-h") || args[1].equals("--help")) {
			usage();
		}

		int i=0;
		while (i < args.length) {
			if (!args[i].startsWith("-")) {
				break;
			}
			else if (args[i].equals("--warm-start")) {
				trainer.modelLoadFilename = args[i+1];
				i += 2;
			} 
			else if (args[i].equals("--max-iter")) {
				trainer.maxIter = Integer.parseInt(args[i+1]);
				i += 2;
			}
			else if (args[i].equals("--dump-feat")) {
				trainer.dumpFeatures = true;
				i += 1;
			} else if (args[i].equals("--penalty")) {
				trainer.penalty = Double.parseDouble(args[i+1]);
				i += 2;
			} else if (args[i].equals("--l1mix")) {
				trainer.l1mix = Double.parseDouble(args[i+1]);
				i += 2;
			}
			else {
				usage();        		
			}
		}

		if (trainer.dumpFeatures) {
			trainer.examplesFilename = args[i];
			trainer.doFeatureDumping();
			System.exit(0);
		}

		if (args.length - i < 2) usage();

		trainer.examplesFilename = args[i];
		trainer.modelSaveFilename = args[i+1];
		
		trainer.doTraining();
	}
	public static void usage() {
		System.out.println(
				"Train [options] <ExamplesFilename> <ModelOutputFilename>\n" +
				"Options:" +
				"\n  --max-iter <n>" +
				"\n  --warm-start <modelfile>    Initializes at weights of this model.  discards base features that aren't in training set." +
				"\n  --dump-feat                 Show extracted features, instead of training. Useful for debugging/analyzing feature extractors." +
				"\n"
		);
		System.exit(1);
	}


}
