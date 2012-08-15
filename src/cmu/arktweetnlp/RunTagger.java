package cmu.arktweetnlp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;

import cmu.arktweetnlp.impl.ModelSentence;
import cmu.arktweetnlp.impl.Sentence;
import cmu.arktweetnlp.io.CoNLLReader;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;

/**
 * Commandline interface to run the Twitter POS tagger with a variety of possible input and output formats.
 * Also does basic evaluation if given labeled input text.
 * 
 * For basic usage of the tagger from Java, see instead Tagger.java.
 */
public class RunTagger {
	Tagger tagger;
	
	// Commandline I/O-ish options
	String inputFormat = "json";
	String outputFormat = "auto";
	int inputField = 1;
	
	String inputFilename;
	String modelFilename;
	
	PrintStream outputStream = System.out;
	Iterable<Sentence> inputIterable = null;
	
	// More options
	public boolean noOutput = false;
	public boolean justTokenize = false;
	
	
	// Evaluation stuff
	/** BTO what is this for? the OOV evals? TODO please rename appropriately, e.g. wordsSeenInTraining  */
	public static HashSet<String> vocab;
	public static double accuracy = 0.0;
	// Only for evaluation mode (conll inputs)
	int numTokensCorrect = 0;
	int numTokens = 0;
	int oovTokensCorrect = 0;
	int oovTokens = 0;
	public int clusterTokensCorrect = 0;
	public static int clusterTokens = 0;

	public static void getVocab() throws FileNotFoundException, IOException, ClassNotFoundException{
		// BTO TODO: can we delete this?
	    //vocab = (HashSet<String>) BasicFileIO.readSerializedObject("traindevvocab");
	}
	
	public static void die(String message) {
		// (BTO) I like "assert false" but assertions are disabled by default in java
		System.err.println(message);
		System.exit(-1);
	}
	public void runTagger() throws IOException, ClassNotFoundException {
		
		tagger = new Tagger();
		tagger.loadModel(modelFilename);
		
		if (inputFormat.equals("conll")) {
			runTaggerInEvalMode();
			return;
		} 
		assert (inputFormat.equals("json") || inputFormat.equals("text"));
		
		die("TODO");
		for (Pair<String, String> recordAndText : iterateTweets()) {
			Sentence sentence=null;
			ModelSentence mSent=null;
			// run tokenizer to fill out Sentence
			// run tagger to get tags
			outputPrependedTagging(sentence, mSent, this.justTokenize, recordAndText.first);
		}
	}
	/** yields (FullInputLine, TweetText) pairs. **/
	private Iterable<Pair<String, String>> iterateTweets() {
		// TODO
		return null;
	}


	/** Runs the correct algorithm (TODO make config option?) **/
	public void goDecode(ModelSentence mSent) {
		//tagger.model.greedyDecode(mSent);
		tagger.model.viterbiDecode(mSent);		
	}
	
	public void runTaggerInEvalMode() throws IOException, ClassNotFoundException {

		List<Sentence> examples = CoNLLReader.readFile(inputFilename); 
		inputIterable = examples;

		int[][] confusion = new int[tagger.model.numLabels][tagger.model.numLabels];

		for (Sentence sentence : examples) {
			ModelSentence mSent = new ModelSentence(sentence.T());
			tagger.featureExtractor.computeFeatures(sentence, mSent);
			goDecode(mSent);
			
			if ( ! noOutput) {
				outputJustTagging(sentence, mSent);	
			}
			evaluateSentenceTagging(sentence, mSent);
			//evaluateOOV(sentence, ms);
			//getconfusion(sentence, ms, confusion);
		}

		System.err.printf("%d / %d correct = %.4f acc, %.4f err\n", 
				numTokensCorrect, numTokens,
				numTokensCorrect*1.0 / numTokens,
				1 - (numTokensCorrect*1.0 / numTokens)
		);
		System.err.printf("%d / %d InVocab correct = %.4f acc, %.4f err\n", 
				oovTokensCorrect, oovTokens,
				oovTokensCorrect*1.0 / oovTokens,
				1 - (oovTokensCorrect*1.0 / oovTokens)
		);
/*			int i=0;
			System.out.println("\t"+tagger.model.labelVocab.toString().replaceAll(" ", ", "));
			for (int[] row:confusion){
				System.out.println(tagger.model.labelVocab.name(i)+"\t"+Arrays.toString(row));
				i++;
			}*/
	}

	private void evaluateOOV(Sentence lSent, ModelSentence mSent) {
		for (int t=0; t < mSent.T; t++) {
			int trueLabel = tagger.model.labelVocab.num(lSent.labels.get(t));
			int predLabel = mSent.labels[t];
			if(vocab.contains(lSent.tokens.get(t))){
				//System.err.println(lSent.tokens.get(t)+"\ttrue:"+tagger.model.labelVocab.name(trueLabel)+" pred:"+tagger.model.labelVocab.name(predLabel));
				oovTokensCorrect += (trueLabel == predLabel) ? 1 : 0;
				oovTokens += 1;
			}
			/*if (tagger.model.labelVocab.name(trueLabel).equals("U"))
				if (!tagger.model.labelVocab.name(predLabel).equals("U"))
					System.err.println(lSent.tokens.get(t)+"\t"+tagger.model.labelVocab.name(predLabel));*/			
		}    
    }
	private void getconfusion(Sentence lSent, ModelSentence mSent, int[][] confusion) {
		for (int t=0; t < mSent.T; t++) {
			int trueLabel = tagger.model.labelVocab.num(lSent.labels.get(t));
			int predLabel = mSent.labels[t];
			if(trueLabel!=-1)
				confusion[trueLabel][predLabel]++;
		}    
    }
	public void evaluateSentenceTagging(Sentence lSent, ModelSentence mSent) {
		for (int t=0; t < mSent.T; t++) {
			int trueLabel = tagger.model.labelVocab.num(lSent.labels.get(t));
			int predLabel = mSent.labels[t];
			numTokensCorrect += (trueLabel == predLabel) ? 1 : 0;
			numTokens += 1;
		}
	}

	/**
	 * assume mSent's labels hold the tagging.
	 */
	public void outputJustTagging(Sentence lSent, ModelSentence mSent) {
		if (outputFormat.equals("conll")) {
			for (int t=0; t < mSent.T; t++) {
				outputStream.printf("%s\t%s\n", 
						lSent.tokens.get(t),  
						tagger.model.labelVocab.name(mSent.labels[t]) );
			}
			outputStream.println("");
		} 
		else {
			die("bad output format for just tagging: " + outputFormat);
		}
	}
	/**
	 * assume mSent's labels hold the tagging.
	 * 
	 * @param lSent
	 * @param mSent
	 * @param inputLine -- assume does NOT have trailing newline.  (default from java's readLine)
	 */
	public void outputPrependedTagging(Sentence lSent, ModelSentence mSent, 
			boolean suppressTags, String inputLine) {
		
		int T = lSent.T();
		String[] tokens = new String[T];
		String[] tags = new String[T];
		for (int t=0; t < T; t++) {
			tokens[t] = lSent.tokens.get(t);
			if (!suppressTags) {
				tags[t] = tagger.model.labelVocab.name(mSent.labels[t]);	
			}
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.join(tokens));
		sb.append("\t");
		if (!suppressTags) {
			sb.append(StringUtils.join(tags));
			sb.append("\t");
		}
		sb.append(inputLine);
//		sb.append(org.apache.commons.lang.StringUtils.join(inputFields, '\t'));
		
		System.out.println(sb.toString());
	}


	///////////////////////////////////////////////////////////////////


	public static void main(String[] args) throws IOException, ClassNotFoundException {        
		if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
			usage();
		}

		RunTagger tagger = new RunTagger();

		int i = 0;
		while (i < args.length) {
			if (!args[i].startsWith("-")) {
				break;
			} else if (args[i].equals("--model")) {
				tagger.modelFilename = args[i+1];
				i += 2;
			} else if (args[i].equals("--just-tokenize")) {
				die("TODO implement");
			} else if (args[i].equals("--quiet")) {
				tagger.noOutput = true;
				i += 1;
			} else if (args[i].equals("--input-format")) {
				tagger.inputFormat = args[i+1];
				i += 2;
			} else if (args[i].equals("--output-format")) {
				tagger.outputFormat = args[i+1];
				i += 2;
			} else if (args[i].equals("--input-field")) {
				tagger.inputField = Integer.parseInt(args[i+1]);
				i += 2;
			}
			else {
				System.out.println("bad option " + args[i]);
				usage();                
			}
		}
		if (tagger.modelFilename == null) {
			usage("Need to specify model");
		}
		
		tagger.finalizeOutputFormat();

		if (args.length - i > 1) usage();
		tagger.inputFilename = args[i];
		
		tagger.runTagger();
	}
	
	public void finalizeOutputFormat() {
		if (outputFormat.equals("auto")) {
			if (inputFormat.equals("conll")) {
				outputFormat = "conll";
			} else {
				outputFormat = "pretsv";
			}
		}
	}
	
	public static void usage() {
		usage(null);
	}

	public static void usage(String extra) {
		System.out.println(
"RunTagger [options] <ExamplesFilename>" +
"\n  runs the CMU ARK Twitter tagger on tweets from ExamplesFilename, " +
"\n  writing taggings to standard output." +
"\n\nOptions:" +
"\n  --model <Filename>        Specify model filename." +
"\n                            [TODO should this default to something?]" +
"\n  --just-tokenize           Only run the tokenizer; no POS tags." +
"\n  --quiet                   Quiet: no output" +
"\n  --input-format <Format>   Default: json." +
"\n                            Options: json, text, conll" +
"\n  --output-format <Format>  Default: auto decide based on input format." +
"\n                            Options: pretsv, conll" +
"\n  --input-field NUM         Default: 1" +
"\n                            Which tab-separated field contains the input" +
"\n                            (1-indexed, like unix 'cut')" +
"\n                            Only for (json, text) input formats." +
"\n" +
"\nThere are two types of input-output formats: " +
"\n(1) tweet-per-line, and (2) token-per-line." +
"\nTweet-per-line input formats:" +
"\n   json: Every input line has a JSON object containing the tweet," +
"\n         as per certain Twitter APIs. (The 'text' field will be tagged.)" +
"\n   text: Every input line has the text for one tweet." +
"\nFor both cases, we the lines in the input are actually TSV," +
"\nand the tweets (text or json) are one particular field." +
"\n(Therefore tab characters are not allowed in tweets." +
"\nTwitter's own JSON formats guarantee this;" +
"\nif you extract the text yourself, you must remove tabs and newlines.)" +
"\nThis allows metadata to be passed through." +
"\nBy default, the first field is used; change with --input-field." +
"\nTweet-per-line output format is" +
"\n   pretsv: Prepend the tokenization and tagging as two new TSV fields, " +
"\n           so the output includes a complete copy of the input." +
"\n           (Control where the fields are inserted with --output-field.)" +
"\nBy default, two TSV fields are prepended:" +
"\n          Tokenization \\t POSTags \\t (original data...)" +
"\nThe tokenization and tags are parallel space-separated lists." +
"\nWrite your own Java wrapper to Tagger.java for a different format." +
"\n" +
"\nThere is only one token-per-line format:" +
"\n   conll: Each line is: Token \\t Tag, and blank line separating tweets." +
"\n");
		
		if (extra != null) {
			System.out.println("ERROR: " + extra);
		}
		System.exit(1);
	}
}
