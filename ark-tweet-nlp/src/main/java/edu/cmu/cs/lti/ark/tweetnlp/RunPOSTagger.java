package edu.cmu.cs.lti.ark.tweetnlp;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import edu.cmu.cs.lti.ark.ssl.pos.POSOptions;
import edu.cmu.cs.lti.ark.ssl.pos.SemiSupervisedPOSTagger;
import edu.cmu.cs.lti.ark.ssl.util.BasicFileIO;
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
		
		@Option(gloss="textline (default) = one tweet's text per line, in UTF-8." +
			"json = a JSON formatted file from the Twitter API.  TODO support conll")
		public static String input_format = "textline";
		
		@Option(gloss="Output filename of taggings.")
		public static String output = null;
		
		@Option(gloss="conll = one token per line, blank lines separating tweets.")
		public static String format = "conll";
	}

	private static TweetTaggerInstance ttInstance = null;
	
	/** Returns list of tags, one per token, parallel to the input tokens. */
	public static List<String> doPOSTagging(List<String> toks) {
		return tweetTagging(toks);
	}
	
	public static List<String> tweetTagging(List<String> toks) {
		return TweetTaggerInstance.getInstance().getTagsForOneSentence(toks);
	}
		
	public static void main(String[] args) throws Exception {
		
		OptionsParser op = new OptionsParser(Opts.class);
		op.doParse(args);
        if (Opts.input == null) {
            op.printHelp();
            return;
        }
		if (Opts.input.equals("-")) throw new RuntimeException("stdin unimplemented");

		System.out.println("Tagging tweets from file: " + Opts.input);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(Opts.input), "UTF-8"));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Opts.output), "UTF-8"));
		JsonFactory factory;
		JsonParser jp;		
		String line;

		int renewEvery=10000;
		int lineNumber=0;
		if (Opts.input_format.equals("textline")){
			long timenow=System.currentTimeMillis();			
			while((line = reader.readLine()) != null) {
				// Re-read tagger every n lines to keep memory in check.
				if (++lineNumber % renewEvery == 0) {
					TweetTaggerInstance.getInstance().renew();
				}
				
				List<String> toks = Twokenize.tokenizeForTagger(line);
				List<String> tags = doPOSTagging(toks);
				if (Opts.format.equals("conll")) {
					for (int i=0; i < toks.size(); i++) {
						writer.write(toks.get(i) + "\t" + tags.get(i) + "\n");
					}
					writer.write("\n");
					writer.flush();
				} else {
	                throw new RuntimeException("Unknown output format " + Opts.format);
	            }
			}
			long finishtime=System.currentTimeMillis();
			System.out.println("tokenized and tagged " + lineNumber + 
					" tweets in " + (finishtime-timenow)/1000L + " seconds...");
			writer.close();			
		}
		else if(Opts.input_format.equals("json")){
			factory = new JsonFactory();
			jp = factory.createJsonParser(new File(Opts.input));
			jp.nextValue();
			long timenow=System.currentTimeMillis();
			while((line = BasicFileIO.getLine(jp)) != null) {
				// Re-read tagger every n lines to keep memory in check.
				if (++lineNumber % renewEvery == 0) {
					TweetTaggerInstance.getInstance().renew();
				}
				
				List<String> toks = Twokenize.tokenizeForTagger(line);
				List<String> tags = doPOSTagging(toks);
				if (Opts.format.equals("conll")) {
					for (int i=0; i < toks.size(); i++) {
						writer.write(toks.get(i) + "\t" + tags.get(i) + "\n");
					}
					writer.write("\n");
					writer.flush();
				} else {
	                throw new RuntimeException("Unknown output format " + Opts.format);
	            }
			}
			long finishtime=System.currentTimeMillis();
			System.out.println("tokenized and tagged " + lineNumber + 
					" tweets in " + (finishtime-timenow)/1000L + " seconds...");
			writer.close();
			jp.close();
			}
		else{
			throw new RuntimeException("Unknown input format " + Opts.input_format);
		}		
	}
}
