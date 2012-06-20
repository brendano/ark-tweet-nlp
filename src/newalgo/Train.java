package newalgo;

import java.io.IOException;
import java.util.ArrayList;

import edu.berkeley.nlp.util.ArrayUtil;
import edu.stanford.nlp.optimization.DiffFunction;
import newalgo.OWLQN.WeightsPrinter;
import newalgo.io.CoNLLReader;
import newalgo.util.Util;

public class Train {

	public double l2penalty = 1;
    public double l1penalty = 0.01;
	public double tol = 1e-5;
    public int maxIter = 500;
//    public String modelLoadFilename = null;		
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
    

    public void doTraining() throws IOException {
        readTrainingSentences(examplesFilename);
        constructLabelVocab();
        if (dumpFeatures) {
        	dumpFeatures();
        	System.exit(0);
        }
        extractFeatures();
        model.lockdownAfterFeatureExtraction();
        optimizationLoop();
//      sgdLoop();
        model.dumpCoefs(modelSaveFilename);
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
    }
    
    public void dumpFeatures() {
    	FeatureExtractor fe = new FeatureExtractor(model, true);
    	fe.dumpMode = true;
    	for (Sentence lSent : lSentences) {
            ModelSentence mSent = new ModelSentence(lSent.T());
            fe.computeFeatures(lSent, mSent);
    	}
    }

    public void extractFeatures() {
        FeatureExtractor fe = new FeatureExtractor(model, true);
        for (Sentence lSent : lSentences) {
            ModelSentence mSent = new ModelSentence(lSent.T());
            fe.computeFeatures(lSent, mSent);
            mSentences.add(mSent);
        }
    }

    public void optimizationLoop() {
    	OWLQN minimizer = new OWLQN();
    	minimizer.setMaxIters(maxIter);
    	minimizer.setQuiet(false);
    	double[] initialWeights = new double[model.flatIDsize()];
    	
    	minimizer.setWeightsPrinting(new MyWeightsPrinter());
    	
        double[] finalWeights = minimizer.minimize(
        		new GradientCalculator(),
        		initialWeights, l1penalty, tol, 5);
        
        model.setCoefsFromFlat(finalWeights);
    }
   

    
    private class GradientCalculator implements DiffFunction {

		@Override
		public int domainDimension() {
			return model.flatIDsize();
		}

		@Override
		public double valueAt(double[] flatCoefs) {
			model.setCoefsFromFlat(flatCoefs);
			double loglik = 0;
			for (ModelSentence s : mSentences) {
				loglik += model.computeLogLik(s);
			}
			return -loglik + regularizerValue(flatCoefs);
		}

		@Override
		public double[] derivativeAt(double[] flatCoefs) {
			double[] g = new double[model.flatIDsize()];
			assert ArrayUtil.sum(g)==0;
			model.setCoefsFromFlat(flatCoefs);
			for (ModelSentence s : mSentences) {
				model.computeGradient(s, g);
			}
			ArrayUtil.multiplyInPlace(g, -1);
			addL2regularizerGradient(g, flatCoefs);
			return g;
		}
    }
    
    private void addL2regularizerGradient(double[] grad, double[] flatCoefs) {
    	assert grad.length == flatCoefs.length;
    	for (int f=0; f < flatCoefs.length; f++) {
    		grad[f] += l2penalty * flatCoefs[f]; 
    	}
    }
    private double l2RegularizerValue(double[] flatCoefs) {
    	double R = 0;
    	for (int f=0; f<flatCoefs.length; f++) {
    		R += Math.pow(f, 2);
    	}
    	return R * 0.5 * l2penalty;
    }
    /**
     * lambda_2 + (1/2) sum (beta_j)^2  +  lambda_1 + sum |beta_j|
     */
    private double regularizerValue(double[] flatCoefs) {
    	double l2_term = 0, l1_term = 0;
    	for (int f=0; f < flatCoefs.length; f++) {
    		l2_term += Math.pow(flatCoefs[f], 2);
    		l1_term += Math.abs(flatCoefs[f]);    			
    	}
    	return 0.5*l2penalty*l2_term+ l1penalty*l1_term;
    }
    
    public class MyWeightsPrinter implements WeightsPrinter {

		@Override
		public void printWeights() {
			double loglik = 0;
			for (ModelSentence s : mSentences) {
				loglik += model.computeLogLik(s);
			}
			System.out.printf("\tTokLL %.4f\t", loglik/numTokens);
		}
    }

    //////////////////////////////////////////////////////////////
    
    
    public static void main(String[] args) throws IOException {
        Train trainer = new Train();
        
        if (args.length < 2 || args[0].equals("-h") || args[1].equals("--help")) {
        	usage();
        }

        int i=0;
        while (i < args.length) {
        	if (!args[i].startsWith("-")) break;
//        	else if (args[i].equals("--load-model")) {
//        		trainer.modelLoadFilename = args[i+1];
//        		i += 2;
//        	} 
        	if (args[i].equals("--maxiter")) {
        		trainer.maxIter = Integer.parseInt(args[i+1]);
        		i += 2;
        	}
        	else if (args[i].equals("--dump-feat")) {
        		trainer.dumpFeatures = true;
        		i += 1;
        	}
        	else {
        		usage();        		
        	}
        }
        
        if (args.length - i < 2) usage();

        trainer.examplesFilename = args[i];
        trainer.modelSaveFilename = args[i+1];

    	trainer.doTraining();
        
    }
    public static void usage() {
    	System.out.println(
    			"Train [options]  examples_filename model_output_filename\n" +
    			"Options:\n" +
    			"  --load-model model_name_to_load\n"
    	);
    	System.exit(1);
    }
    

}
