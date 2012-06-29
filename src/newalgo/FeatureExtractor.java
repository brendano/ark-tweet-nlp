package newalgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.Metaphone;

import edu.cmu.cs.lti.ark.ssl.pos.SemiSupervisedPOSTagger;
import edu.cmu.cs.lti.ark.ssl.pos.TagDictionary;
import edu.cmu.cs.lti.ark.ssl.util.BasicFileIO;
import edu.cmu.cs.lti.ark.tweetnlp.Twokenize;

import newalgo.util.Util;

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

	public FeatureExtractor(Model model, boolean isTrainingTime) {
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
		assert linguisticSentence.T() > 0;
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
		allFeatureExtractors.add(new WordformFeatures());
		allFeatureExtractors.add(new SimpleOrthFeatures());

		//allFeatureExtractors.add(new Positions());
		allFeatureExtractors.add(new URLFeatures());
		allFeatureExtractors.add(new NgramSuffix(4));
		allFeatureExtractors.add(new CapitalizationFeatures()); 
		allFeatureExtractors.add(new MetaphoneLexical());
		allFeatureExtractors.add(new MetaphonePOSDict());
		allFeatureExtractors.add(new PrevWord());
		allFeatureExtractors.add(new NextWord());
		//allFeatureExtractors.add(new PrevCurrNext());
		allFeatureExtractors.add(new POSTagDict());
		allFeatureExtractors.add(new Listofnames("celebs")); //june 7 version of freebase celebrity list
		allFeatureExtractors.add(new Listofnames("videogame")); //june 22 version of freebase video game list
		allFeatureExtractors.add(new Listofnames("mobyplaces"));	//moby dictionary of US locations
	}

	public class WordformFeatures implements FeatureExtractorInterface {
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
				String normalizedtok=tok.replaceAll("[‘’]", "'").replaceAll("[“”]", "\"");
				pairs.add(t, "Word|" + normalizedtok);
				pairs.add(t, "Lower|" + normalizedtok.toLowerCase());
				// pairs.add(t, "Xxdshape|" + Xxdshape(tok));
				pairs.add(t, "charclass|" + charclassshape(tok));
			}
		}

		private String Xxdshape(String tok) { //charclassshape performs better
			String s=tok.replaceAll("[a-z]", "x").replaceAll("[0-9]", "d").replaceAll("[A-Z]","X");
			return s;
		}

	}
	private String charclassshape(String tok) {
		StringBuilder sb = new StringBuilder(2 * tok.length());
		for(Character ch:tok.toCharArray()){
			sb.append(Character.getType(ch)).append(',');			
		}
		return sb.toString();
	}

	public class SimpleOrthFeatures implements FeatureExtractorInterface {
		public Pattern hasDigit = Pattern.compile(".*[0-9].*");
		/** TODO change to punctuation class, or better from Twokenize **/
		//public Pattern allPunct = Pattern.compile("^[^a-zA-Z0-9]*$");
		public Pattern allPunct = Pattern.compile("^\\W*$");
		public Pattern emoticon = Pattern.compile(Twokenize.emoticon);
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);

				if (hasDigit.matcher(tok).matches())
					pairs.add(t, "HasDigit");

				if (tok.charAt(0) == '@')
					pairs.add(t, "InitAt");

				if (tok.charAt(0) == '#')
					pairs.add(t, "InitHash");

				if (allPunct.matcher(tok).matches()){
					pairs.add(t, "AllPunct");
				}

				if (emoticon.matcher(tok).matches()){
					pairs.add(t, "Emoticon");
				}

				if (tok.contains("-")){
					pairs.add(t, "Hyphenated");
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
				String tok = tokens.get(t);
				int l=tok.length();
				for(int i=1;i<=ngram;i++){
					if(l>=i){
						pairs.add(t, i+"gramSuff|"+tok.substring(l-i, l));
					}
				}
			}
		}    
	}   
	public class URLFeatures implements FeatureExtractorInterface {	
		Pattern validURL=Pattern.compile(Twokenize.url);
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
				if (validURL.matcher(tok).matches()){
					pairs.add(t, "validURL");
				}
			}
		}
	}    
	public class Positions implements FeatureExtractorInterface {	
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t).toLowerCase();
				int i;
				pairs.add(t, "t="+t+"|"+tok);
				pairs.add(t, "t="+(t-tokens.size())+"|"+tok);
				for(i=1;i<=t;i++){	//everything >=0
					pairs.add(t, "t>="+i);
				}
				for(int j=i;j<tokens.size()-1;j++){
					pairs.add(t, "t<="+j);
				}
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

				boolean allCap = numChar==numCap;
				boolean shortCap = allCap && numChar <= 1;
				boolean longCap  = allCap && numChar >= 2;
				boolean initCap = !allCap && numChar >= 2 && Character.isUpperCase(tok.charAt(0));

				String caplabel = shortCap ? "shortcap" : longCap ? "longcap" : initCap ? "initcap" : null;

				if (numChar >= 1 && caplabel!=null) {
					pairs.add(t, caplabel);
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
	private String MetaphoneNum(String str){
		StringBuilder sb = new StringBuilder(str);
		if (str.charAt(str.length()-1)=='1')
			sb.deleteCharAt(str.length()-1).append("one");
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
					//tok=tok.replace("4", "four").replace("2", "two"); //?
					String ppword=MetaphoneNum(tok);
					String metaphone_word = getDblMetaphone().encode(ppword);
					String alternate_word = getDblMetaphone().doubleMetaphone(ppword, true);
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
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
				TagDictionary d = TagDictionary.instance();
				if (d.WORD_TO_POS.containsKey(tok)) {
					List<String> poses = d.WORD_TO_POS.get(tok);
					for (int i=0; i < poses.size(); i++) {
						pairs.add(t, "POSTagDict|"+poses.get(i));
					}
				} 	    		
			}
		}
	}     
	//Transitive Features    
	public class NextWord implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size()-1; t++) {
				pairs.add(t, "nextword|"+tokens.get(t+1));
				//	pairs.add(t, "currnext|"+tokens.get(t)+"|"+tokens.get(t+1));
			}
			//	pairs.add(tokens.size()-1, "currnext|"+tokens.get(tokens.size()-1)+"|<END>");
			pairs.add(tokens.size()-1, "nextword|<END>");
		}
	}

	public class PrevWord implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) { 	
			pairs.add(0, "prevword|<START>");
			//	pairs.add(0, "prevcurr|<START>|"+tokens.get(0));
			for (int t=1; t < tokens.size(); t++) {
				pairs.add(t, "prevword|"+tokens.get(t-1));
				//	pairs.add(t, "prevcurr|"+tokens.get(t-1)+"|"+tokens.get(t));
			}
		}
	}
	public class PrevCurrNext implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			if (tokens.size()>1){
				pairs.add(0, "prevcurrnext|<START>|"+tokens.get(0)+tokens.get(1));
				for (int t=1; t < tokens.size()-1; t++) {
					pairs.add(t, "prevcurrnext|"+tokens.get(t-1)+"|"+tokens.get(t)+"|"+tokens.get(t+1));
				}
				pairs.add(tokens.size()-1,"prevcurrnext|"+tokens.get(tokens.size()-2)+
						"|"+tokens.get(tokens.size()-1)+"|<END>");
			}
		}
	}    
	private static String[] initDict(String dict){
		BufferedReader bReader = new BufferedReader(
				new InputStreamReader(SemiSupervisedPOSTagger.class.getResourceAsStream(dict),Charset.forName("UTF-8")));
		ArrayList<String> dictlist = new ArrayList<String>();
		String line=BasicFileIO.getLine(bReader);
		while(line != null){
			dictlist.add(line);
			line=BasicFileIO.getLine(bReader);
		}
		String[] dictarray = new String[dictlist.size()];
		dictlist.toArray(dictarray);
		Arrays.sort(dictarray);
		return dictarray;
	}
	public class Listofnames implements FeatureExtractorInterface{
		String Listname="";
		String[] members;
		public Listofnames(String str) {
			Listname=str;
			members = initDict(Listname);
		}
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
				if (Arrays.binarySearch(members,tok,String.CASE_INSENSITIVE_ORDER)>0){
					pairs.add(t, Listname);
				}
			}        	
		}
	}   
}
