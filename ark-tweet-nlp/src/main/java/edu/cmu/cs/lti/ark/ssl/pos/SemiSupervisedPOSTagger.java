package edu.cmu.cs.lti.ark.ssl.pos;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;

import edu.berkeley.nlp.math.DifferentiableFunction;
import edu.berkeley.nlp.util.CallbackFunction;
import edu.berkeley.nlp.util.Counter;
import edu.berkeley.nlp.util.Lists;
import edu.berkeley.nlp.util.PriorityQueue;
import edu.cmu.cs.lti.ark.ssl.pos.POSFeatureTemplates.EmitFeatureTemplate;
import edu.cmu.cs.lti.ark.ssl.pos.POSFeatureTemplates.InterpolationFeatureTemplate;
import edu.cmu.cs.lti.ark.ssl.pos.POSFeatureTemplates.TransFeatureTemplate;
import edu.cmu.cs.lti.ark.ssl.pos.crf.CRFObjectiveFunction;
import edu.cmu.cs.lti.ark.ssl.pos.crf.Inference;
import edu.cmu.cs.lti.ark.ssl.util.*;
import fig.basic.Pair;
import jargs.gnu.CmdLineParser;

/**
 *
 * @author dipanjan
 * TODO: implement stacking for CRF
 */
public class SemiSupervisedPOSTagger {
	/**
	 *
	 */
	private static final long serialVersionUID = 481162207516110632L;

	public static Logger log = Logger.getLogger(SemiSupervisedPOSTagger.class.getCanonicalName());

	public static Random baseRand = new Random(43569);
	public static Random[] rands;
    private final static String[] NAMES;
    private static final Map<String, double[]> DIST_SIM;


	static {
		rands = new Random[10];
		for (int i=0; i<10; ++i) {
			rands[i] = new Random(baseRand.nextInt());
		}
        NAMES = initNames();
        DIST_SIM = initDistSim();
	}

	private POSOptions options;
	private CmdLineParser parser;

	/**
	 * Variables needed for training and testing
	 * the POS tagger.
	 */
	private ArrayList<String> indexToPOS;
	private ArrayList<String> indexToWord;
	private ArrayList<String> indexToFeature;
	private Map<String, Integer> posToIndex;
	private Map<String, Integer> wordToIndex;
	private Map<String, Integer> featureToIndex;
	private ArrayList<Integer> featureIndexCounts;
	private List<Pair<Integer,Double>>[][] activeTransFeatures;
	private List<Pair<Integer,Double>>[][] activeEmitFeatures;
	private List<Pair<Integer,Double>>[][] activeCRFTransFeatures;
	private List<Pair<Integer,Double>>[][] activeCRFEmitFeatures;
	private List<Pair<Integer,Double>>[][] activeStackedFeatures;

	/**
	 * Various input options
	 */
	private String trainSet;
	private String unlabeledSet;
	private boolean useUnlabeledData;
	private String testSet;
	private String trainOrTest;
	private String modelFile;
	private String runOutput = null;
	private int numLabeledSentences;
	private int numUnLabeledSentences;
	private int maxSentenceLength;
	private int iters;
	private int printRate;
	private boolean useStandardMultinomialMStep;
	private double standardMStepCountSmoothing;
	private boolean useGlobalForLabeledData;
	private double initialWeightsUpper;
	private double  initialWeightsLower;
	private double regularizationWeight;
	private double regularizationBias;
	private boolean useStandardFeatures;
	private int lengthNGramSuffixFeature;
	private boolean useBiasFeature;
	private double biasFeatureBias;
	private double biasFeatureRegularizationWeight;
	private int randSeedIndex;
	private String execPoolDir;
	private Collection<Pair<List<String>, List<String>>> sequences;
	private Collection<List<String>> uSequences;
	private boolean restartTraining;
	private String restartModelFile;
	private boolean useSameSetOfFeatures;
	private boolean startWithTrainedSupervisedModel;
	private String trainedSupervisedModelFile;
	private POSModel trainedSupervisedModel;
	private double gamma;
	private boolean useOnlyUnlabeledData;
	private String regParametersModelFile;
	private POSModel regParametersModel;
	private boolean useTagDictionary;
	private String tagDictionaryFile;
	private String clusterToTagMappingFile;
	private int[][] tagsToClusters;
	private int[][] tagDictionary;
	private boolean trainHMMDiscriminatively;
	private boolean useStackedFeatures;
	private String stackedFile;
	private int[][] stackedTags;
	private int numTags = 0;
	private String noahsFeaturesFile = null;
	private Map<String, String> noahsFeatures;
	private String initTransitionsFile = null;
	double[][] initTransProbs = null;
	private boolean useDistSim;
	private Map<String, double[]> distSimTable;
	private boolean useNames;
	private String[] namesArray;
	private boolean printPosteriors = false;

	// interpolation options
	private boolean useInterpolation;
	private int numHelperLanguages;
	double[][][] transitionMatrices;
	private Pair<Integer,Double>[][] activeConvexCombiners;
	private String fineToCoarseMapFile;
	private String pathToHelperTransitions;
	private String[] validTagArray; // unsorted array, with END and then START at the finish


	/**
	 * labeled observations
	 */
	private int[][] lObservations;
	/**
	 * labeled of the supervised part of the data
	 */
	private int[][] goldLabels;
	/**
	 * unlabeled observations
	 */
	private int[][] uObservations;


    public static void main(String[] args) {
		POSOptions options = new POSOptions(args);
		options.parseArgs(args);
		SemiSupervisedPOSTagger tagger = new SemiSupervisedPOSTagger(options);
		tagger.run();
	}

	public SemiSupervisedPOSTagger(POSOptions options0) {
		options = options0;
		parser = options.parser;
		setVariousOptions();
		createExecutionDirectory();
	}

	private void createExecutionDirectory() {
		long timeStamp = new Date().getTime();
		File dir = new File(execPoolDir + "/" + timeStamp);
		if (dir.exists()) {
			deleteDir(dir);
		}
		dir.mkdir();
		execPoolDir = dir.getAbsolutePath();
		log.info("Execution directory: " + execPoolDir);
	}

	public boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// The directory is now empty so delete it
		return dir.delete();
	}

	private void getHelperTransitionsTagDict(String[] paths) {
		System.out.println("Paths to transition matrices:");
		for (String path: paths) {
			System.out.println(path);
		}
		Set<String> vTags = AverageMultinomials.getValidTags(fineToCoarseMapFile);
		if (vTags.size() != indexToPOS.size()) {
			System.out.println("Problem. Valid tag array size:" + vTags.size() + " indexToPOS size:" + indexToPOS.size());
		}
		ArrayList<String> validTagList = new ArrayList<String>();
		for (int i = 0; i < indexToPOS.size(); i++) {
			validTagList.add(indexToPOS.get(i));
		}
		validTagList.add("END");
		validTagList.add("START");
		validTagArray = new String[validTagList.size()];
		validTagList.toArray(validTagArray);
		System.out.println("Valid tags from dictionary:");
		for (String tag: validTagArray) {
			System.out.println(tag);
		}
		String[] coarseTagArray = ComputeTransitionMultinomials.COARSE_TAGS;
		Arrays.sort(coarseTagArray);
		if (validTagArray.length - 2 != this.numTags) {
			System.out.println("Problem. Number of input tags: " + numTags + " unequal to valid tags: " + (validTagArray.length - 2));
		}
		System.out.println("Number of helper languages:" + numHelperLanguages);
		transitionMatrices = new double[numHelperLanguages][validTagArray.length][validTagArray.length];
		for (int i = 0; i < numHelperLanguages; i++) {
			double[][] gold =
				AverageMultinomials.readGoldMultinomials(paths[i], coarseTagArray.length, "language" + i);
				for (int j = 0; j < validTagArray.length; j++) {
				double sum = 0.0;
				Arrays.fill(transitionMatrices[i][j], 0);
				for (int k = 0; k < validTagArray.length; k++) {
					String from = validTagArray[j];
					String to = validTagArray[k];
					int frmIndex = Arrays.binarySearch(coarseTagArray, from);
					int toIndex = Arrays.binarySearch(coarseTagArray, to);
					transitionMatrices[i][j][k] = gold[frmIndex][toIndex];
					sum += transitionMatrices[i][j][k];
				}
				for (int k = 0; k < validTagArray.length; k++) {
					transitionMatrices[i][j][k] /= sum;
				}
			}
		}
	}

	private void readTagDictionary() {
		BufferedReader bReader = BasicFileIO.openFileToRead(tagDictionaryFile);
		String line = BasicFileIO.getLine(bReader);
		Map<Integer, int[]> tempDict = new HashMap<Integer, int[]>();
		Set<String> setOfTags = new HashSet<String>();
		while (line != null) {
			line = line.trim();
			String[] tabSeparatedToks = line.split("\t");
			String word = tabSeparatedToks[0];
			String[] tags = TabSeparatedFileReader.getToks(tabSeparatedToks[1]);
			for (String tag: tags) {
				setOfTags.add(tag);
			}
			int wordIndex = POSUtil.indexString(word, indexToWord, wordToIndex);
			line = BasicFileIO.getLine(bReader);
			int[] intTags = new int[tags.length];
			for (int i = 0; i < intTags.length; i++) {
				intTags[i] = POSUtil.indexString(tags[i], indexToPOS, posToIndex);
			}
			Arrays.sort(intTags);
			tempDict.put(wordIndex, intTags);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		int wordSize = indexToWord.size();
		tagDictionary = new int[wordSize][];
		for (int i = 0; i < tagDictionary.length; i ++) {
			if (!tempDict.containsKey(i)) {
				tagDictionary[i] = null;
			} else {
				tagDictionary[i] = tempDict.get(i);
			}
		}
		if (this.numTags != setOfTags.size()) {
			System.out.println("Problem. Input number of tags is not equal to setOfTags size:" + setOfTags.size());
		}
		int posSize = indexToPOS.size();
		tagsToClusters = new int[posSize][];
		for (int i = 0; i < posSize; i++) {
			tagsToClusters[i] = new int[1];
			tagsToClusters[i][0] = i;
		}
		// rewriting init transitions file
		if (initTransitionsFile != null) {
			System.out.println("Reading transition probabilities in the tag dictionary setting...");
			initTransProbs = readDictionaryBasedTransitionsFile(initTransitionsFile);
		}
		// reading helper transitions files
		if (useInterpolation) {
			System.out.println("Getting helper transitions using dictionary tags..");
			String[] paths = pathToHelperTransitions.split(",");
			getHelperTransitionsTagDict(paths);
		}
	}

	private double[][] readDictionaryBasedTransitionsFile(
			String initTransitionsFile2) {
		int countTags = 0;
		BufferedReader bReader = BasicFileIO.openFileToRead(initTransitionsFile);
		String line = BasicFileIO.getLine(bReader);
		double lineNum = 0;
		while (line != null) {
			lineNum++;
			line = BasicFileIO.getLine(bReader);
		}
		countTags = (int) Math.sqrt(lineNum);
		System.out.println("Number of tags from dictionary:" + indexToPOS.size() + " number of tags in trans file:" + countTags);
		BasicFileIO.closeFileAlreadyRead(bReader);
		double[][] arr = new double[countTags][countTags];
		bReader = BasicFileIO.openFileToRead(initTransitionsFile);
		line = BasicFileIO.getLine(bReader);
		int posSize = indexToPOS.size();
		while (line != null) {
			String[] toks = line.trim().split(" ");
			if (toks.length != 3) {
				System.out.println("Problem with init probs file. Line:" + line);
				System.exit(-1);
			}
			String first = toks[0];
			String second = toks[1];
			int firstIndex = -1;
			int secondIndex = -1;
			if (first.equals("START")) {
				firstIndex = posSize + 1;
			} else if (first.equals("END")) {
				firstIndex = posSize;
			} else {
				firstIndex = posToIndex.get(first);
			}
			if (second.equals("START")) {
				secondIndex = posSize + 1;
			} else if (second.equals("END")) {
				secondIndex = posSize;
			} else {
				secondIndex = posToIndex.get(second);
			}
			arr[firstIndex][secondIndex] = new Double(toks[2]);
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		return arr;
	}

	private void setVariousOptions() {
		trainOrTest = (String) parser.getOptionValue(options.trainOrTest);
		if (trainOrTest.equals("train")) {
			trainSet = (String) parser.getOptionValue(options.trainSet);
			useUnlabeledData = parser.getOptionValue(options.useUnlabeledData)
			== null ? false : true;
			if (useUnlabeledData) {
				unlabeledSet =
					(String) parser.getOptionValue(options.unlabeledSet);
				numUnLabeledSentences =
					(Integer) parser.getOptionValue(options.numUnLabeledSentences);
				useSameSetOfFeatures = parser.getOptionValue(options.useSameSetOfFeatures)
				== null ? false : true;
				if (useSameSetOfFeatures) {
					startWithTrainedSupervisedModel = parser.getOptionValue(options.startWithTrainedSupervisedModel)
					== null ? false : true;
					if (startWithTrainedSupervisedModel) {
						trainedSupervisedModelFile = (String) parser.getOptionValue(options.trainedSupervisedModel);
					}
				}
				gamma = (Double) parser.getOptionValue(options.gamma);
				useTagDictionary = parser.getOptionValue(options.useTagDictionary)
				== null ? false : true;
				if (useTagDictionary) {
					tagDictionaryFile = (String) parser.getOptionValue(options.tagDictionaryFile);
				}
			} else {
				useOnlyUnlabeledData = parser.getOptionValue(options.useOnlyUnlabeledData)
				== null ? false : true;
				if (useOnlyUnlabeledData) {
					regParametersModelFile = (String) parser.getOptionValue(options.regParametersModel);
					if (regParametersModelFile != null && !regParametersModelFile.equals("null")) {
						regParametersModel = (POSModel) BasicFileIO.readSerializedObject(regParametersModelFile);
					} else {
						regParametersModel = null;
						numTags = (Integer) parser.getOptionValue(options.numTags);
					}
					unlabeledSet =
						(String) parser.getOptionValue(options.unlabeledSet);
					numUnLabeledSentences =
						(Integer) parser.getOptionValue(options.numUnLabeledSentences);
					useTagDictionary = parser.getOptionValue(options.useTagDictionary)
					== null ? false : true;
					if (useTagDictionary) {
						tagDictionaryFile = (String) parser.getOptionValue(options.tagDictionaryFile);
					}
				}
			}
		} else {
			testSet = (String) parser.getOptionValue(options.testSet);
			useTagDictionary = parser.getOptionValue(options.useTagDictionary)
			== null ? false : true;
			if (useTagDictionary) {
				tagDictionaryFile = (String) parser.getOptionValue(options.tagDictionaryFile);
			}
			printPosteriors =  parser.getOptionValue(options.printPosteriors)
			== null ? false : true;
		}
		modelFile = (String) parser.getOptionValue(options.modelFile);
		runOutput = (String) parser.getOptionValue(options.runOutput);
		if (runOutput != null && !runOutput.equals("null")) {

		} else {
			runOutput = null;
		}
		if (!useOnlyUnlabeledData) {
			numLabeledSentences = (Integer) parser.getOptionValue(options.numLabeledSentences);
		}
		maxSentenceLength = (Integer)
		parser.getOptionValue(options.maxSentenceLength);
		iters = (Integer)
		parser.getOptionValue(options.iters);
		printRate = (Integer)
		parser.getOptionValue(options.printRate);
		useStandardMultinomialMStep =
			parser.getOptionValue(options.useStandardMultinomialMStep)
			== null ? false : true;
		Double standardMStepCountSmoothing0 = (Double)
		parser.getOptionValue(options.standardMStepCountSmoothing);
		if (standardMStepCountSmoothing0 == null) {
			standardMStepCountSmoothing = 0.0;
		} else {
			standardMStepCountSmoothing = standardMStepCountSmoothing0;
		}
		useGlobalForLabeledData = (Boolean)
		parser.getOptionValue(options.useGlobalForLabeledData)
		== null ? false : true;
		trainHMMDiscriminatively = (Boolean)
		parser.getOptionValue(options.trainHMMDiscriminatively)
		== null ? false : true;
		Double initialWeightsUpper0 = (Double)
		parser.getOptionValue(options.initialWeightsUpper);
		if (initialWeightsUpper0 == null) {
			initialWeightsUpper = 0.01;
		} else {
			initialWeightsUpper = initialWeightsUpper0;
		}
		Double initialWeightsLower0 = (Double)
		parser.getOptionValue(options.initialWeightsLower);
		if (initialWeightsLower0 == null) {
			initialWeightsLower = -0.01;
		} else {
			initialWeightsLower = initialWeightsLower0;
		}
		Double regularizationWeight0 = (Double)
		parser.getOptionValue(options.regularizationWeight);
		if (regularizationWeight0 == null) {
			regularizationWeight = 0.0;
		} else {
			regularizationWeight = regularizationWeight0;
		}
		Double regularizationBias0 = (Double)
		parser.getOptionValue(options.regularizationBias);
		if (regularizationBias0 == null) {
			regularizationBias = 0.0;
		} else {
			regularizationBias = regularizationBias0;
		}
		useStandardFeatures = (Boolean)
		parser.getOptionValue(options.useStandardFeatures)
		== null ? false : true;
		Integer lengthNGramSuffixFeature0 = (Integer)
		parser.getOptionValue(options.lengthNGramSuffixFeature);
		if (lengthNGramSuffixFeature0 == null) {
			lengthNGramSuffixFeature = 3;
		} else {
			lengthNGramSuffixFeature = lengthNGramSuffixFeature0;
		}
		useBiasFeature = (Boolean)
		parser.getOptionValue(options.useBiasFeature)
		== null ? false : true;
		if (useBiasFeature) {
			biasFeatureBias = (Double)
			parser.getOptionValue(options.biasFeatureBias);
			biasFeatureRegularizationWeight = (Double)
			parser.getOptionValue(options.biasFeatureRegularizationWeight);
		}
		Integer randSeedIndex0 = (Integer)
		parser.getOptionValue(options.randSeedIndex);
		if (randSeedIndex0 == null) {
			randSeedIndex0 = 1;
		} else {
			randSeedIndex = randSeedIndex0;
		}
		execPoolDir = (String) parser.getOptionValue(options.execPoolDir);
		restartTraining = (Boolean) parser.getOptionValue(options.restartTraining)
		== null ? false : true;
		if (restartTraining) {
			restartModelFile = (String) parser.getOptionValue(options.restartModelFile);
		}
		useStackedFeatures = (Boolean)
		parser.getOptionValue(options.useStackedFeatures) == null ? false : true;
		if (useStackedFeatures) {
			stackedFile = (String) parser.getOptionValue(options.stackedFile);
		}
		noahsFeaturesFile = (String) parser.getOptionValue(options.noahsFeaturesFile);
		System.out.println("Noahs feature file:" + noahsFeaturesFile);
		if (noahsFeaturesFile != null && !noahsFeaturesFile.equals("null")) {
			noahsFeatures = getNoahsFeatures(noahsFeaturesFile);
		} else {
			noahsFeatures = null;
		}
		initTransitionsFile = (String) parser.getOptionValue(options.initTransitionsFile);
		System.out.println("File with initial transition probs:" + initTransitionsFile);
		if (initTransitionsFile != null && !initTransitionsFile.equals("null") && !useTagDictionary) {
			initTransProbs = getInitTransProbs(initTransitionsFile);
		} else {
			initTransProbs = null;
		}
		useDistSim = (Boolean)
		parser.getOptionValue(options.useDistSim)
		== null ? false : true;
		if (useDistSim) {
			distSimTable = DIST_SIM;
		} else {
			distSimTable = null;
		}
		useNames = (Boolean)
		parser.getOptionValue(options.useNames)
		== null ? false : true;
		if (useNames) {
			namesArray = NAMES;
		} else {
			namesArray = null;
		}
		useInterpolation = (Boolean)
		parser.getOptionValue(options.useInterpolation)
		== null ? false : true;
		if (useInterpolation) {
			pathToHelperTransitions = (String) parser.getOptionValue(options.pathToHelperTransitions);
			fineToCoarseMapFile = (String) parser.getOptionValue(options.fineToCoarseMapFile);
			String[] paths = pathToHelperTransitions.split(",");
			numHelperLanguages = paths.length;
			if (!useTagDictionary) {
				getHelperTransitions(paths);
			} else {
				System.out.println("Using tag dictionary, hence not reading helper matrices now.");
			}
		}
	}

	private void getHelperTransitions(String[] paths) {
		System.out.println("Paths to transition matrices:");
		for (String path: paths) {
			System.out.println(path);
		}
		Set<String> validTags = AverageMultinomials.getValidTags(fineToCoarseMapFile);
		String[] temp = new String[validTags.size()];
		validTags.toArray(temp);
		Arrays.sort(temp);
		ArrayList<String> validTagList = new ArrayList<String>();
		for (String str: temp) {
			validTagList.add(str);
		}
		validTagList.add("END");
		validTagList.add("START");
		validTagArray = new String[validTagList.size()];
		validTagList.toArray(validTagArray);
		System.out.println("Valid tags:");
		for (String tag: validTagArray) {
			System.out.println(tag);
		}
		String[] coarseTagArray = ComputeTransitionMultinomials.COARSE_TAGS;
		Arrays.sort(coarseTagArray);
		if (validTagArray.length - 2 != this.numTags) {
			System.out.println("Problem. Number of input tags: " + numTags + " unequal to valid tags: " + (validTagArray.length - 2));
		}
		System.out.println("Number of helper languages:" + numHelperLanguages);
		transitionMatrices = new double[numHelperLanguages][validTagArray.length][validTagArray.length];
		for (int i = 0; i < numHelperLanguages; i++) {
			double[][] gold =
				AverageMultinomials.readGoldMultinomials(paths[i], coarseTagArray.length, "language" + i);
			for (int j = 0; j < validTagArray.length; j++) {
				double sum = 0.0;
				Arrays.fill(transitionMatrices[i][j], 0);
				for (int k = 0; k < validTagArray.length; k++) {
					String from = validTagArray[j];
					String to = validTagArray[k];
					int frmIndex = Arrays.binarySearch(coarseTagArray, from);
					int toIndex = Arrays.binarySearch(coarseTagArray, to);
					transitionMatrices[i][j][k] = gold[frmIndex][toIndex];
					sum += transitionMatrices[i][j][k];
				}
				for (int k = 0; k < validTagArray.length; k++) {
					transitionMatrices[i][j][k] /= sum;
				}
			}
		}
	}
	private static String[] initNames() {
		// System.out.println("Reading NAMES file...");
		BufferedReader bReader =
			new BufferedReader(new InputStreamReader(
                    SemiSupervisedPOSTagger.class.getResourceAsStream("names"),
                    Charset.forName("UTF-8")));
		String line = BasicFileIO.getLine(bReader);
		ArrayList<String> namesList = new ArrayList<String>();
		while (line != null) {
			line = line.trim();
			namesList.add(line);
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		String[] arr = new String[namesList.size()];
		namesList.toArray(arr);
		Arrays.sort(arr);
		return NAMES;
	}

	private static Map<String, double[]> initDistSim() {
		BufferedReader bReader =
			new BufferedReader(new InputStreamReader(
                    SemiSupervisedPOSTagger.class.getResourceAsStream("embeddings.txt"),
                    Charset.forName("UTF-8")));
		String line = BasicFileIO.getLine(bReader);
		Map<String, double[]> map = new HashMap<String, double[]>();
		while (line != null) {
			String[] toks = line.trim().split("\t");
			String word = toks[0];
			ArrayList<String> dists =
				ProjectAlignedTags.getTokens(toks[1].trim());
			double[] arr = new double[dists.size()];
			for (int i = 0; i < dists.size(); i++) {
				arr[i] = new Double(dists.get(i));
			}
			map.put(word, arr);
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		return Collections.unmodifiableMap(map);
	}



	private double[][] getInitTransProbs(String initTransitionsFile) {
		int countTags = 0;
		BufferedReader bReader = BasicFileIO.openFileToRead(initTransitionsFile);
		String line = BasicFileIO.getLine(bReader);
		while (line != null) {
			countTags++;
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		double[][] arr = new double[countTags][countTags];
		bReader = BasicFileIO.openFileToRead(initTransitionsFile);
		line = BasicFileIO.getLine(bReader);
		int count = 0;
		while (line != null) {
			String[] toks = line.trim().split(" ");
			if (toks.length != countTags) {
				System.out.println("Problem with init probs file. Line:" + line);
				System.exit(-1);
			}
			for (int j = 0; j < toks.length; j++) {
				arr[count][j] += new Double(toks[j]);
			}
			count++;
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		return arr;
	}

	private Map<String, String> getNoahsFeatures(String noahsFeaturesFile) {
		Map<String, String> map = new HashMap<String, String>();
		BufferedReader bReader = BasicFileIO.openFileToRead(noahsFeaturesFile);
		String line = BasicFileIO.getLine(bReader);
		while (line != null) {
			String[] toks = line.trim().split("\t");
			map.put(toks[0], toks[1]);
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		return map;
	}

	private void readClusterToTagMappingFile() {

	}

	private void logInputInfo() {
		log.info("Use standard multinomial MStep: " + useStandardMultinomialMStep);
		log.info("Use global: " + useGlobalForLabeledData);
		log.info("Use standard features: " + useStandardFeatures);
		log.info("Num gold labels: " +  indexToPOS.size());
		log.info("Num word types: " + indexToWord.size());
		log.info("Regularization weight: " + regularizationWeight);
		log.info("Regularization bias: " + regularizationBias);
		log.info("Use bias feature: " + useBiasFeature);
		if (useBiasFeature) {
			log.info("Bias feature bias: " + biasFeatureBias);
			log.info("Bias feature regularization weight: " +
					biasFeatureRegularizationWeight);
		}
		log.info("Gold POS labels: " + indexToPOS.toString());
		log.info("Initial random weights lower: " +
				initialWeightsLower);
		log.info("Initial random weights upper: " +
				initialWeightsUpper);
		if (useUnlabeledData) {
			if (useSameSetOfFeatures) {
				log.info("Using same set of features in CRF and HMM");
				if (startWithTrainedSupervisedModel) {
					log.info("Starting with a trained supervised model: "+ trainedSupervisedModelFile);
				}
			} else {
				log.info("Not using same set of features in CRF and HMM");
			}
		}
		if (!useGlobalForLabeledData) {
			if (trainHMMDiscriminatively) {
				log.info("Training the HMM in a discriminative fashion.");
			}
		}
		if (useStackedFeatures) {
			log.info("Using stacked features");
			log.info("Stacked tag file:" + stackedFile);
		}
	}


	public void initializeDataStructures() {
		featureIndexCounts = new ArrayList<Integer>();
		indexToPOS = new ArrayList<String>();
		indexToWord = new ArrayList<String>();
		indexToFeature = new ArrayList<String>();
		posToIndex = new HashMap<String, Integer>();
		wordToIndex = new HashMap<String, Integer>();
		featureToIndex = new HashMap<String, Integer>();
	}

	public void run() {
		initializeDataStructures();
		log.info("Train or test condition:" + trainOrTest);
		if (trainOrTest.equals("train")) {
			train();
		} else {
			test();
		}
	}

	private void logObservationInfo() {
		int[][] observations = null;
		if (!useOnlyUnlabeledData) {
			observations = lObservations;
		} else {
			observations = uObservations;
		}
		int numTokens = 0;
		int maxSeqLength = 0;
		for (int s=0; s < observations.length; ++s) {
			numTokens += observations[s].length;
			maxSeqLength = Math.max(maxSeqLength, observations[s].length);
		}

		if (useUnlabeledData) {
			for (int s=0; s < uObservations.length; ++s) {
				numTokens += uObservations[s].length;
				maxSeqLength = Math.max(maxSeqLength, uObservations[s].length);
			}
		}

		log.info("Number of total tokens: " + numTokens);
		log.info("Maximum sequence length: " + maxSeqLength);
		log.info("Size of primary observations:" + observations.length);
		if (useUnlabeledData) {
			log.info("Size of unlabeled observations:" + uObservations.length);
		}
	}

	public List<Pair<Integer,Double>>[][] getActiveCRFTransFeatures(
			List<TransFeatureTemplate> templates,
			int numObservations,
			int numLabels) {
		List<Pair<Integer,Double>>[][] activeFeatures = new List[numLabels][numLabels];
		for (int s0=0; s0<numLabels; ++s0) {
			for (int s1=0; s1<numLabels; ++s1) {
				activeFeatures[s0][s1] = new ArrayList<Pair<Integer,Double>>();
				for (TransFeatureTemplate template : templates) {
					List<Pair<String, Double>> features = template.getFeatures(s0, s1);
					for (Pair<String, Double> feature : features) {
						int index = POSUtil.indexString(feature.getFirst(),
								indexToFeature,
								featureToIndex);
						activeFeatures[s0][s1].add(Pair.makePair(index, feature.getSecond()));
					}
				}
			}
		}
		return activeFeatures;
	}

	public Pair<Integer,Double>[][] getActiveInterpolationFeatures(
			InterpolationFeatureTemplate template,
			int numHelperLanguages,
			int numLabels,
			int startLabel,
			int stopLabel) {
		Pair<Integer,Double>[][] activeFeatures = new Pair[numHelperLanguages][numLabels];
		for (int l = 0; l < numHelperLanguages; l++) {
			for (int s0=0; s0<numLabels; ++s0) {
				Pair<String, Double> feature = template.getFeature(l, s0);
				int index = POSUtil.indexString(feature.getFirst(),
						indexToFeature,
						featureToIndex);
				if (index >= featureIndexCounts.size()) {
					featureIndexCounts.add(1);
				} else {
					featureIndexCounts.set(index, featureIndexCounts.get(index)+1);
				}
				activeFeatures[l][s0] = Pair.makePair(index, feature.getSecond());
			}
		}
		return activeFeatures;
	}

	public List<Pair<Integer,Double>>[][] getActiveTransFeatures(
			List<TransFeatureTemplate> templates,
			int numObservations,
			int numLabels,
			int startLabel,
			int stopLabel) {
		List<Pair<Integer,Double>>[][] activeFeatures = new List[numLabels][numLabels];
		for (int s0=0; s0<numLabels; ++s0) {
			if (s0 != stopLabel) {
				for (int s1=0; s1<numLabels; ++s1) {
					if (s1 != startLabel) {
						activeFeatures[s0][s1] = new ArrayList<Pair<Integer,Double>>();
						for (TransFeatureTemplate template : templates) {
							List<Pair<String, Double>> features = template.getFeatures(s0, s1);
							for (Pair<String, Double> feature : features) {
								int index = POSUtil.indexString(feature.getFirst(),
										indexToFeature,
										featureToIndex);
								if (index >= featureIndexCounts.size()) {
									featureIndexCounts.add(1);
								} else {
									featureIndexCounts.set(index, featureIndexCounts.get(index)+1);
								}
								activeFeatures[s0][s1].add(Pair.makePair(index, feature.getSecond()));
							}
						}
					}
				}
			}
		}
		return activeFeatures;
	}

	public List<Pair<Integer,Double>>[][] getActiveCRFEmitFeatures(
			List<EmitFeatureTemplate> templates,
			int numObservations,
			int numLabels) {

		List<Pair<Integer,Double>>[][] activeFeatures = new List[numLabels][numObservations];
		System.out.println(""+numLabels+"  "+numObservations+"\n");
		for (int s=0; s<numLabels; ++s) {
			for (int i=0; i<numObservations; ++i) {
				activeFeatures[s][i] = new ArrayList<Pair<Integer,Double>>();
				for (EmitFeatureTemplate template : templates) {
					List<Pair<String, Double>> features = template.getFeatures(s, indexToWord.get(i));
					for (Pair<String, Double> feature : features) {
						int index = POSUtil.indexString(feature.getFirst(),
								indexToFeature, featureToIndex);
						if (index >= featureIndexCounts.size()) {
							featureIndexCounts.add(1);
						} else {
							featureIndexCounts.set(index, featureIndexCounts.get(index)+1);
						}
						activeFeatures[s][i].add(Pair.makePair(index, feature.getSecond()));
					}
				}
			}
		}
		return activeFeatures;
	}


	public List<Pair<Integer,Double>>[][] getActiveEmitFeatures(List<EmitFeatureTemplate> templates, int numObservations, int numLabels, int startLabel, int stopLabel) {
		List<Pair<Integer,Double>>[][] activeFeatures = new List[numLabels][numObservations];
		for (int s=0; s<numLabels; ++s) {
			if (s != startLabel && s != stopLabel) {
				for (int i=0; i<numObservations; ++i) {
					activeFeatures[s][i] = new ArrayList<Pair<Integer,Double>>();
					for (EmitFeatureTemplate template : templates) {
						List<Pair<String, Double>> features = template.getFeatures(s, indexToWord.get(i));
						for (Pair<String, Double> feature : features) {
							int index = POSUtil.indexString(feature.getFirst(),
									indexToFeature, featureToIndex);
							if (index >= featureIndexCounts.size()) {
								featureIndexCounts.add(1);
							} else {
								featureIndexCounts.set(index, featureIndexCounts.get(index)+1);
							}
							activeFeatures[s][i].add(Pair.makePair(index, feature.getSecond()));
						}
					}
				}
			}
		}
		return activeFeatures;
	}

	public List<Pair<Integer, Double>>[][] getActiveStackedFeatures(int numObservations, int numLabels, int startLabel, int stopLabel) {
		List<Pair<Integer,Double>>[][] activeFeatures = new List[numLabels][indexToPOS.size()];
		POSFeatureTemplates templates1 = new POSFeatureTemplates();
		EmitFeatureTemplate template =
			templates1.new StackedIndicatorFeature(useTagDictionary,
					wordToIndex,
					tagDictionary,
					tagsToClusters);
		for (int s=0; s<numLabels; ++s) {
			if (s != startLabel && s != stopLabel) {
				for (int i = 0; i < indexToPOS.size(); i ++) {
					activeFeatures[s][i] = new ArrayList<Pair<Integer,Double>>();
					List<Pair<String, Double>> features = template.getFeatures(s, indexToPOS.get(i));
					for (Pair<String, Double> feature : features) {
						int index = POSUtil.indexString(feature.getFirst(),
								indexToFeature, featureToIndex);
						if (index >= featureIndexCounts.size()) {
							featureIndexCounts.add(1);
						} else {
							featureIndexCounts.set(index, featureIndexCounts.get(index)+1);
						}
						activeFeatures[s][i].add(Pair.makePair(index, feature.getSecond()));
					}
				}
			}
		}
		return activeFeatures;
	}

//	modified this to fit the gaussian prior story
	public double[] getRegularizationWeights(List<TransFeatureTemplate> transFeatures, List<EmitFeatureTemplate> emitFeatures) {
		double[] regularizationWeights = new double[indexToFeature.size()];
		for (int f = 0; f<indexToFeature.size(); ++f) {
			if (indexToFeature.get(f).startsWith("bias")) {
				regularizationWeights[f] = biasFeatureRegularizationWeight;
			} else {
				regularizationWeights[f] = regularizationWeight;
			}
		}
		return regularizationWeights;
	}

	public double[] getRegularizationWeights() {
		double[] regularizationWeights = new double[indexToFeature.size()];
		for (int f = 0; f<indexToFeature.size(); ++f) {
			if (indexToFeature.get(f).startsWith("bias")) {
				regularizationWeights[f] = biasFeatureRegularizationWeight;
			} else {
				regularizationWeights[f] = regularizationWeight;
			}
		}
		return regularizationWeights;
	}

	public double[] getSpecialRegularizationBiases() {
		double[] regularizationBiases = new double[indexToFeature.size()];
		for (int f = 0; f<indexToFeature.size(); ++f) {
			if (indexToFeature.get(f).startsWith("bias")) {
				regularizationBiases[f] = biasFeatureBias;
			} else {
				String featName = indexToFeature.get(f);
				if (regParametersModel!=null && regParametersModel.getFeatureToIndex().containsKey(featName)) {
					int index = regParametersModel.getFeatureToIndex().get(featName);
					regularizationBiases[f] = regParametersModel.getWeights()[index];
				} else {
					regularizationBiases[f] = 0.0;
				}
			}
		}
		return regularizationBiases;
	}

	public double[] getSpecialRegularizationBiases(
			List<TransFeatureTemplate> transFeatures,
			List<EmitFeatureTemplate> emitFeatures,
			int numLabels,
			int startLabel,
			int stopLabel) {
		double[] regularizationBiases = new double[indexToFeature.size()];
		for (int f = 0; f<indexToFeature.size(); ++f) {
			if (indexToFeature.get(f).startsWith("bias")) {
				regularizationBiases[f] = biasFeatureBias;
			} else {
				String featName = indexToFeature.get(f);
				if (regParametersModel!=null && regParametersModel.getFeatureToIndex().containsKey(featName)) {
					int index = regParametersModel.getFeatureToIndex().get(featName);
					regularizationBiases[f] = regParametersModel.getWeights()[index];
				} else {
					regularizationBiases[f] = 0.0;
				}
			}
		}
		return regularizationBiases;
	}

	public double[] getRegularizationBiases(List<TransFeatureTemplate> transFeatures, List<EmitFeatureTemplate> emitFeatures, int numLabels, int startLabel, int stopLabel) {
		double[] regularizationBiases = new double[indexToFeature.size()];
		for (int f = 0; f<indexToFeature.size(); ++f) {
			if (indexToFeature.get(f).startsWith("bias")) {
				regularizationBiases[f] = biasFeatureBias;
			} else {
				regularizationBiases[f] = regularizationBias;
			}
		}
		return regularizationBiases;
	}

	public double[] uniformRandomWeights(int dim, double lower, double upper, Random rand) {
		double range = upper - lower;
		double[] weights = new double[dim];
		for (int i=0; i<dim; ++i) {
			double randVal = rand.nextDouble();
			weights[i] = lower + (range*randVal);
		}
		return weights;
	}

	public void train() {

		if(!useOnlyUnlabeledData) {
			log.info("Labeled training set:" + trainSet);
			sequences =
				TabSeparatedFileReader.readPOSSeqences(trainSet,
						numLabeledSentences,
						maxSentenceLength);
			if (useUnlabeledData) {
				if (startWithTrainedSupervisedModel) {
					trainedSupervisedModel = (POSModel)
					BasicFileIO.readSerializedObject(trainedSupervisedModelFile);
					featureIndexCounts = trainedSupervisedModel.getFeatureIndexCounts();
					featureToIndex = trainedSupervisedModel.getFeatureToIndex();
					indexToFeature = trainedSupervisedModel.getIndexToFeature();
					indexToPOS = trainedSupervisedModel.getIndexToPOS();
					posToIndex = trainedSupervisedModel.getPosToIndex();
				}
			}
			Pair<int[][], int[][]> pairList = POSUtil.getObservationsAndGoldLabels(
					sequences,
					indexToWord,
					wordToIndex,
					indexToPOS,
					posToIndex);
			lObservations = pairList.getFirst();
			goldLabels = pairList.getSecond();
			/*
			 * account for unlabeled data
			 */
			if (useUnlabeledData) {
				uSequences =
					UnlabeledSentencesReader.readSequences(unlabeledSet,
							this.numUnLabeledSentences,
							this.maxSentenceLength);
				uObservations =
					POSUtil.getObservationsFromUnlabeledSet(uSequences,
							indexToWord,
							wordToIndex);
			}
			logObservationInfo();
			logInputInfo();
		} else { //fully unlabeled data
			log.info("Totally unlabeled training set:" + unlabeledSet);
			uSequences =
				UnlabeledSentencesReader.readSequences(unlabeledSet,
						this.numUnLabeledSentences,
						this.maxSentenceLength);
			uObservations =
				POSUtil.getObservationsFromUnlabeledSet(uSequences,
						indexToWord,
						wordToIndex);
			if (regParametersModel == null) {
				if (numTags == 0) {
					log.severe("Number of tags is zero. Exiting.");
					System.exit(-1);
				} else {
					if (useTagDictionary) {
						System.out.println("Reading tag dictionary...");
						readTagDictionary();
					} else {
						for (int i = 0; i < numTags; i++) {
							indexToPOS.add("T"+i);
							posToIndex.put("T"+i, i);
						}
					}
				}
			} else {
				indexToPOS = regParametersModel.getIndexToPOS();
				posToIndex = regParametersModel.getPosToIndex();
			}
			logObservationInfo();
			logInputInfo();
		}

		int numStackedSentences = 0;
		if (useOnlyUnlabeledData) {
			numStackedSentences = numUnLabeledSentences;
		} else {
			numStackedSentences = numLabeledSentences;
		}

		if (useStackedFeatures) {
			Collection<Pair<List<String>, List<String>>> stackedSequences
			= TabSeparatedFileReader.readPOSSeqences(stackedFile,
					numStackedSentences,
					maxSentenceLength);
			Pair<int[][], int[][]> pairList = POSUtil.getObservationsAndGoldLabels(
					stackedSequences,
					indexToWord,
					wordToIndex,
					indexToPOS,
					posToIndex);
			stackedTags = pairList.getSecond();
			for (int i = 0; i < stackedTags.length; i ++) {
				if (useOnlyUnlabeledData) {
					if (stackedTags[i].length != uObservations[i].length) {
						log.severe("Problem with length of sentence:" + i);
						System.exit(-1);
					}
				} else {
					if (stackedTags[i].length != lObservations[i].length) {
						log.severe("Problem with length of sentence:" + i);
						System.exit(-1);
					}
				}
			}
		}

		// Get feature templates
		List<TransFeatureTemplate> transFeatures = null;
		InterpolationFeatureTemplate interpolationFeature =
			null;
		if (!useInterpolation) {
			transFeatures =
				POSFeatureTemplates.getTransFeatures(useBiasFeature);
		} else { // interpolation of existing multinomials
			interpolationFeature =
				POSFeatureTemplates.getInterpolationFeatures(numHelperLanguages);
		}
		List<EmitFeatureTemplate> emitFeatures =
			POSFeatureTemplates.getEmitFeatures(useStandardFeatures,
					lengthNGramSuffixFeature,
					useTagDictionary, wordToIndex,
					tagDictionary,
					tagsToClusters,
					noahsFeatures,
					distSimTable,
					namesArray);
		FileWriter curveOut = null;
		try {
			curveOut = new FileWriter(execPoolDir + "/" + "curve");
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Do gradient ascent

		int numLabels = indexToPOS.size();

		// checking if we are using only unlabeled data
		if (useOnlyUnlabeledData) {
			if (!useInterpolation) {
				trainUnsupervisedFeatureHMM(numLabels,
						transFeatures,
						emitFeatures,
						curveOut);
			} else {
				trainUnsupervisedFeatureHMMInterpolation(numLabels,
						interpolationFeature,
						emitFeatures,
						curveOut);
			}
		} else if (!useUnlabeledData) { // a supervised model
			if (!useGlobalForLabeledData) {
				if (!trainHMMDiscriminatively)  {
					trainSupervisedFeatureHMM(numLabels,
							transFeatures,
							emitFeatures,
							curveOut);
				} else {
					trainSupervisedFeatureHMMDiscriminatively(numLabels,
							transFeatures,
							emitFeatures,
							curveOut);
				}
			} else {
				trainCRF(numLabels,
						transFeatures,
						emitFeatures,
						curveOut);
			}
		} else { // a model with a discriminative as well as generative objective
			trainSemiSupervisedModel(numLabels,
					transFeatures,
					emitFeatures,
					curveOut);
		}
	}

	public void trainSemiSupervisedModel(int numLabels,
			List<TransFeatureTemplate> transFeatures,
			List<EmitFeatureTemplate> emitFeatures,
			FileWriter curveOut) {

		int stopLabel = numLabels;
		int startLabel = numLabels + 1;

		/*
		 * getting active features
		 */
		log.info("Caching features...");
		log.info("Caching transition features...");
		// these should ideally be the same as
		// the CRF model, hence
		// we need to cache these features only once
		activeTransFeatures =
			getActiveTransFeatures(transFeatures,
					indexToWord.size(),
					numLabels + 2,
					startLabel,
					stopLabel);
		log.info("Caching emission features...");
		activeEmitFeatures =
			getActiveEmitFeatures(emitFeatures,
					indexToWord.size(),
					numLabels + 2,
					startLabel,
					stopLabel);
		if (useStackedFeatures) {
			activeStackedFeatures =
				getActiveStackedFeatures(indexToWord.size(),
						numLabels + 2,
						startLabel,
						stopLabel);
		}

		VertexFeatureExtractor vertexExtractor =
			new VertexFeatureExtractor(activeEmitFeatures);
		EdgeFeatureExtractor edgeExtractor =
			new EdgeFeatureExtractor(activeTransFeatures);

		double sigma = regularizationWeight;
		regularizationWeight = 0.0;

		SemiSupervisedCRFHMMModel grad =
			new SemiSupervisedCRFHMMModel(
					lObservations,
					goldLabels,
					uObservations,
					numLabels,
					indexToWord.size(),
					sigma,
					vertexExtractor,
					edgeExtractor,
					indexToFeature.size(),
					gamma,
					printPosteriors);
		GradientSequenceModel hmmModel =
			grad.getHMMModel();

		double[] regularizationWeights =
			getRegularizationWeights(transFeatures, emitFeatures);
		double[] regularizationBiases =
			getRegularizationBiases(transFeatures,
					emitFeatures,
					hmmModel.getNumLabels(),
					hmmModel.getStartLabel(),
					hmmModel.getStopLabel());
		hmmModel.setActiveFeatures(activeTransFeatures,
				activeEmitFeatures,
				activeStackedFeatures,
				indexToFeature.size(),
				regularizationWeights,
				regularizationBiases);

		double[] initialWeights = new double[indexToFeature.size()];
		if (startWithTrainedSupervisedModel) {
			double[] trainedWeights = trainedSupervisedModel.getWeights();
			Arrays.fill(initialWeights, 0);
			for (int i = 0; i < trainedWeights.length; i ++) {
				initialWeights[i] = trainedWeights[i];
			}
		} else {
			for (int i = 0; i < initialWeights.length; i ++) {
				initialWeights[i] = 0.0;
			}
			if (useBiasFeature) {
				initialWeights[featureToIndex.get("bias")] = biasFeatureBias;
			}
		}

		POSModel model = new POSModel();
		model.setFeatureIndexCounts(featureIndexCounts);
		model.setFeatureToIndex(featureToIndex);
		model.setIndexToFeature(indexToFeature);
		model.setIndexToPOS(indexToPOS);
		model.setPosToIndex(posToIndex);
		PrintLikelihoodCallbackCRF crfCallBack =
			new PrintLikelihoodCallbackCRF(model, curveOut);
		log.info("Training with LBFGS");
		double[] w = LBFGSOptimizer.optimize(grad,
				initialWeights,
				crfCallBack,
				iters);
		log.info("Finished training with LBFGS");
		model.setWeights(w);
		BasicFileIO.writeSerializedObject(modelFile, model);
	}

	public void trainUnsupervisedFeatureHMMInterpolation(
			int numLabels,
			InterpolationFeatureTemplate interpolationFeature,
			List<EmitFeatureTemplate> emitFeatures,
			FileWriter curveOut) {
		GradientSequenceModel grad =
			new GradientSequenceInterpolatedModel(uObservations,
					numLabels,
					indexToWord.size(),
					transitionMatrices,
					printPosteriors);
		// Cache active features
		activeConvexCombiners =
			this.getActiveInterpolationFeatures(interpolationFeature,
					numHelperLanguages, grad.getNumLabels(),
					grad.getStartLabel(),
					grad.getStopLabel());

		activeEmitFeatures =
			getActiveEmitFeatures(emitFeatures,
					grad.getNumObservations(),
					grad.getNumLabels(),
					grad.getStartLabel(),
					grad.getStopLabel());

		if (useStackedFeatures) {
			activeStackedFeatures =
				getActiveStackedFeatures(grad.getNumObservations(),
						grad.getNumLabels(),
						grad.getStartLabel(),
						grad.getStopLabel());
		}

		log.info("Num features: " + indexToFeature.size());
		double[] regularizationWeights =
			getRegularizationWeights();
		double[] regularizationBiases =
			getSpecialRegularizationBiases();
		grad.setActiveFeatures(activeConvexCombiners,
				activeEmitFeatures,
				activeStackedFeatures,
				indexToFeature.size(),
				regularizationWeights,
				regularizationBiases);
		// Initialize weights to 0.0;
		double[] initialWeights;
		if (regParametersModel != null) {
			initialWeights = uniformRandomWeights(indexToFeature.size(), initialWeightsLower, initialWeightsUpper, rands[0]);
			initialWeights = initializeFeatureWeightsWithTrainedValues(initialWeights);
		} else {
			initialWeights = uniformRandomWeights(indexToFeature.size(), initialWeightsLower, initialWeightsUpper, rands[0]);
			if (initTransProbs != null) {
				initialWeights =
					initializeFeatureWeightsWithInitialTransitionProbs(initialWeights, grad);
			}
		}
		if (useBiasFeature) {
			initialWeights[featureToIndex.get("bias")] = biasFeatureBias;
		}
		grad.setWeights(initialWeights);
		POSModel model = new POSModel();
		model.setFeatureIndexCounts(featureIndexCounts);
		model.setFeatureToIndex(featureToIndex);
		model.setIndexToFeature(indexToFeature);
		model.setIndexToPOS(indexToPOS);
		model.setPosToIndex(posToIndex);
		PrintLikelihoodCallbackCRF crfCallBack =
			new PrintLikelihoodCallbackCRF(model, curveOut);
		log.info("Training with LBFGS");
		double[] w = LBFGSOptimizer.optimize(grad,
				initialWeights,
				crfCallBack,
				iters);
		log.info("Finished training with LBFGS");
		model.setWeights(w);
		BasicFileIO.writeSerializedObject(modelFile, model);
	}

	public void trainUnsupervisedFeatureHMM(
			int numLabels,
			List<TransFeatureTemplate> transFeatures,
			List<EmitFeatureTemplate> emitFeatures,
			FileWriter curveOut) {
		GradientSequenceModel grad =
			new GradientGenSequenceModel(uObservations,
					stackedTags,
					numLabels, indexToWord.size(),
					printPosteriors);
		// Cache active features
		activeTransFeatures =
			getActiveTransFeatures(transFeatures,
					grad.getNumObservations(),
					grad.getNumLabels(),
					grad.getStartLabel(),
					grad.getStopLabel());

		activeEmitFeatures =
			getActiveEmitFeatures(emitFeatures,
					grad.getNumObservations(),
					grad.getNumLabels(),
					grad.getStartLabel(),
					grad.getStopLabel());
		if (useStackedFeatures) {
			activeStackedFeatures =
				getActiveStackedFeatures(grad.getNumObservations(),
						grad.getNumLabels(),
						grad.getStartLabel(),
						grad.getStopLabel());
		}

		log.info("Num features: " + indexToFeature.size());
		double[] regularizationWeights =
			getRegularizationWeights(transFeatures, emitFeatures);
		double[] regularizationBiases =
			getSpecialRegularizationBiases(transFeatures,
					emitFeatures,
					grad.getNumLabels(),
					grad.getStartLabel(),
					grad.getStopLabel());
		grad.setActiveFeatures(activeTransFeatures,
				activeEmitFeatures,
				activeStackedFeatures,
				indexToFeature.size(),
				regularizationWeights,
				regularizationBiases);
		// Initialize weights to 0.0;
		double[] initialWeights;
		if (regParametersModel != null) {
			initialWeights = uniformRandomWeights(indexToFeature.size(), initialWeightsLower, initialWeightsUpper, rands[0]);
			initialWeights = initializeFeatureWeightsWithTrainedValues(initialWeights);
		} else {
			initialWeights = uniformRandomWeights(indexToFeature.size(), initialWeightsLower, initialWeightsUpper, rands[0]);
			if (initTransProbs != null) {
				initialWeights =
					initializeFeatureWeightsWithInitialTransitionProbs(initialWeights, grad);
			}
		}
		if (useBiasFeature) {
			initialWeights[featureToIndex.get("bias")] = biasFeatureBias;
		}
		grad.setWeights(initialWeights);
		POSModel model = new POSModel();
		model.setFeatureIndexCounts(featureIndexCounts);
		model.setFeatureToIndex(featureToIndex);
		model.setIndexToFeature(indexToFeature);
		model.setIndexToPOS(indexToPOS);
		model.setPosToIndex(posToIndex);
		PrintLikelihoodCallbackCRF crfCallBack =
			new PrintLikelihoodCallbackCRF(model, curveOut);
		log.info("Training with LBFGS");
		double[] w = LBFGSOptimizer.optimize(grad,
				initialWeights,
				crfCallBack,
				iters);
		log.info("Finished training with LBFGS");
		model.setWeights(w);
		BasicFileIO.writeSerializedObject(modelFile, model);
	}

	private double[] initializeFeatureWeightsWithInitialTransitionProbs(
			double[] initialWeights,
			GradientSequenceModel grad) {
		System.out.println("Reading initial transition proababilities...");
		int numLabels = grad.getNumLabels();
		int startLabel = grad.getStartLabel();
		int stopLabel = grad.getStopLabel();
		if (numLabels != initTransProbs.length) {
			System.out.println("Problem. " +
			"Number of input labels and the number of labels in the transition matrix unequal.");
			System.exit(-1);
		}
		int featLen = indexToFeature.size();
		for (int i = 0; i < featLen; i++) {
			String feat = indexToFeature.get(i);
			if (!feat.startsWith("tind")) {
				continue;
			}
			String[] toks = feat.trim().split("\\|");
			int label1 = new Integer(toks[toks.length-2]);
			int label2 = new Integer(toks[toks.length-1]);
			int index1 = label1;
			int index2 = label2;
			if (label1 == startLabel) {
				index1 = initTransProbs.length - 1;
			} else if (label1 == stopLabel) {
				index1 = initTransProbs.length - 2;
			}
			if (label2 == startLabel) {
				index2 = initTransProbs.length - 1;
			} else if (label2 == stopLabel) {
				index2 = initTransProbs.length - 2;
			}
			double prob = initTransProbs[index1][index2];
			initialWeights[i] = Math.log(prob);
		}
		return initialWeights;
	}

	private double[] initializeFeatureWeightsWithTrainedValues(double[] initialWeights) {
		for (int f = 0; f < initialWeights.length; f++) {
			String featName = indexToFeature.get(f);
			if (regParametersModel!=null && regParametersModel.getFeatureToIndex().containsKey(featName)) {
				int index = regParametersModel.getFeatureToIndex().get(featName);
				initialWeights[f] = regParametersModel.getWeights()[index];
			}
		}
		return initialWeights;
	}

//	trains a HMM model with a discriminative objective
	public void trainSupervisedFeatureHMMDiscriminatively(
			int numLabels,
			List<TransFeatureTemplate> transFeatures,
			List<EmitFeatureTemplate> emitFeatures,
			FileWriter curveOut) {
		int stopLabel = numLabels;
		int startLabel = numLabels + 1;
		POSModel oldModel = null;
		if (restartTraining) {
			oldModel =
				(POSModel) BasicFileIO.readSerializedObject(restartModelFile);
			featureIndexCounts = oldModel.getFeatureIndexCounts();
			featureToIndex = oldModel.getFeatureToIndex();
			indexToFeature = oldModel.getIndexToFeature();
			indexToPOS = oldModel.getIndexToPOS();
			posToIndex = oldModel.getPosToIndex();
		}
		log.info("Caching features...");
		log.info("Caching transition features...");
		activeTransFeatures =
			getActiveTransFeatures(transFeatures,
					indexToWord.size(),
					numLabels + 2,
					startLabel,
					stopLabel);
		log.info("Caching emission features...");
		activeEmitFeatures =
			getActiveEmitFeatures(emitFeatures,
					indexToWord.size(),
					numLabels + 2,
					startLabel,
					stopLabel);
		if (useStackedFeatures) {
			activeStackedFeatures =
				getActiveStackedFeatures(indexToWord.size(),
						numLabels + 2,
						startLabel,
						stopLabel);
		}
		log.info("Num features: " + indexToFeature.size());
		SupervisedGenSequenceModelDiscObjective grad =
			new SupervisedGenSequenceModelDiscObjective(
					lObservations, goldLabels,
					numLabels, indexToWord.size(),
					indexToFeature.size(),
					regularizationWeight,
					printPosteriors);
		double[] zeroRegWeights = new double[indexToFeature.size()];
		double[] zeroBiasWeights = new double[indexToFeature.size()];
		Arrays.fill(zeroRegWeights, 0.0);
		Arrays.fill(zeroBiasWeights, 0.0);

		SupervisedGenSequenceModel num = grad.getNumeratorModel();
		GradientGenSequenceModel denom = grad.getDenominatorModel();
		num.setActiveFeatures(activeTransFeatures,
				activeEmitFeatures,
				activeStackedFeatures,
				indexToFeature.size(),
				zeroRegWeights,
				zeroBiasWeights);
		denom.setActiveFeatures(activeTransFeatures,
				activeEmitFeatures,
				activeStackedFeatures,
				indexToFeature.size(),
				zeroRegWeights,
				zeroBiasWeights);
		// Initialize weights to 0.0;
		double[] initialWeights = new double[grad.dimension()];
		if (restartTraining) {
			initialWeights = oldModel.getWeights();
		} else {
			Arrays.fill(initialWeights, 0.0);
			if (useBiasFeature) {
				initialWeights[featureToIndex.get("bias")] = biasFeatureBias;
			}
		}
		POSModel model = new POSModel();
		model.setFeatureIndexCounts(featureIndexCounts);
		model.setFeatureToIndex(featureToIndex);
		model.setIndexToFeature(indexToFeature);
		model.setIndexToPOS(indexToPOS);
		model.setPosToIndex(posToIndex);
		PrintLikelihoodCallbackCRF crfCallBack =
			new PrintLikelihoodCallbackCRF(model, curveOut);
		log.info("Training with LBFGS");
		double[] w = LBFGSOptimizer.optimize(grad,
				initialWeights,
				crfCallBack,
				iters);
		log.info("Finished training with LBFGS");
		model.setWeights(w);
		BasicFileIO.writeSerializedObject(modelFile, model);
	}

	public void trainSupervisedFeatureHMM(
			int numLabels,
			List<TransFeatureTemplate> transFeatures,
			List<EmitFeatureTemplate> emitFeatures,
			FileWriter curveOut) {
		GradientSequenceModel grad = new SupervisedGenSequenceModel(lObservations, goldLabels,
				numLabels, indexToWord.size());
		POSModel oldModel = null;
		if (restartTraining) {
			oldModel =
				(POSModel) BasicFileIO.readSerializedObject(restartModelFile);
			featureIndexCounts = oldModel.getFeatureIndexCounts();
			featureToIndex = oldModel.getFeatureToIndex();
			indexToFeature = oldModel.getIndexToFeature();
			indexToPOS = oldModel.getIndexToPOS();
			posToIndex = oldModel.getPosToIndex();
		}
		// Cache active features
		activeTransFeatures =
			getActiveTransFeatures(transFeatures,
					grad.getNumObservations(),
					grad.getNumLabels(),
					grad.getStartLabel(),
					grad.getStopLabel());

		activeEmitFeatures =
			getActiveEmitFeatures(emitFeatures,
					grad.getNumObservations(),
					grad.getNumLabels(),
					grad.getStartLabel(),
					grad.getStopLabel());
		if (useStackedFeatures) {
			activeStackedFeatures =
				getActiveStackedFeatures(grad.getNumObservations(),
						grad.getNumLabels(),
						grad.getStartLabel(),
						grad.getStopLabel());
		}

		log.info("Num features: " + indexToFeature.size());
		double[] regularizationWeights =
			getRegularizationWeights(transFeatures, emitFeatures);
		double[] regularizationBiases =
			getRegularizationBiases(transFeatures,
					emitFeatures,
					grad.getNumLabels(),
					grad.getStartLabel(),
					grad.getStopLabel());
		grad.setActiveFeatures(activeTransFeatures,
				activeEmitFeatures,
				activeStackedFeatures,
				indexToFeature.size(),
				regularizationWeights,
				regularizationBiases);
		// Initialize weights to 0.0;
		double[] initialWeights = new double[grad.getNumFeatures()];
		if (restartTraining) {
			initialWeights = oldModel.getWeights();
		} else {
			Arrays.fill(initialWeights, 0.0);
			if (useBiasFeature) {
				initialWeights[featureToIndex.get("bias")] = biasFeatureBias;
			}
		}
		grad.setWeights(initialWeights);
		POSModel model = new POSModel();
		model.setFeatureIndexCounts(featureIndexCounts);
		model.setFeatureToIndex(featureToIndex);
		model.setIndexToFeature(indexToFeature);
		model.setIndexToPOS(indexToPOS);
		model.setPosToIndex(posToIndex);
		PrintLikelihoodCallbackCRF crfCallBack =
			new PrintLikelihoodCallbackCRF(model, curveOut);
		log.info("Training with LBFGS");
		double[] w = LBFGSOptimizer.optimize(grad,
				initialWeights,
				crfCallBack,
				iters);
		log.info("Finished training with LBFGS");
		model.setWeights(w);
		BasicFileIO.writeSerializedObject(modelFile, model);
	}

	public void trainCRF(int numLabels,
			List<TransFeatureTemplate> transFeatures,
			List<EmitFeatureTemplate> emitFeatures,
			FileWriter curveOut) {
		/*
		 * getting active CRF features
		 */
		// Cache active features
		log.info("Caching features...");
		log.info("Caching transition features...");
		POSModel oldModel = null;
		if (restartTraining) {
			oldModel =
				(POSModel) BasicFileIO.readSerializedObject(restartModelFile);
			featureIndexCounts = oldModel.getFeatureIndexCounts();
			featureToIndex = oldModel.getFeatureToIndex();
			indexToFeature = oldModel.getIndexToFeature();
			indexToPOS = oldModel.getIndexToPOS();
			posToIndex = oldModel.getPosToIndex();
		}
		activeCRFTransFeatures =
			getActiveCRFTransFeatures(transFeatures,
					indexToWord.size(),
					numLabels);
		log.info("Caching emission features...");
		activeCRFEmitFeatures =
			getActiveCRFEmitFeatures(emitFeatures,
					indexToWord.size(),
					numLabels);
		log.info("Total number of features:" + indexToFeature.size());

		VertexFeatureExtractor vertexExtractor =
			new VertexFeatureExtractor(activeCRFEmitFeatures);
		EdgeFeatureExtractor edgeExtractor =
			new EdgeFeatureExtractor(activeCRFTransFeatures);
		DifferentiableFunction objective = new CRFObjectiveFunction(
				lObservations,
				goldLabels,
				numLabels,
				regularizationWeight,
				vertexExtractor,
				edgeExtractor,
				indexToFeature.size());
		POSModel model = new POSModel();
		model.setFeatureIndexCounts(featureIndexCounts);
		model.setFeatureToIndex(featureToIndex);
		model.setIndexToFeature(indexToFeature);
		model.setIndexToPOS(indexToPOS);
		model.setPosToIndex(posToIndex);
		PrintLikelihoodCallbackCRF crfCallBack =
			new PrintLikelihoodCallbackCRF(model, curveOut);
		log.info("Training with LBFGS");
		double[] initialWeights = new double[indexToFeature.size()];
		if (restartTraining) {
			initialWeights = oldModel.getWeights();
		} else {
			for (int i = 0; i < initialWeights.length; i ++) {
				initialWeights[i] = 0.0;
			}
			if (useBiasFeature) {
				initialWeights[featureToIndex.get("bias")] = biasFeatureBias;
			}
		}
		double[] w = LBFGSOptimizer.optimize(objective,
				initialWeights,
				crfCallBack,
				iters);
		log.info("Finished training with LBFGS");
		model.setWeights(w);
		BasicFileIO.writeSerializedObject(modelFile, model);
	}

	public void test() {
		log.info("test set:" + testSet);
		Collection<Pair<List<String>, List<String>>>  sequences =
			TabSeparatedFileReader.readPOSSeqences(testSet,
					numLabeledSentences,
					maxSentenceLength);
		if (useGlobalForLabeledData) {
			testCRF(sequences);
		} else {
			testFeatureHMM(sequences);
		}
	}

	public List<List<String>> testCRF(Collection<Pair<List<String>, List<String>>>  sequences,
						POSModel model) {
		featureIndexCounts = model.getFeatureIndexCounts();
		featureToIndex = model.getFeatureToIndex();
		indexToFeature = model.getIndexToFeature();
		indexToPOS = model.getIndexToPOS();
		posToIndex = model.getPosToIndex();

		int numLabels = indexToPOS.size();
		/*
		 * clearing these two data structures during inference
		 * because new words were creating memory explosion
		 * dipanjan 11/08/2011
		 */
		indexToWord.clear();
		wordToIndex.clear();
		Pair<int[][], int[][]> pairList = POSUtil.getObservationsAndGoldLabels(
				sequences,
				indexToWord,
				wordToIndex,
				indexToPOS,
				posToIndex);
		lObservations = pairList.getFirst();
		goldLabels = pairList.getSecond();
		// logObservationInfo();
		// logInputInfo();
		if (useStackedFeatures) {
			Collection<Pair<List<String>, List<String>>> stackedSequences
			= TabSeparatedFileReader.readPOSSeqences(stackedFile,
					numLabeledSentences,
					maxSentenceLength);
			pairList = POSUtil.getObservationsAndGoldLabels(
					stackedSequences,
					indexToWord,
					wordToIndex,
					indexToPOS,
					posToIndex);
			stackedTags = pairList.getSecond();
			for (int i = 0; i < stackedTags.length; i ++) {
				if (stackedTags[i].length != lObservations.length) {
					log.severe("Problem with length of sentence:" + i);
					System.exit(-1);
				}
			}
		}
		/*
		 * getting active CRF features
		 */
		// Cache active features
		List<TransFeatureTemplate> transFeatures =
			POSFeatureTemplates.getTransFeatures(useBiasFeature);
		List<EmitFeatureTemplate> emitFeatures =
			POSFeatureTemplates.getEmitFeatures(useStandardFeatures,
					lengthNGramSuffixFeature,
					useTagDictionary, wordToIndex,
					tagDictionary,
					tagsToClusters,
					noahsFeatures,
					distSimTable,
					namesArray);
		log.info("Caching features...");
		log.info("Caching transition features...");
		activeCRFTransFeatures =
			getActiveCRFTransFeatures(transFeatures,
					indexToWord.size(),
					numLabels);
		log.info("Caching emission features...");
		activeCRFEmitFeatures =
			getActiveCRFEmitFeatures(emitFeatures,
					indexToWord.size(),
					numLabels);
		VertexFeatureExtractor vertexExtractor =
			new VertexFeatureExtractor(activeCRFEmitFeatures);
		EdgeFeatureExtractor edgeExtractor =
			new EdgeFeatureExtractor(activeCRFTransFeatures);
		double[] trainedWeights = model.getWeights();

		double[] largerSetOfWeights = new double[indexToFeature.size()];
		Arrays.fill(largerSetOfWeights, 0.0);
		for (int i = 0; i < trainedWeights.length; i ++) {
			largerSetOfWeights[i] = trainedWeights[i];
		}
		Inference inf =  new Inference(numLabels, vertexExtractor, edgeExtractor);
		double total = 0.0;
		double correct = 0.0;
		if (runOutput != null) {
			BufferedWriter bWriter = BasicFileIO.openFileToWrite(runOutput);
			for (int i = 0; i < lObservations.length; i ++) {
				List<Integer> tags = posteriorDecode(
						lObservations[i], inf, largerSetOfWeights);
				// List<Integer> tags = getViterbiLabelSequence(lObservations[i], inf, largerSetOfWeights);
				for (int j = 0; j < goldLabels[i].length; j++) {
					if(goldLabels[i][j] == tags.get(j)) {
						correct++;
					}
					total++;
					BasicFileIO.writeLine(bWriter,
							indexToWord.get(lObservations[i][j]) +
							"\t" + indexToPOS.get(tags.get(j)));
				}
				BasicFileIO.writeLine(bWriter, "");
			}
			log.info("Accuracy:" + (correct / total));
			BasicFileIO.closeFileAlreadyWritten(bWriter);
		} else {
			ArrayList<List<String>> col =
				new ArrayList<List<String>>();
			for (int i = 0; i < lObservations.length; i ++) {
				List<Integer> tags = posteriorDecode(
						lObservations[i], inf, largerSetOfWeights);
				List<String> list = new ArrayList<String>();
				for (int j = 0; j < goldLabels[i].length; j++) {
					list.add(indexToPOS.get(tags.get(j)));
				}
				col.add(list);
			}
			return col;
		}
		return null;
	}

	public void testCRF(Collection<Pair<List<String>, List<String>>>  sequences) {
		POSModel model = (POSModel) BasicFileIO.readSerializedObject(modelFile);
		testCRF(sequences, model);
	}

	public List<Integer> posteriorDecode(int[] s, Inference inf, double[] w) {
		Pair<double[][], double[]> alphaAndFactors =
			inf.getAlphas(s, w);
		Pair<double[][], double[]> betaAndFactors =
			inf.getBetas(s, w);
		double[][] vposts =
			inf.getVertexPosteriors(alphaAndFactors, betaAndFactors);
		List<Integer> result = new ArrayList<Integer>();
		for (double[] vPost: vposts) {
			double max = -Double.MAX_VALUE;
			int ind = -1;
			for (int i = 0; i < vPost.length; i++) {
				if (vPost[i] > max) {
					max = vPost[i];
					ind = i;
				}
			}
			result.add(ind);
		}
		return result;
	}

	public List<Integer> getViterbiLabelSequence(int[] s, Inference inf, double[] w) {
		return getTopKLabelSequencesAndScores(s, 1, inf, w).get(0).getFirst();
	}

	public List<Pair<List<Integer>, Double>> getTopKLabelSequencesAndScores(int[] s, int k, Inference inf, double[] w) {
		Pair<int[][][][], double[][][]> chart = inf.getKBestChartAndBacktrace(s, w, k);
		List<Pair<List<Integer>, Double>> sentences = new ArrayList<Pair<List<Integer>, Double>>(k);
		int n = s.length;
		PriorityQueue<Pair<Integer, Integer>> rankedScores = buildRankedScoreQueue(chart.getSecond()[n-1]);
		for (int i=0; i<k && rankedScores.hasNext(); i++) {
			double score = rankedScores.getPriority();
			Pair<Integer, Integer> chain = rankedScores.next();
			sentences.add(Pair.makePair(rebuildChain(chart.getFirst(), chain.getFirst(), chain.getSecond()), score));
		}
		return sentences;
	}

	private PriorityQueue<Pair<Integer, Integer>> buildRankedScoreQueue(double[][] scores) {
		PriorityQueue<Pair<Integer, Integer>> pq = new PriorityQueue<Pair<Integer,Integer>>();
		for (int l=0; l<scores.length; l++) {
			for (int c=0; c<scores[l].length; c++) {
				pq.add(Pair.makePair(l, c), scores[l][c]);
			}
		}
		return pq;
	}

	private List<Integer> rebuildChain(int[][][][] backtrace, int endLabel, int endCandidate) {
		int n = backtrace.length;
		List<Integer> l = new ArrayList<Integer>(n);
		int currentLabel = endLabel;
		int currentCandidate = endCandidate;
		for (int i=n-1; i>=0; i--) {
			l.add(currentLabel);
			int nextLabel = backtrace[i][currentLabel][currentCandidate][0];
			currentCandidate = backtrace[i][currentLabel][currentCandidate][1];
			currentLabel = nextLabel;
		}
		assert(currentLabel == -1 && currentCandidate == 0);
		Lists.reverse(l);
		return l;
	}


	public void testFeatureHMM(Collection<Pair<List<String>, List<String>>>  sequences) {
		POSModel model = (POSModel) BasicFileIO.readSerializedObject(modelFile);
		featureIndexCounts = model.getFeatureIndexCounts();
		featureToIndex = model.getFeatureToIndex();
		indexToFeature = model.getIndexToFeature();
		indexToPOS = model.getIndexToPOS();
		posToIndex = model.getPosToIndex();

		Pair<int[][], int[][]> pairList = POSUtil.getObservationsAndGoldLabels(
				sequences,
				indexToWord,
				wordToIndex,
				indexToPOS,
				posToIndex);
		System.out.println("Use tag dictionary..." + useTagDictionary);
		numTags = indexToPOS.size();
		if (useTagDictionary) {
			System.out.println("Reading tag dictionary...");
			readTagDictionary();
		}

		lObservations = pairList.getFirst();
		goldLabels = pairList.getSecond();
		logObservationInfo();
		logInputInfo();
		if (useStackedFeatures) {
			Collection<Pair<List<String>, List<String>>> stackedSequences
			= TabSeparatedFileReader.readPOSSeqences(stackedFile,
					numLabeledSentences,
					maxSentenceLength);
			pairList = POSUtil.getObservationsAndGoldLabels(
					stackedSequences,
					indexToWord,
					wordToIndex,
					indexToPOS,
					posToIndex);
			stackedTags = pairList.getSecond();
			for (int i = 0; i < stackedTags.length; i ++) {
				if (stackedTags[i].length != lObservations[i].length) {
					log.severe("Problem with length of sentence:" + i);
					System.exit(-1);
				}
			}
		}
		List<TransFeatureTemplate> transFeatures =
			POSFeatureTemplates.getTransFeatures(useBiasFeature);
		List<EmitFeatureTemplate> emitFeatures =
			POSFeatureTemplates.getEmitFeatures(useStandardFeatures,
					lengthNGramSuffixFeature,
					useTagDictionary, wordToIndex,
					tagDictionary,
					tagsToClusters,
					noahsFeatures,
					distSimTable,
					namesArray);
		FileWriter curveOut = null;
		try {
			curveOut = new FileWriter(execPoolDir + "/" + "curve");
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Do gradient ascent
		GradientSequenceModel grad = null;
		int numLabels = indexToPOS.size();
		grad = new GradientGenSequenceModel(lObservations,
				stackedTags,
				numLabels,
				indexToWord.size(),
				printPosteriors);
		activeTransFeatures =
			getActiveTransFeatures(transFeatures,
					grad.getNumObservations(),
					grad.getNumLabels(),
					grad.getStartLabel(),
					grad.getStopLabel());

		activeEmitFeatures =
			getActiveEmitFeatures(emitFeatures,
					grad.getNumObservations(),
					grad.getNumLabels(),
					grad.getStartLabel(),
					grad.getStopLabel());

		if (useStackedFeatures) {
			activeStackedFeatures =
				getActiveStackedFeatures(grad.getNumObservations(),
						grad.getNumLabels(),
						grad.getStartLabel(),
						grad.getStopLabel());
		}
		log.info("Num features: " + indexToFeature.size());
		double[] regularizationWeights =
			getRegularizationWeights(transFeatures, emitFeatures);
		double[] regularizationBiases =
			getRegularizationBiases(transFeatures,
					emitFeatures,
					grad.getNumLabels(),
					grad.getStartLabel(),
					grad.getStopLabel());
		grad.setActiveFeatures(activeTransFeatures,
				activeEmitFeatures,
				activeStackedFeatures,
				indexToFeature.size(),
				regularizationWeights,
				regularizationBiases);
		double[] initialWeights = new double[grad.getNumFeatures()];
		double[] trainedWeights = model.getWeights();
		Arrays.fill(initialWeights, 0.0);
		for (int i = 0; i < trainedWeights.length; i ++) {
			initialWeights[i] = trainedWeights[i];
		}
		grad.setWeights(initialWeights);
		grad.computePotentials();
		grad.getForwardBackward().compute();
		double margProb = grad.calculateRegularizedLogMarginalLikelihood();
		int[][] guessLabels = grad.getForwardBackward().posteriorDecode();
		log.info("Log marginal prob: " + margProb);
		evaluateCurrentScore(curveOut, grad.getWeights(), guessLabels, margProb, 0);
		if (printPosteriors) {
			double[][][] posteriors = grad.getAllPosteriors();
			printPosteriors(posteriors);
		}
	}

	public void printPosteriors(double[][][] posteriors) {
		System.out.println("Printing posteriors....");
		Comparator comp = new Comparator<Pair<String, Double>>() {
			public int compare(Pair<String, Double> o1,
					Pair<String, Double> o2) {
				if (o1.getSecond() > o2.getSecond()) {
					return -1;
				} else if (o1.getSecond() == o2.getSecond()) {
					return 0;
				} else
					return 1;
			}
		};
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(runOutput + ".posteriors");
		int posSize = indexToPOS.size();
		for (int i = 0; i < lObservations.length; i ++) {
			for (int j = 0; j < goldLabels[i].length; j++) {
				Pair<String, Double>[] arr = new Pair[posSize];
				String line = indexToWord.get(lObservations[i][j]) + "\t";
				for (int k = 0; k < posSize; k++) {
					arr[k] = new Pair<String, Double>(indexToPOS.get(k), posteriors[i][j][k]);
				}
				Arrays.sort(arr, comp);
				for (int k = 0; k < posSize; k++) {
					line += arr[k].getFirst() + " " + arr[k].getSecond() + " ";
				}
				line = line.trim();
				BasicFileIO.writeLine(bWriter, line);
			}
			BasicFileIO.writeLine(bWriter, "");
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}

	private double scoreLabels(int[][] goldLabels, int[][] guessLabels) {
		double correct = 0.0;
		double total = 0.0;
		if (goldLabels.length != guessLabels.length) {
			log.severe("Problem. Length of gold labels should be equal " +
					"to guess labels:" + goldLabels.length +
					" " + guessLabels.length);
			System.exit(-1);
		}
		for (int i = 0; i < goldLabels.length; i++) {
			if (goldLabels[i].length != guessLabels[i].length) {
				log.severe("Problem. Length of gold labels in sentence " +
						i +" should be equal " +
						"to guess labels:" + goldLabels[i].length +
						" " + guessLabels[i].length);
				System.exit(-1);
			}
			for (int j = 0; j < goldLabels[i].length; j++) {
				if (goldLabels[i][j] == guessLabels[i][j]) {
					correct++;
				}
				total++;
			}
		}
		return (correct / total);
	}

	private void evaluateCurrentScore(FileWriter curveOut,
			double[] weights,
			int[][] guessLabels,
			double margProb,
			int iter) {
		double score = scoreLabels(goldLabels,
				guessLabels);
		log.info("Score: " + score);
		if (curveOut != null) {
			try {
				curveOut.append(String.format("%d\t%f\t%f\n", iter, margProb, score));
				curveOut.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(runOutput);
		for (int i = 0; i < lObservations.length; i ++) {
			for (int j = 0; j < goldLabels[i].length; j++) {
				BasicFileIO.writeLine(bWriter,
						indexToWord.get(lObservations[i][j]) +
						"\t" + indexToPOS.get(guessLabels[i][j]));
			}
			BasicFileIO.writeLine(bWriter, "");
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
		printWeightsToFile(iter, weights);
	}

	private void printWeightsToFile(int iter, double[] weights) {
		if (iter % printRate == 0) {
			try {
				FileWriter w = new FileWriter(execPoolDir +"/" + "weights"+iter);
				Counter<String> weightsCounter = new Counter<String>();
				for (int f=0; f<indexToFeature.size(); ++f) {
					weightsCounter.setCount(indexToFeature.get(f), weights[f]);
				}
				List<String> sortedFeatures = weightsCounter.getSortedKeys();
				for (int f=0; f<sortedFeatures.size(); ++f) {
					w.write(String.format("%s\t%f\n", sortedFeatures.get(f), weightsCounter.getCount(sortedFeatures.get(f))));
					w.flush();
				}
				w.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private class PrintLikelihoodCallbackCRF implements CallbackFunction {
		private FileWriter writer;
		private POSModel model;

		public PrintLikelihoodCallbackCRF(POSModel model0,
				FileWriter writer0) {
			this.writer = writer0;
			this.model = model0;
		}

		public void callback(Object... args) {
			double[] weights = (double[]) args[0];
			int iteration = (Integer) args[1];
			double margProb = -((Double) args[2]);
			double[] grad = (double[]) args[3];
			double derivative = 0.0;
			for (int f=0; f<grad.length; ++f) {
				derivative += grad[f] * grad[f];
			}
			log.info("End of iteration:"+ iteration);
			log.info("Log probability:" + margProb);
			log.info("Derivative: " + derivative);
			if(iteration % 10 == 0) {
				model.setWeights(weights);
				BasicFileIO.writeSerializedObject(modelFile+"_"+iteration, model);
			}
		}
	}

	private class PrintLikelihoodCallback implements CallbackFunction {
		private FileWriter writer;
		private GradientSequenceModel func;

		public PrintLikelihoodCallback(FileWriter writer0, GradientSequenceModel func0) {
			this.writer = writer0;
			this.func = func0;
		}

		public void callback(Object... args) {
			double margProb = -((Double) args[2]);
			int[][] guessLabels = func.getForwardBackward().posteriorDecode();
			evaluateCurrentScore(writer, (double[]) args[0], guessLabels, margProb, ((Integer) args[1])+1);
			double[] grad = (double[]) args[3];
			double derivative = 0.0;
			for (int f=0; f<grad.length; ++f) {
				derivative += grad[f] * grad[f];
			}
			log.info("Derivative: " + derivative);
		}
	}
}
