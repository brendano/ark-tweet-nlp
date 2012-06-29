package newalgo;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import newalgo.io.CoNLLReader;

public class RunTagger {
    
    String inputFormat = "conll";
    String outputFormat = "conll";
    String inputFilename;
    String modelFilename;
    PrintStream outputStream = System.out;
    public boolean noOutput = false;
    
    Iterable<Sentence> inputIterable = null;
    Model model;
    
    // Only for evaluation mode (conll inputs)
    int numTokensCorrect = 0;
    int numTokens = 0;
    
    public void runTagger() throws IOException {
        if (inputFormat.equals("conll")) {
            List<Sentence> examples = CoNLLReader.readFile(inputFilename);
            inputIterable = examples;
        } else {
            assert false;
        }
        
        model = Model.loadModelFromText(modelFilename);
        FeatureExtractor fe = new FeatureExtractor(model, false);
        
        boolean evalMode = inputFormat.equals("conll");

        for (Sentence sentence : inputIterable) {
            ModelSentence ms = new ModelSentence(sentence.T());
            fe.computeFeatures(sentence, ms);
            model.greedyDecode(ms);

            if ( ! noOutput) {
                outputTagging(sentence, ms);
            }
            if (evalMode) {
                evaluateSentenceTagging(sentence, ms);
            }
        }
        
        if (evalMode) {
            System.err.printf("%d / %d correct = %.4f acc, %.4f err\n", 
                    numTokensCorrect, numTokens,
                    numTokensCorrect*1.0 / numTokens,
                    1 - (numTokensCorrect*1.0 / numTokens)
            );
        }
    }
    
    public void evaluateSentenceTagging(Sentence lSent, ModelSentence mSent) {
        for (int t=0; t < mSent.T; t++) {
            int trueLabel = model.labelVocab.num(lSent.labels.get(t));
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
                outputStream.printf("%s\t%s\n", lSent.tokens.get(t),  model.labelVocab.name(mSent.labels[t]) );
            }
            outputStream.println("");
        } else {
            assert false;
        }
        
    }
    
    
    ///////////////////////////////////////////////////////////////////

    
    public static void main(String[] args) throws IOException {        
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
