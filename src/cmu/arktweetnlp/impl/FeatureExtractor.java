package cmu.arktweetnlp.impl;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.Metaphone;

import cmu.arktweetnlp.RunTagger;
import cmu.arktweetnlp.Twokenize;
import cmu.arktweetnlp.util.BasicFileIO;
import cmu.arktweetnlp.util.Runner;
import cmu.arktweetnlp.util.Util;
import edu.berkeley.nlp.util.StringUtils;
import edu.stanford.nlp.util.Pair;

/**
 * Extracts features and numberizes them
 * Also numberizes other things if necessary (e.g. label numberizations for MEMM training)
 */
public class FeatureExtractor {

	/** Only use the model for vocabulary and dimensionality info. **/
	private Model model;
	private ArrayList<FeatureExtractorInterface> allFeatureExtractors;
	public boolean isTrainingTime;
	public boolean dumpMode = false;
	public FeatureExtractor(Model model, boolean isTrainingTime){
		this.model = model;
		this.isTrainingTime = isTrainingTime;
		assert model.labelVocab.isLocked();
		initializeFeatureExtractors();
	}

	/**
	 * Does feature extraction on one sentence.
	 * 
	 * Input: textual representation of sentence
	 * Output: fills up modelSentence with numberized features
	 */
	public void computeFeatures(Sentence linguisticSentence, ModelSentence modelSentence) {
		int T = linguisticSentence.T();
		assert linguisticSentence.T() > 0; //TODO: handle this when assertions are off
		computeObservationFeatures(linguisticSentence, modelSentence);
		if (isTrainingTime) {
			for (int t=0; t < T; t++) {
				modelSentence.labels[t] = model.labelVocab.num( linguisticSentence.labels.get(t) );
			}
			computeCheatingEdgeFeatures(linguisticSentence, modelSentence);
		}
	}

	/**
	 * Peek at the modelSentence to see its labels -- for training only!
	 * @param sentence
	 * @param modelSentence
	 */
	private void computeCheatingEdgeFeatures(Sentence sentence, ModelSentence modelSentence) {
		assert isTrainingTime;
		modelSentence.edgeFeatures[0] = model.startMarker();
		for (int t=1; t < sentence.T(); t++) {
			modelSentence.edgeFeatures[t] = modelSentence.labels[t-1];
		}
	}

	private void computeObservationFeatures(Sentence sentence, ModelSentence modelSentence) {
		PositionFeaturePairs pairs = new PositionFeaturePairs();
		// Extract in featurename form
		for (FeatureExtractorInterface fe : allFeatureExtractors) {
			fe.addFeatures(sentence.tokens, pairs);
		}

		// Numberize.  This should be melded with the addFeatures() loop above, so no wasteful
		// temporaries that later turn out to be OOV... but is this really an issue?
		for (int i=0; i < pairs.size(); i++) {
			int t = pairs.labelIndexes.get(i);
			String fName = pairs.featureNames.get(i);
			int fID = model.featureVocab.num(fName);
			if ( ! isTrainingTime && fID == -1) {
				// Skip OOV features at test time.
				// Note we have implicit conjunctions from base features, so
				// these are base features that weren't seen for *any* label at training time -- of course they will be useless for us...
				continue;
			}
			double fValue = pairs.featureValues.get(i);
			modelSentence.observationFeatures.get(t).add(new Pair<Integer,Double>(fID, fValue));
		}
		if (dumpMode) {
			Util.p("");
			for (int t=0; t < sentence.T(); t++) {
				System.out.printf("%s\n\t", sentence.tokens.get(t));
				for (Pair<Integer,Double> fv : modelSentence.observationFeatures.get(t)) {
					System.out.printf("%s ", model.featureVocab.name(fv.first));
				}
				System.out.printf("\n");
			}
		}
	}


	public interface FeatureExtractorInterface {
		/**
		 * Input: sentence
		 * Output: labelIndexes, featureIDs/Values through positionFeaturePairs
		 *
		 * We want to yield a sequence of (t, featID, featValue) pairs,
		 * to be conjuncted against label IDs at position t.
		 * Represent as parallel arrays.  Ick yes, but we want to save object allocations (is this crazy?)
		 * This method should append to them.
		 */
		public void addFeatures(List<String> tokens, PositionFeaturePairs positionFeaturePairs);
	}

	public static class PositionFeaturePairs {
		public ArrayList<Integer> labelIndexes;
		public ArrayList<String> featureNames;
		public ArrayList<Double> featureValues;

		public PositionFeaturePairs() {
			labelIndexes = new ArrayList<Integer>();
			featureNames = new ArrayList<String>();
			featureValues = new ArrayList<Double>();
		}
		public void add(int labelIndex, String featureID) {
			add(labelIndex, featureID, 1.0);
		}
		public void add(int labelIndex, String featureID, double featureValue) {
			labelIndexes.add(labelIndex);
			featureNames.add(featureID);
			featureValues.add(featureValue);
		}
		public int size() { return featureNames.size(); }
	}


	///////////////////////////////////////////////////////////////////////////
	//
	// Actual feature extractors

	// for performance, figuring out a numberization approach faster than string concatenation might help
	// internet suggests that String.format() is slower than string concat
	// maybe can reuse a StringBuilder object? Ideally, would do direct manipulation of a char[] with reuse.

	private void initializeFeatureExtractors() {
		allFeatureExtractors = new ArrayList<FeatureExtractorInterface>();
		allFeatureExtractors.add(new Paths3());
		allFeatureExtractors.add(new NgramSuffix(140));
		allFeatureExtractors.add(new NgramPrefix(140));
		allFeatureExtractors.add(new POSTagDict());
		allFeatureExtractors.add(new MetaphonePOSDict());
		allFeatureExtractors.add(new PrevWord());
		//allFeatureExtractors.add(new Prev2Words());
		allFeatureExtractors.add(new NextWord());
		//allFeatureExtractors.add(new Next2Words());
		//allFeatureExtractors.add(new URLFeatures());
		allFeatureExtractors.add(new WordformFeatures());
		allFeatureExtractors.add(new Listofnames("proper_names"));
		allFeatureExtractors.add(new Listofnames("celebs")); //2012-08-09 version of freebase celebrity list
		allFeatureExtractors.add(new Listofnames("videogame")); //june 22 version of freebase video game list
		allFeatureExtractors.add(new Listofnames("mobyplaces"));	//moby dictionary of US locations
		allFeatureExtractors.add(new Listofnames("family"));
		allFeatureExtractors.add(new Listofnames("male"));
		allFeatureExtractors.add(new Listofnames("female"));
		allFeatureExtractors.add(new CapitalizationFeatures());
		allFeatureExtractors.add(new SimpleOrthFeatures());
		allFeatureExtractors.add(new PrevNext());
		allFeatureExtractors.add(new Positions());
	}

	public class WordformFeatures implements FeatureExtractorInterface {
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
				String normalizedtok=tok.replaceAll("[‘’´`]", "'").replaceAll("[“”]", "\"");
				pairs.add(t, "Word|" + normalizedtok);
				pairs.add(t, "Lower|" + Runner.normalize(normalizedtok));
				pairs.add(t, "Xxdshape|" + Xxdshape(normalizedtok), .5);
				pairs.add(t, "charclass|" + charclassshape(tok), .5);
			}
		}

		private String Xxdshape(String tok) { 
			String s=tok.replaceAll("[a-z]", "x").replaceAll("[0-9]", "d").replaceAll("[A-Z]","X");
			return s;
		}
		private String charclassshape(String tok) {
			StringBuilder sb = new StringBuilder(3 * tok.length());
			for(int i=0; i<tok.length(); i++){
				sb.append(Character.getType(tok.codePointAt(i))).append(',');			
			}
			return sb.toString();
		}
	}


	public class SimpleOrthFeatures implements FeatureExtractorInterface {
		public Pattern hasDigit = Pattern.compile("[0-9]");
		/** TODO change to punctuation class, or better from Twokenize **/
		//Pattern allPunct = Pattern.compile("^[^a-zA-Z0-9]*$");
		Pattern allPunct = Pattern.compile("^\\W*$");
		Pattern emoticon = Pattern.compile(Twokenize.emoticon);
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);

				if (hasDigit.matcher(tok).find())
					pairs.add(t, "HasDigit");

				if (tok.charAt(0) == '@')
					pairs.add(t, "InitAt");

				if (tok.charAt(0) == '#')
					pairs.add(t, "InitHash");

				/*if (allPunct.matcher(tok).matches()){
					pairs.add(t, "AllPunct");
				}*/
				if (emoticon.matcher(tok).matches()){
					pairs.add(t, "Emoticon");
				}
				if (tok.contains("-")){
					pairs.add(t, "Hyphenated");
					String[] splithyph = Runner.normalize(tok).split("-", 2);
					//pairs.add(t, "preHyph|"+splithyph[0]);//quote -Ben Franklin
					//pairs.add(t, "postHyph|"+splithyph[1]);//-esque
					for (String part:splithyph){
						pairs.add(t, "hyph|" + part);
					}
				}
			}
		}    
	}
	public class NgramSuffix implements FeatureExtractorInterface {
		int ngram=3;
		public NgramSuffix(int i) {
			ngram=i;
		}
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = Runner.normalize(tokens.get(t).replaceAll("[‘’´`]", "'").replaceAll("[“”]", "\""));
				int l=tok.length();
				for(int i=1;i<=ngram;i++){
					if(l>=i){
						pairs.add(t, i+"gramSuff|"+tok.substring(l-i, l));
						/*if (t<tokens.size()-1 && i==3)
							pairs.add(t+1, "prev"+i+"gramSuff|"+tok.substring(l-i, l).toLowerCase()); */
					}
					else break;
				}
			}
		}    
	}
	public class NgramPrefix implements FeatureExtractorInterface {
		int ngram=3;
		public NgramPrefix(int i) {
			ngram=i;
		}
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = Runner.normalize(tokens.get(t).replaceAll("[‘’´`]", "'").replaceAll("[“”]", "\""));
				int l=tok.length();
				for(int i=1;i<=ngram;i++){
					if(l>=i){
						pairs.add(t, i+"gramPref|"+tok.substring(0, i));
					}
					else break;
				}
			}
		}
	}
	public class URLFeatures implements FeatureExtractorInterface {	
		Pattern validURL = Pattern.compile(Twokenize.url);
		Pattern validEmail = Pattern.compile(Twokenize.Email);
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
				if (validURL.matcher(tok).matches()){
					pairs.add(t, "validURL");
				}
				if (validEmail.matcher(tok).matches()){
					pairs.add(t, "validURL");
				}
			}
		}
	}    
	public class Positions implements FeatureExtractorInterface {	
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < Math.min(tokens.size(), 4); t++) {
				pairs.add(t, "t="+t);
			}
			for (int t=tokens.size()-1; t > Math.max(tokens.size()-4, -1); t--) {
				pairs.add(t, "t=-"+t);
			}
		}
	}

	public class CapitalizationFeatures implements FeatureExtractorInterface {	
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
				/*if (Character.isUpperCase(tok.charAt(0))) {
	    			pairs.add(t, "initcap");
	    		}*/
				int numChar = 0;
				int numCap = 0;
				for (int i=0; i < tok.length(); i++) {
					numChar += Character.isLetter(tok.charAt(i)) ? 1 : 0;
					numCap += Character.isUpperCase(tok.charAt(i)) ? 1 : 0;
				}

				// A     => shortcap
				// HELLO => longcap
				// Hello => initcap
				// HeLLo => mixcap

				boolean allCap = numChar==numCap;
				boolean shortCap = allCap && numChar <= 1;
				boolean longCap  = allCap && numChar >= 2; 
				boolean initCap = !allCap && numChar >= 2 && Character.isUpperCase(tok.charAt(0)) && numCap==1;
				boolean mixCap = numCap>=1 && numChar >= 2 && (tok.charAt(0) != '@') && !(tok.startsWith("http://"));

				String caplabel = shortCap ? "shortcap" : longCap ? "longcap" : initCap ? "initcap"
						: mixCap ? "mixcap" : "nocap";
				if (caplabel != null){
					if (numChar >= 1) {
						if (tok.endsWith("'s"))
							caplabel = "pos-" + caplabel;
						else if (t==0)
							caplabel = "first-" + caplabel;
						pairs.add(t, caplabel+"");
					}
				}
			}
		}
	}
	private static Metaphone _metaphone = null;
	private static DoubleMetaphone dblmetaphone = null;
	public static Metaphone getMetaphone() {
		if (_metaphone == null) {
			_metaphone = new Metaphone();
			_metaphone.setMaxCodeLen(100);
		} 
		return _metaphone;
	};
	public static DoubleMetaphone getDblMetaphone() {
		if (dblmetaphone == null) {
			dblmetaphone = new DoubleMetaphone();
			dblmetaphone.setMaxCodeLen(100);
		} 
		return dblmetaphone;
	};
	private String MetaphoneNum(String str){ //change this eventually
		StringBuilder sb = new StringBuilder(str);
		if (str.charAt(str.length()-1)=='1')
			sb.deleteCharAt(str.length()-1).append("one");
		if (str.charAt(0)=='1')
			sb.deleteCharAt(0).insert(0, "one");
		if (str.charAt(0)=='2')
			sb.deleteCharAt(0).insert(0, "two");
		else if(str.charAt(0)=='4')
			sb.deleteCharAt(0).insert(0, "four");
		return sb.toString();
	}
	public class MetaphoneLexical implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
				if(tok.length()>1){
					//String ppword=MetaphoneNum(tok);
					String metaphone_word = getDblMetaphone().encode(tok);
					String alternate_word = getDblMetaphone().doubleMetaphone(tok, true);
					pairs.add(t, "metaphone_word|"+metaphone_word);
					if(!metaphone_word.equals(alternate_word))
						pairs.add(t, "metaphone_word|"+alternate_word);
				}
			}        	
		}
	}
	public class MetaphonePOSDict implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
				if(tok.length()>1){
					//tok=tok.replace("4", "four").replace("2", "two").replace("3","three");
					String metaphone_word = getMetaphone().encode(tok);
					TagDictionary d = TagDictionary.instance();
					String key = String.format("**MP**%s", metaphone_word);
					if (d.WORD_TO_POS.containsKey(key)) {
						List<String> poses = d.WORD_TO_POS.get(key);
						for (String pos : poses) {
							pairs.add(t, "metaph_POSDict|"+pos);
						}    				
					}
				}
			}
		}
	} 
	public class POSTagDict implements FeatureExtractorInterface{
		Pattern URL = Pattern.compile(Twokenize.url);
		Pattern letter = Pattern.compile("[A-Za-z]{3,}");
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
				TagDictionary d = TagDictionary.instance();
				List<String> poses = d.WORD_TO_POS.get(tok);
				if (poses == null && letter.matcher(tok).find() && (!URL.matcher(tok).matches())) {
					String normtok = Runner.normalizecap(tok);
					poses = d.WORD_TO_POS.get(normtok);
					if (poses==null) {
						ArrayList<String> fuzz = new ArrayList<String>(); 
						fuzz.addAll(Paths3.fuzztoken(normtok, true));
						fuzz.addAll(Paths3.fuzztoken(Runner.normalize(tok), true));
				    	for (String f:fuzz){
				    		poses = d.WORD_TO_POS.get(f);
				    		if (poses!=null){
				    			//System.err.println(tok+"->"+f);
				    			break;
				    		}
				    	}						
					}
				}
				if (poses!=null) {
					pairs.add(t, "POSTagDict|"+poses.get(0));
					if (t > 0)
						pairs.add(t-1, "NextPOSTag|" + poses.get(0));
					if (t < tokens.size()-1)
						pairs.add(t+1, "PrevPOSTag|" + poses.get(0));					
					for (int i=1; i < poses.size(); i++) {
						pairs.add(t, "POSTagDict|" + poses.get(i), (poses.size()-(double)i)/poses.size());
					}
				} 	    		
			}
		}
	}
	
	
	/** TODO this should be moved into config somehow **/
	public static String clusterResourceName = "/cmu/arktweetnlp/50mpaths2";
	
	public static class Paths3 implements FeatureExtractorInterface{
		public static HashMap<String,String> path;
		Pattern URL = Pattern.compile(Twokenize.url);
		static Pattern repeatchar = Pattern.compile("([\\w])\\1{1,}");
		static Pattern repeatvowel = Pattern.compile("(a|e|i|o|u)\\1+");
		public Paths3() {
			//read in paths file
			BufferedReader bReader = BasicFileIO.getResourceReader(clusterResourceName);
			String[] splitline = new String[3];
			String line=BasicFileIO.getLine(bReader);
			path = new HashMap<String,String>(); 
			while(line != null){
				splitline = line.split("\\t");
				//if (Integer.parseInt(splitline[2])>0)
					path.put(splitline[1], splitline[0]);
				line = BasicFileIO.getLine(bReader);
			}
			RunTagger.wordsInCluster = new HashSet<String>(path.keySet());
		}
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			String bitstring = null;
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
			    String normaltok = Runner.normalize(tok);
			    bitstring = path.get(normaltok);
			    if (bitstring==null){
			    	for (String fuzz:fuzztoken(normaltok, true)){
			    		bitstring = path.get(fuzz);
			    		if (bitstring!=null){
			    			//System.err.println(normaltok+"->"+fuzz);
			    			break;
			    		}
			    	}
			    }
			    
				if (bitstring!=null){
					int i;
					bitstring = StringUtils.pad(bitstring, 16).replace(' ', '0');
					for(i=2; i<=16; i+=2){
						pairs.add(t, "BigCluster|" + bitstring.substring(0,i));
					}
					if (t<tokens.size()-1){
						for(i=4; i<=12; i+=4)
							pairs.add(t+1, "PrevBigCluster"+"|" + bitstring.substring(0,i));
					}
					if (t>0){
						for(i=4; i<=12; i+=4)
							pairs.add(t-1, "NextBigCluster"+"|" + bitstring.substring(0,i));
					}
				}
/*				else{
					pairs.add(t, "BigCluster|none");
				}*/
			}
		}
		public static ArrayList<String> fuzztoken(String tok, boolean apos) {
	        ArrayList<String> fuzz = new ArrayList<String>();
	        fuzz.add(tok.replaceAll("[‘’´`]", "'").replaceAll("[“”]", "\""));
	        fuzz.add(tok);
	        fuzz.add(repeatchar.matcher(tok).replaceAll("$1"));//omggggggg->omg
	        fuzz.add(repeatchar.matcher(tok).replaceAll("$1$1"));//omggggggg->omgg
	        fuzz.add(repeatvowel.matcher(tok).replaceAll("$1"));//heeellloooo->helllo
	        if (apos && !(tok.startsWith("<URL"))){
	        	fuzz.add(tok.replaceAll("\\p{Punct}", ""));//t-swift->tswift
	        	//maybe a bad idea (bello's->bello, re-enable->re, croplife's->'s)
	        	fuzz.addAll(Arrays.asList(tok.split("\\p{Punct}")));
	        }
	        return fuzz;
        }
	}

	//Transitive Features    
	public class NextWord implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			ArrayList<String> normtoks = Runner.normalize(tokens);
			for (int t=0; t < tokens.size()-1; t++) {
				pairs.add(t, "nextword|"+tokens.get(t+1), .5);
				pairs.add(t, "nextword|"+normtoks.get(t+1), .5);
				pairs.add(t, "currnext|"+normtoks.get(t)+"|"+normtoks.get(t+1));
			}
			pairs.add(tokens.size()-1, "currnext|"+normtoks.get(tokens.size()-1)+"|<END>");
			pairs.add(tokens.size()-1, "nextword|<END>");
		}
	}
	public class Next2Words implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			if (tokens.size()>1){
				ArrayList<String> normtoks = Runner.normalize(tokens);
				for (int t=0; t < tokens.size()-2; t++) {
					pairs.add(t, "next2words|"+tokens.get(t+1)+"|"+tokens.get(t+2), .5);
					pairs.add(t, "next2words|"+normtoks.get(t+1)
							+"|"+normtoks.get(t+2), .5);
				}
				pairs.add(tokens.size()-2, "next2words|"+normtoks.get(tokens.size()-1)+"|<END>", .5);
				pairs.add(tokens.size()-2, "next2words|"+tokens.get(tokens.size()-1)+"|<END>", .5);
			}
		}
	}
	public class PrevWord implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			ArrayList<String> normtoks = Runner.normalize(tokens);
			pairs.add(0, "prevword|<START>");
			pairs.add(0, "prevcurr|<START>|"+normtoks.get(0));
			for (int t=1; t < tokens.size(); t++) {
				pairs.add(t, "prevword|"+tokens.get(t-1));
				pairs.add(t, "prevword|"+normtoks.get(t-1));
				pairs.add(t, "prevcurr|"+normtoks.get(t-1)
						+"|"+normtoks.get(t));
			}
		}
	}
	public class Prev2Words implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) { 	
			if (tokens.size()>1){
				ArrayList<String> normtoks = Runner.normalize(tokens);
				pairs.add(1, "prev2words|<START>|"+normtoks.get(0)+"|"+normtoks.get(1));
				for (int t=2; t < tokens.size(); t++) {
					pairs.add(t, "prev2words|"+tokens.get(t-2)+"|"+tokens.get(t-1));
					pairs.add(t, "prev2words|"+normtoks.get(t-2)+"|"+normtoks.get(t-1));
				}
			}
		}
	}
	public class PrevNext implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			ArrayList<String> normtoks = Runner.normalize(tokens);
			if (tokens.size()>1){
				pairs.add(0, "prevnext|<START>|"+normtoks.get(1));
				//pairs.add(0, "prevcurrnext|<START>|"+normtoks.get(0)+"|"+normtoks.get(1));
				for (int t=1; t < normtoks.size()-1; t++) {
					pairs.add(t, "prevnext|"+normtoks.get(t-1)+"|"+normtoks.get(t+1));
					//pairs.add(t, "prevcurrnext|"+normtoks.get(t-1)+"|"+normtoks.get(0)+"|"+normtoks.get(1));
				}
				pairs.add(normtoks.size()-1,"prevnext|"+normtoks.get(tokens.size()-2)+"|<END>");
				//pairs.add(normtoks.size()-1,"prevcurrnext|"+normtoks.get(tokens.size()-2)+"|"+normtoks.get(tokens.size()-1)+"|<END>");
			}
		}
	}
	private static HashSet<String> initDict(String dict){
		BufferedReader bReader = BasicFileIO.getResourceReader("/cmu/arktweetnlp/" + dict);
		HashSet<String> dictset = new HashSet<String>();
		String line=BasicFileIO.getLine(bReader);
		while(line != null){
			dictset.add(line.toLowerCase());
			line=BasicFileIO.getLine(bReader);
		}
		return dictset;
	}
	/*
	 * If you add a new list, make sure to run "mvn install -q" so it's copied
	 * to the target folder or else you'll get a null pointer exception
	 */
	public class Listofnames implements FeatureExtractorInterface{
		String Listname="";
		HashSet<String> members;
		public Listofnames(String str) {
			Listname=str;
			this.members = initDict(Listname);
		}
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
				String normaltok = tok.toLowerCase().replaceAll("[‘’´`]", "'").replaceAll("[“”]", "\"");
				if (members.contains(normaltok)){
					pairs.add(t, Listname);
				}
			}        	
		}
	}
}

