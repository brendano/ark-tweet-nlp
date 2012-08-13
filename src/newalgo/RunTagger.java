package newalgo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import newalgo.io.CoNLLReader;

/**
 * Commandline interface to run the tagger with a variety of possible input and output formats.
 * Also does basic evaluation if given labeled input text.
 * 
 * For basic usage of the tagger from Java, see Tagger.java.
 */
public class RunTagger {

	String inputFormat = "conll";
	String outputFormat = "conll";
	String inputFilename;
	String modelFilename; // Need to fill via defaults in a sane way
	PrintStream outputStream = System.out;
	public boolean noOutput = false;
	public static HashSet<String> vocab;
	Iterable<Sentence> inputIterable = null;
	Tagger tagger;
	public static double accuracy = 0.0;
	// Only for evaluation mode (conll inputs)
	int numTokensCorrect = 0;
	int numTokens = 0;
	int oovTokensCorrect = 0;
	int oovTokens = 0;
	public int clusterTokensCorrect = 0;
	public static int clusterTokens = 0;
	public static void getVocab() throws FileNotFoundException, IOException, ClassNotFoundException{
	    //vocab = (HashSet<String>) BasicFileIO.readSerializedObject("traindevvocab");
	}
	public void runTagger() throws IOException, ClassNotFoundException {
		if (inputFormat.equals("conll")) {
			List<Sentence> examples = CoNLLReader.readFile(inputFilename);
			inputIterable = examples;
		} else {
			assert false;
		}

		tagger = new Tagger();
		tagger.loadModel(modelFilename);
		int[][] confusion = new int[Model.numLabels][Model.numLabels];
		boolean evalMode = inputFormat.equals("conll");
		for (Sentence sentence : inputIterable) {
			ModelSentence ms = new ModelSentence(sentence.T());
			tagger.featureExtractor.computeFeatures(sentence, ms);
			//tagger.model.greedyDecode(ms);
			tagger.model.viterbiDecode(ms);
			if ( ! noOutput) {
				outputTagging(sentence, ms);
			}
			if (evalMode) {
				evaluateSentenceTagging(sentence, ms);
				//evaluateOOV(sentence, ms);
				//getconfusion(sentence, ms, confusion);
			}
		}

		if (evalMode) {
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
	 * assume ms' labels hold the tagging
	 */
	public void outputTagging(Sentence lSent, ModelSentence mSent) {
		if (outputFormat.equals("conll")) {
			for (int t=0; t < mSent.T; t++) {
				outputStream.printf("%s\t%s\n", 
						lSent.tokens.get(t),  
						tagger.model.labelVocab.name(mSent.labels[t]) );
			}
			outputStream.println("");
		} 
		else {
			assert false;
		}
	}


	///////////////////////////////////////////////////////////////////


	public static void main(String[] args) throws IOException, ClassNotFoundException {        
		if (args.length < 2 || args[0].equals("-h") || args[1].equals("--help")) {
			usage();
		}

		RunTagger tagger = new RunTagger();

		int i=0;
		while (i < args.length) {
			if (!args[i].startsWith("-")) break;
			if (args[i].equals("--input-format")) {
				tagger.inputFormat = args[i+1];
				i += 2;
			} else if (args[i].equals("--output-format")) {
				tagger.outputFormat = args[i+1];
				i += 2;
			} else if (args[i].equals("-q")) {
				tagger.noOutput = true;
				i += 1;
			}
			else {
				usage();                
			}
		}

		if (args.length - i > 2) usage();


		tagger.inputFilename = args[i];
		tagger.modelFilename = args[i+1];
		//        tagger.inputReader = BasicFileIO.openFileToRead(args[i]);
		tagger.runTagger();

	}

	public static void usage() {
		System.out.println(
				"RunTagger [options] <ExamplesFilename> <ModelFilename>\n" +
				"Options:" +
				"\n  --input-format <Format>" + 
				"\n  --output-format <Format>" +
				"\n  -q                               Quiet: no output" +
		"\n");
		System.exit(1);
	}
}
