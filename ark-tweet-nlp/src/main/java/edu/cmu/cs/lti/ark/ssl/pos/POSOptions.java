package edu.cmu.cs.lti.ark.ssl.pos;

import java.util.logging.Logger;

import jargs.gnu.CmdLineParser;
import fig.basic.Option;

public class POSOptions {
	
	private static Logger log = Logger.getLogger(SemiSupervisedPOSTagger.class.getCanonicalName());
	
	public CmdLineParser parser;
	@Option(gloss = "Tagged sentences for training")
	public CmdLineParser.Option trainSet;
	
	@Option(gloss = "Unlabeled sentences for SSL")
	public CmdLineParser.Option unlabeledSet;
		
	@Option(gloss = "Whether to use unlabeled data")
	public CmdLineParser.Option useUnlabeledData;
	
	@Option(gloss = "Tagged sentences for test")
	public CmdLineParser.Option testSet;	
		
	@Option(gloss = "Tagged sentences for test")
	public CmdLineParser.Option trainOrTest;	
	
	@Option(gloss = "Model file")
	public CmdLineParser.Option modelFile;
		
	@Option(gloss = "Run output")
	public CmdLineParser.Option runOutput;
	
	@Option(gloss = "Number of sentences to read from labeled train/test file.")
	public CmdLineParser.Option numLabeledSentences;
	
	@Option(gloss = "Number of sentences to read from labeled train/test file.")
	public CmdLineParser.Option numUnLabeledSentences;
		
	@Option(gloss = "Skip over sentences that have length longer than this number.")
	public CmdLineParser.Option maxSentenceLength;

	@Option(gloss = "Number of unsupervised tag clusters to use.")
	public CmdLineParser.Option numLabels;
	
	@Option(gloss = "Number of iterations to run for.")
	public CmdLineParser.Option iters;

	@Option(gloss = "Number of iterations between recording weights to file.")
	public CmdLineParser.Option printRate;

	@Option(gloss = "Forces only full-indicator features, and does standard M-step by normalizing expected counts.")
	public CmdLineParser.Option useStandardMultinomialMStep;

	@Option(gloss = "Add this pseudo-count to expected counts when doing standard M-step.")
	public CmdLineParser.Option standardMStepCountSmoothing;

	@Option(gloss = "If true, use locally normalized potentials. Otherwise use globally normalized.")
	public CmdLineParser.Option useGlobalForLabeledData;

	@Option(gloss = "Upper end point of interval from which initial weights are drawn UAR.")
	public CmdLineParser.Option initialWeightsUpper;

	@Option(gloss = "Lower end point of interval from which initial weights are drawn UAR.")
	public CmdLineParser.Option initialWeightsLower;

	@Option(gloss = "Regulariztion term is sum_i[ c*(w_i - b)^2 ] where c is regularization weight and b is regularization bias.")
	public CmdLineParser.Option regularizationWeight;

	@Option(gloss = "Regulariztion term is sum_i[ c*(w_i - b)^2 ] where c is regularization weight and b is regularization bias.")
	public CmdLineParser.Option regularizationBias;

	@Option(gloss = "If true, in addition to full-indicator features use the following features: 1) initial capital 2) contains digit 3) contains hyphen 4) 3-gram suffix indicators. Otherwise just use full-indicators.")
	public CmdLineParser.Option useStandardFeatures;

	@Option(gloss = "n to use in n-gram suffix features.")
	public CmdLineParser.Option lengthNGramSuffixFeature;
	
	@Option(gloss = "Use a bias feature that is constant.")
	public CmdLineParser.Option useBiasFeature;

	@Option(gloss = "Regularization bias for weight for bias feature.")
	public CmdLineParser.Option biasFeatureBias;

	@Option(gloss = "Regularization weight for weight for bias feature.")
	public CmdLineParser.Option biasFeatureRegularizationWeight;

	@Option(gloss = "Index of random seed to use. (10 possible random seeds are deterministically precomputed.)")
	public CmdLineParser.Option randSeedIndex;
	
	@Option(gloss = "Prefix to an execution directory")
	public CmdLineParser.Option execPoolDir;
	
	@Option(gloss = "Restart from a given model file")
	public CmdLineParser.Option restartTraining;
	
	@Option(gloss = "Restart from a given model file")
	public CmdLineParser.Option restartModelFile;		
	
	@Option(gloss = "Use same set of features in both labeled and unlabeled data.")
	public CmdLineParser.Option useSameSetOfFeatures;
	
	@Option(gloss = "Start with a trained supervised model.")
	public CmdLineParser.Option startWithTrainedSupervisedModel;
	
	@Option(gloss = "Trained supervised model.")
	public CmdLineParser.Option trainedSupervisedModel;
		
	@Option(gloss = "Weight given to unlabeled data.")
	public CmdLineParser.Option gamma;
		
	@Option(gloss = "If we want to use unlabeled data alone.")
	public CmdLineParser.Option useOnlyUnlabeledData;
	
	@Option(gloss = "Use model from which we'd like to read the reg parameters' means")
	public CmdLineParser.Option regParametersModel;	
	
	@Option(gloss = "Use tag dictionary")
	public CmdLineParser.Option useTagDictionary;	
		
	@Option(gloss = "Tag dictionary file")
	public CmdLineParser.Option tagDictionaryFile;	
	
	@Option(gloss = "Cluster to tag map file")
	public CmdLineParser.Option clusterToTagMappingFile;	
	
	@Option(gloss = "Train the HMM discriminatively")
	public CmdLineParser.Option trainHMMDiscriminatively;
	
	@Option(gloss = "Use stacked features")
	public CmdLineParser.Option useStackedFeatures;
	
	@Option(gloss = "Stacked file")
	public CmdLineParser.Option stackedFile;
	
	@Option(gloss = "Number of unsupervised tags")
	public CmdLineParser.Option numTags;	
	
	@Option(gloss = "File containing Noah's features")
	public CmdLineParser.Option noahsFeaturesFile;
	
	@Option(gloss = "File containing initial transition probabilities")
	public CmdLineParser.Option initTransitionsFile;
	
	@Option(gloss = "Use distributional similarities")
	public CmdLineParser.Option useDistSim;
	
	@Option(gloss = "Use names")
	public CmdLineParser.Option useNames;	
	
	@Option(gloss = "Use interpolation")
	public CmdLineParser.Option useInterpolation;
	
	@Option(gloss = "Fine to coarse tagmap file")
	public CmdLineParser.Option fineToCoarseMapFile;
	
	@Option(gloss = "Comma separated path to helper transitions")
	public CmdLineParser.Option pathToHelperTransitions;
	
	@Option(gloss = "Print posteriors")
	public CmdLineParser.Option printPosteriors;
	
	public POSOptions(String[] args) {
		parser = new CmdLineParser();
		trainSet = parser.addStringOption("trainSet");
		unlabeledSet = parser.addStringOption("unlabeledSet");
		useUnlabeledData = parser.addBooleanOption("useUnlabeledData");
		testSet = parser.addStringOption("testSet");
		trainOrTest = parser.addStringOption("trainOrTest");
		modelFile = parser.addStringOption("modelFile");
		runOutput = parser.addStringOption("runOutput");
		numLabeledSentences = parser.addIntegerOption("numLabeledSentences");
		numUnLabeledSentences = parser.addIntegerOption("numUnLabeledSentences");
		maxSentenceLength = parser.addIntegerOption("maxSentenceLength");
		numLabels = parser.addIntegerOption("numLabels");
		iters = parser.addIntegerOption("iters");
		printRate = parser.addIntegerOption("printRate");
		useStandardMultinomialMStep = parser.addBooleanOption("useStandardMultinomialMStep");
		standardMStepCountSmoothing = parser.addDoubleOption("standardMStepCountSmoothing");
		useGlobalForLabeledData = parser.addBooleanOption("useGlobalForLabeledData");
		initialWeightsUpper = parser.addDoubleOption("initialWeightsUpper");
		initialWeightsLower = parser.addDoubleOption("initialWeightsLower");
		regularizationWeight = parser.addDoubleOption("regularizationWeight");
		regularizationBias = parser.addDoubleOption("regularizationBias");
		useStandardFeatures = parser.addBooleanOption("useStandardFeatures");
		lengthNGramSuffixFeature = parser.addIntegerOption("lengthNGramSuffixFeature");
		useBiasFeature = parser.addBooleanOption("useBiasFeature");
		biasFeatureBias = parser.addDoubleOption("biasFeatureBias");
		biasFeatureRegularizationWeight = parser.addDoubleOption("biasFeatureRegularizationWeight");
		randSeedIndex = parser.addIntegerOption("randSeedIndex");
		execPoolDir = parser.addStringOption("execPoolDir");
		restartTraining = parser.addBooleanOption("restartTraining");
		restartModelFile = parser.addStringOption("restartModelFile");
		useSameSetOfFeatures = parser.addBooleanOption("useSameSetOfFeatures");
		startWithTrainedSupervisedModel = parser.addBooleanOption("startWithTrainedSupervisedModel");
		trainedSupervisedModel = parser.addStringOption("trainedSupervisedModel");
		gamma = parser.addDoubleOption("gamma");
		useOnlyUnlabeledData = parser.addBooleanOption("useOnlyUnlabeledData");
		regParametersModel = parser.addStringOption("regParametersModel");
		useTagDictionary = parser.addBooleanOption("useTagDictionary");			
		tagDictionaryFile = parser.addStringOption("tagDictionaryFile");
		clusterToTagMappingFile = parser.addStringOption("clusterToTagMappingFile");
		trainHMMDiscriminatively = parser.addBooleanOption("trainHMMDiscriminatively");
		useStackedFeatures = parser.addBooleanOption("useStackedFeatures");
		stackedFile = parser.addStringOption("stackedFile");
		numTags = parser.addIntegerOption("numTags");
		noahsFeaturesFile = parser.addStringOption("noahsFeaturesFile");
		initTransitionsFile = parser.addStringOption("initTransitionsFile");
		useDistSim = parser.addBooleanOption("useDistSim");
		useNames = parser.addBooleanOption("useNames");
		useInterpolation = parser.addBooleanOption("useInterpolation");
		fineToCoarseMapFile = parser.addStringOption("fineToCoarseMapFile");
		pathToHelperTransitions = parser.addStringOption("pathToHelperTransitions");
		printPosteriors = parser.addBooleanOption("printPosteriors");
	}
	
	public String[] parseArgs(String[] args) {	
		try {
			parser.parse(args);
		}
		catch ( CmdLineParser.OptionException e ) {
			log.severe(e.getMessage());
			System.exit(-1);
		}
	    return args;
	}	
}