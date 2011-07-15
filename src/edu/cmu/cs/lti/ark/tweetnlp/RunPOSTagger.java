package edu.cmu.cs.lti.ark.tweetnlp;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.cmu.cs.lti.ark.ssl.pos.POSOptions;
import edu.cmu.cs.lti.ark.ssl.pos.SemiSupervisedPOSTagger;
import fig.basic.OptionsParser;
import fig.basic.Option;

/**
 * Runs the POS tagger on raw tweet text -- unlike SemiSupervisedPOSTagger which does training and testing on CoNLL-formatted, pre-tokenized text.
 * 
 * @author brendano
 *
 */
public class RunPOSTagger {
	
	public static class Opts {
		@Option(gloss="Input filename of tweets.")
		public static String input = null;
		
		@Option(gloss="textline = one tweet's text per line, in UTF-8.  TODO support conll and json")
		public static String input_format = "textline";
		
		@Option(gloss="Output filename of taggings.")
		public static String output = null;
		
		@Option(gloss="conll = one token per line, blank lines separating tweets.")
		public static String output_format = "conll";
	}

	/** Returns list of tags, one per token, parallel to the input tokens. */
	public static List<String> doPOSTagging(List<String> toks, SemiSupervisedPOSTagger tagger) {
		// TODO please replace this
		return dummyTagging(toks);
	}
	public static List<String> dummyTagging(List<String> toks) {
		ArrayList<String> tags = new ArrayList();
		for (String tok : toks) tags.add("N");
		return tags;
	}
	public static void main(String[] args) throws Exception {
		
		OptionsParser op = new OptionsParser(Opts.class);
		op.doParse(args);
//		System.out.println("OPTIONS:\n" + op.doGetOptionPairs());
		if (Opts.input.equals("-")) throw new RuntimeException("stdin unimplemented");
		
		
		System.out.println("Loading POS tagger.");
		// TODO dipanjan please help :)
		// If you keep it, be aware in its current state it doesn't seem to work, it needs more tweaking.
		// Right now it doesn't work anyways.
		// One big thing is, --testSet is not a good way to go.  
		// This code needs to be in control of feeding in sentences. 
		// This string-arg list is a horrible hack, feel free to get rid of.
		String[] posOptionArgs = new String[]{
				"--trainOrTest","test"
				// ... more ...
		};
		POSOptions posOptions = new POSOptions(posOptionArgs);
//		SemiSupervisedPOSTagger tagger = new SemiSupervisedPOSTagger(posOptions);
		SemiSupervisedPOSTagger tagger = null;
		
		System.out.println("Tagging tweets from file: " + Opts.input);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(Opts.input), "UTF-8"));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Opts.output), "UTF-8"));
		String line;
		while((line = reader.readLine()) != null) {
			List<String> toks = Twokenize.tokenizeForTagger_J(line);
			List<String> tags = doPOSTagging(toks, tagger);
			
			if (Opts.output_format.equals("conll")) {
				for (int i=0; i < toks.size(); i++) {
					writer.write(toks.get(i) + "\t" + tags.get(i) + "\n");
				}
				writer.write("\n");
				writer.flush();
			}
		}
	}
}
