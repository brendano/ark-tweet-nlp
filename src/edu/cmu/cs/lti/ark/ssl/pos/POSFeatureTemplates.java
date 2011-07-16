/**
 * Copyright 2010. 
 * Carnegie Mellon University.
 */
package edu.cmu.cs.lti.ark.ssl.pos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.language.Metaphone;

import edu.berkeley.nlp.util.StringUtils;
import fig.basic.Pair;

/**
 * Feature templates for POS Tagging.
 * @author dipanjand
 *
 */
public class POSFeatureTemplates {

	public static Logger log = Logger.getLogger(POSFeatureTemplates.class.getCanonicalName());

	public interface EmitFeatureTemplate {

		public List<Pair<String, Double>> getFeatures(int label, 
				String emission);
		public String getName();
	}

	public interface TransFeatureTemplate {

		public List<Pair<String,Double>> getFeatures(int label1, int label2);

		public String getName();

	}

	public interface InterpolationFeatureTemplate {
		public Pair<String,Double> getFeature(int id, int label);
		
		public String getName();
	}
	
	public class InterpolationIndicatorFeature implements InterpolationFeatureTemplate {
		public String name = "iind"; 

		public String getName() {return name;}

		public Pair<String, Double> getFeature(int id, int label) {
			return Pair.makePair(String.format(name+"|%d|%d", id, label), 1.0);
		}
	}
	
	public class TransIndicatorFeature implements TransFeatureTemplate {

		public String name = "tind"; 

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label1, int label2) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			features.add(Pair.makePair(String.format(name+"|%d|%d", label1, label2), 1.0));
			return features;
		}

	}

	public class BaseEmitFeature implements EmitFeatureTemplate {

		public boolean useTagDictionary = false;
		public Map<String, Integer> wordToIndex;
		public int[][] tagDictionary;
		public int[][] tagMapping;

		public BaseEmitFeature(boolean useTagDictionary0, 
				Map<String, Integer> wordToIndex0,
				int[][] tagDictionary0,
				int[][] tagMapping0) {
			useTagDictionary = useTagDictionary0;
			if (useTagDictionary) {
				wordToIndex = wordToIndex0;
				tagDictionary = tagDictionary0;
				tagMapping = tagMapping0;
			}
		}		

		public List<Pair<String, Double>> getFeatures(int label, String emission) {
			// TODO Auto-generated method stub
			return null;
		}

		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}
	}	

	public class EmitIndicatorFeature extends BaseEmitFeature {

		public EmitIndicatorFeature(boolean useTagDictionary0,
				Map<String, Integer> wordToIndex0, int[][] tagDictionary0,
				int[][] tagMapping0) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
		}

		public String name = "eind"; 
		
		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			if (isEmissionValid(useTagDictionary, word, wordToIndex, tagDictionary, label, tagMapping)) {
				features.add(Pair.makePair(String.format(name+"|%d|%s", label, word), 1.0));
			} else {
				features.add(Pair.makePair(String.format(name+"|%d|%s", label, word), Double.NEGATIVE_INFINITY));
			}
			return features;
		}
	}
	
	public class StackedIndicatorFeature extends BaseEmitFeature {
		
		public StackedIndicatorFeature(boolean useTagDictionary0,
				Map<String, Integer> wordToIndex0, int[][] tagDictionary0,
				int[][] tagMapping0) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
		}

		public String name = "stind"; 
		
		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String tag) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			features.add(Pair.makePair(String.format(name+"|fl|%d|%s", label, tag), 1.0));
			features.add(Pair.makePair(String.format(name+"|1|%d|%s", label, tag.substring(0,1)), 1.0));
			if (tag.length() > 2) {
				features.add(Pair.makePair(String.format(name+"|2|%d|%s", label, tag.substring(0,2)), 1.0));
			}
			return features;
		}
	}
	

	public class NGramSuffixIndicatorFeature extends BaseEmitFeature {

		public String name; 
		private int n;

		public NGramSuffixIndicatorFeature(boolean useTagDictionary0,
				Map<String, Integer> wordToIndex0, int[][] tagDictionary0, 
				int[][] tagMapping0, int n0) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
			this.n = n0;
			this.name = "suff"+n0;
		}

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			if (word.length() >= n) {
				if (isEmissionValid(useTagDictionary, word, wordToIndex, tagDictionary, label, tagMapping)) {
					features.add(Pair.makePair(String.format(name+"|%d|%s",  label, word.substring(word.length()-n, word.length())), 1.0));
				} else {
					features.add(Pair.makePair(String.format(name+"|%d|%s",  label, word.substring(word.length()-n, word.length())), Double.NEGATIVE_INFINITY));
				}
			}
			return features;
		}

	}

	public class InitialCapitalIndicatorFeature extends BaseEmitFeature {

		public String name = "initcap"; 

		public InitialCapitalIndicatorFeature(boolean useTagDictionary0,
				Map<String, Integer> wordToIndex0, int[][] tagDictionary0,
				int[][] tagMapping0) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
		}

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			if (Character.isUpperCase(word.charAt(0))) {
				if (isEmissionValid(useTagDictionary, word, wordToIndex, tagDictionary, label, tagMapping)) {
					features.add(Pair.makePair(String.format(name+"|%d", label), 1.0));
				} else {
					features.add(Pair.makePair(String.format(name+"|%d", label), Double.NEGATIVE_INFINITY));
				}
			}
			return features;
		}

	}

	public class CapitalizationFeatures extends BaseEmitFeature {
		public CapitalizationFeatures(boolean useTagDictionary0, Map<String, Integer> wordToIndex0, int[][] tagDictionary0, int[][] tagMapping0) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
		}
		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			
			int numChar = 0;
			int numCap = 0;
			for (int i=0; i < word.length(); i++) {
				numChar += Character.isLetter(word.charAt(i)) ? 1 : 0;
				numCap += Character.isUpperCase(word.charAt(i)) ? 1 : 0;
			}
			
			// A     => shortcap
			// HELLO => longcap
			// Hello => initcap
			
			boolean allCap = numChar==numCap;
			boolean shortCap = allCap && numChar <= 1;
			boolean longCap  = allCap && numChar >= 2;
			boolean initCap = !allCap && numChar >= 2 && Character.isUpperCase(word.charAt(0));
			
			String caplabel = shortCap ? "shortcap" : longCap ? "longcap" : initCap ? "initcap" : null;
			
			if (numChar >= 1 && caplabel!=null) {
				features.add(Pair.makePair(String.format("%s|%d", caplabel, label), 1.0));	
			}
			
			// Note: downcasing is repetitive with MetaphoneLexical, which implicitly does that anyway.
			// removing this gives me tiny regression 0.8777777777777778 => 0.8767295597484277
			if (numCap >= 1 && caplabel!=null) {
				String lowered = word.toLowerCase();
				if (isEmissionValid(useTagDictionary, word, wordToIndex, tagDictionary, label, tagMapping)) {
					features.add(Pair.makePair(String.format("downcase|%s|%s|%d", lowered, caplabel, label), 1.0)); 
				} else {
					features.add(Pair.makePair(String.format("downcase|%s|%s|%d", lowered, caplabel, label), Double.NEGATIVE_INFINITY));
				}
			}
			return features;
		}
	}

	public class ContainsHyphenIndicatorFeature extends BaseEmitFeature {

		public String name = "hyphen"; 

		public ContainsHyphenIndicatorFeature(boolean useTagDictionary0,
				Map<String, Integer> wordToIndex0, int[][] tagDictionary0,
				int[][] tagMapping0) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
		}

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			if (word.contains("-")) {
				if (isEmissionValid(useTagDictionary, word, wordToIndex, tagDictionary, label, tagMapping)) {
					features.add(Pair.makePair(String.format(name+"|%d", label), 1.0));
				} else {
					features.add(Pair.makePair(String.format(name+"|%d", label), Double.NEGATIVE_INFINITY));
				}
			}
			return features;
		}
	}

	public class ContainsDigitIndicatorFeature extends BaseEmitFeature  {

		public String name = "digit"; 

		public ContainsDigitIndicatorFeature(boolean useTagDictionary0,
				Map<String, Integer> wordToIndex0, int[][] tagDictionary0,
				int[][] tagMapping0) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
		}

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			boolean containsDigit = false;
			for (int i=0; i<word.length(); ++i) {
				if (Character.isDigit(word.charAt(i))) {
					containsDigit = true;
				}
			}
			if (containsDigit) {
				if (isEmissionValid(useTagDictionary, word, wordToIndex, tagDictionary, label, tagMapping)) {
					features.add(Pair.makePair(String.format(name+"|%d", label), 1.0));
				} else {
					features.add(Pair.makePair(String.format(name+"|%d", label), Double.NEGATIVE_INFINITY));
				}
			}
			return features;
		}

	}
	
	public class POSDictFeatures extends BaseEmitFeature {
		public POSDictFeatures(boolean useTagDictionary0, Map<String, Integer> wordToIndex0, int[][] tagDictionary0, int[][] tagMapping0) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
		}
		public String getName() { return "pos_dict_features"; }
		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			TagDictionary d = TagDictionary.instance();
			if (d.word2poses.containsKey(word)) {
				List<String> poses = d.word2poses.get(word);
				for (int i=0; i < poses.size(); i++) {
					String f;
					f = String.format("pos|label=%d|pos=%s", label, poses.get(i));
					features.add(Pair.makePair(f, 1.0));
					
					// Impart frequency information via ordinal rank indicators
					// They make a small difference (0.4% if using full dictionary, none if reduced dictionary)
					for (int j=0; j <= i; j++) {
						f = String.format("pos|label=%d|pos=%s,rank>=%d", label, poses.get(i), j);
						features.add(Pair.makePair(f, 1.0));
					}
//					for (int j=i+1; i < poses.size(); i++) {
//						f = String.format("pos|label=%d|pos=%s,rank<%d", label, poses.get(i), j);
//						features.add(Pair.makePair(f, 1.0));
//					}
				}
			} 
			// I think it's representationally equivalent to not use this
//			else {
//				features.add(Pair.makePair(String.format("pos|%d|no_pos_entry", label),1.0));
//			}
			return features;
		}
	}
	
	private static Metaphone _metaphone = null;
	public static Metaphone getMetaphone() {
		if (_metaphone == null) {
			_metaphone = new Metaphone();
			_metaphone.setMaxCodeLen(100);
		}
		return _metaphone;
	};

	public class MetaphoneLexical extends BaseEmitFeature {
		public MetaphoneLexical(boolean useTagDictionary0, Map<String, Integer> wordToIndex0, int[][] tagDictionary0, int[][] tagMapping0) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
		}
		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			String metaphone_word = getMetaphone().encode(word);
			features.add(Pair.makePair(String.format("metaphone_word|%d|%s", label, metaphone_word), 1.0));			
			return features;
		}
	}
	
	public class MetaphonePOSProjection extends BaseEmitFeature {
		public MetaphonePOSProjection(boolean useTagDictionary0, Map<String, Integer> wordToIndex0, int[][] tagDictionary0, int[][] tagMapping0) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
		}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			String metaphone_word = getMetaphone().encode(word);
			TagDictionary d = TagDictionary.instance();
			String key = String.format("**MP**%s", metaphone_word);
			if (d.word2poses.containsKey(key)) {
				List<String> poses = d.word2poses.get(key);
				
				for (String pos : poses) {
					String f = String.format("metaphone_posproj|%d|%s",label,pos);
					features.add(Pair.makePair(f, 1.0));
				}
				// Rank info can't save metaphone multi-POS
//				for (int i=0; i < poses.size(); i++) {
//					for (int j=0; j <= i; j++) {
//						String f = String.format("metaphone_posproj|%d|%s,rank>=%d",label,poses.get(i), j);
//						features.add(Pair.makePair(f, 1.0));
//					}
//				}
			}
			return features;
		}
	}

	
	
	public class StartsWithAtSymbolIndicatorFeature extends BaseEmitFeature  {
		public String name = "atsym"; 

		public StartsWithAtSymbolIndicatorFeature(boolean useTagDictionary0,
				Map<String, Integer> wordToIndex0, int[][] tagDictionary0,
				int[][] tagMapping0) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
		}

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			boolean atsym = false;
			if (word.charAt(0) == '@') {
				atsym = true;
			}
			if (atsym) {
				if (isEmissionValid(useTagDictionary, word, wordToIndex, tagDictionary, label, tagMapping)) {
					features.add(Pair.makePair(String.format(name+"|%d", label), 1.0));
				} else {
					features.add(Pair.makePair(String.format(name+"|%d", label), Double.NEGATIVE_INFINITY));
				}
			}
			return features;
		}
	}
	
	public class StartsWithHashSymbolIndicatorFeature extends BaseEmitFeature  {
		public String name = "hashsym"; 

		public StartsWithHashSymbolIndicatorFeature(boolean useTagDictionary0,
				Map<String, Integer> wordToIndex0, int[][] tagDictionary0,
				int[][] tagMapping0) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
		}

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			boolean hashsym = false;
			if (word.charAt(0) == '#') {
				hashsym = true;
			}
			if (hashsym) {
				if (isEmissionValid(useTagDictionary, word, wordToIndex, tagDictionary, label, tagMapping)) {
					features.add(Pair.makePair(String.format(name+"|%d", label), 1.0));
				} else {
					features.add(Pair.makePair(String.format(name+"|%d", label), Double.NEGATIVE_INFINITY));
				}
			}
			return features;
		}
	}
	
	public class VariousLexicalAspectsIndicatorFeature extends BaseEmitFeature  {
		public String name = "noahlex"; 
		public Map<String, String> nmap;
		public VariousLexicalAspectsIndicatorFeature(boolean useTagDictionary0,
				Map<String, Integer> wordToIndex0, int[][] tagDictionary0,
				int[][] tagMapping0,
				Map<String, String> noahsFeatures) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
			nmap = noahsFeatures;
		}

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			if (nmap.containsKey(word)) {		
				String feat = nmap.get(word);
				if (isEmissionValid(useTagDictionary, word, wordToIndex, tagDictionary, label, tagMapping)) {
					features.add(Pair.makePair(String.format(name+"|%d|%s", label, feat), 1.0));
				} else {
					features.add(Pair.makePair(String.format(name+"|%d|%s", label, feat), Double.NEGATIVE_INFINITY));
				}
			}
			return features;
		}
	}
	
	public class ContainsHTTPIndicatorFeature extends BaseEmitFeature  {
		public String name = "http";

		public ContainsHTTPIndicatorFeature(boolean useTagDictionary0,
				Map<String, Integer> wordToIndex0, int[][] tagDictionary0,
				int[][] tagMapping0) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
		}

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			boolean http = false;
			if (word.contains("http://")) {
				http = true;
			}
			if (http) {
				if (isEmissionValid(useTagDictionary, word, wordToIndex, tagDictionary, label, tagMapping)) {
					features.add(Pair.makePair(String.format(name+"|%d", label), 1.0));
				} else {
					features.add(Pair.makePair(String.format(name+"|%d", label), Double.NEGATIVE_INFINITY));
				}
			}
			return features;
		}
	}
	
	public class RTIndicatorFeature extends BaseEmitFeature  {
		public String name = "RT"; 

		public RTIndicatorFeature(boolean useTagDictionary0,
				Map<String, Integer> wordToIndex0, int[][] tagDictionary0,
				int[][] tagMapping0) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
		}

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			boolean http = false;
			if (word.equals("RT")) {
				http = true;
			}
			if (http) {
				if (isEmissionValid(useTagDictionary, word, wordToIndex, tagDictionary, label, tagMapping)) {
					features.add(Pair.makePair(String.format(name+"|%d", label), 1.0));
				} else {
					features.add(Pair.makePair(String.format(name+"|%d", label), Double.NEGATIVE_INFINITY));
				}
			}
			return features;
		}
	}
	
	public class NameIndicatorFeature extends BaseEmitFeature  {
		public String name = "isname"; 
		public String[] namesArray;
		
		public NameIndicatorFeature(boolean useTagDictionary0,
				Map<String, Integer> wordToIndex0, int[][] tagDictionary0,
				int[][] tagMapping0,
				String[] namesArray) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
			this.namesArray = namesArray;
		}

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			boolean isName = false;
			if (Arrays.binarySearch(namesArray, word) >= 0) {
				isName = true;
			}
			if (isName) {
				if (isEmissionValid(useTagDictionary, word, wordToIndex, tagDictionary, label, tagMapping)) {
					features.add(Pair.makePair(String.format(name+"|%d", label), 1.0));
				} else {
					features.add(Pair.makePair(String.format(name+"|%d", label), Double.NEGATIVE_INFINITY));
				}
			}
			return features;
		}
	}
	
	
	public class DistSimFeature extends BaseEmitFeature  {
		public String name = "distsim"; 
		public Map<String, double[]> distSimTable;
		
		public DistSimFeature(boolean useTagDictionary0,
				Map<String, Integer> wordToIndex0, int[][] tagDictionary0,
				int[][] tagMapping0,
				Map<String, double[]> distSimTable) {
			super(useTagDictionary0, wordToIndex0, tagDictionary0, tagMapping0);
			this.distSimTable = distSimTable;
		}

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			String lcWord = word.toLowerCase();
			if (distSimTable.containsKey(lcWord)) {
				if (isEmissionValid(useTagDictionary, word, wordToIndex, tagDictionary, label, tagMapping)) {
					double[] arr = distSimTable.get(lcWord);
					for (int i = 0; i < arr.length; i ++) {
						features.add(Pair.makePair(String.format(name+"|%d|%d", i, label), arr[i]));
					}
				} else {
					double[] arr = distSimTable.get(lcWord);
					for (int i = 0; i < arr.length; i ++) {
						features.add(Pair.makePair(String.format(name+"|%d|%d", i, label), Double.NEGATIVE_INFINITY));
					}
				}
			}
			return features;
		}
	}
	
	public class BiasIndicatorFeature implements TransFeatureTemplate {

		public String name = "bias"; 

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label1, int label2) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			features.add(Pair.makePair(String.format(name), 1.0));
			return features;
		}

	}

	public class NodeIndicatorFeature implements TransFeatureTemplate {

		public String name = "nind"; 

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label1, int label2) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			features.add(Pair.makePair(String.format(name+"|%d", label1), 1.0));
			return features;
		}

	}

	public class NextNodeIndicatorFeature implements TransFeatureTemplate {

		public String name = "nnind"; 

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label1, int label2) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			features.add(Pair.makePair(String.format(name+"|%d", label2), 1.0));
			return features;
		}
	}	

	public static InterpolationFeatureTemplate
		getInterpolationFeatures(int numLanguages) {
		POSFeatureTemplates templates = new POSFeatureTemplates();
		InterpolationFeatureTemplate interFeature = templates.new InterpolationIndicatorFeature();
		log.info("Interpolation features:");
		log.info(interFeature.getName());
		return interFeature;		
	}
	

	public static List<TransFeatureTemplate> 
	getTransFeatures(boolean useBiasFeature) {
		POSFeatureTemplates templates = new POSFeatureTemplates();
		List<TransFeatureTemplate> transFeatures = new ArrayList<TransFeatureTemplate>();
		transFeatures.add(templates.new TransIndicatorFeature());
		if (useBiasFeature) {
			transFeatures.add(templates.new BiasIndicatorFeature());
		}
		log.info("Trans features:");
		for (TransFeatureTemplate feat : transFeatures) {
			log.info(feat.getName());
		}
		return transFeatures;
	}
	
	

	public static List<EmitFeatureTemplate> 
	getEmitFeatures(boolean useStandardFeatures, 
			int lengthNGramSuffixFeature,
			boolean uTd, Map<String, Integer> wi, 
			int[][] td,
			int[][] tM,
			Map<String, String> noahsFeatures,
			Map<String, double[]> distSimTable,
			String[] namesArray) {		
		POSFeatureTemplates templates = new POSFeatureTemplates();
		List<EmitFeatureTemplate> emitFeatures = new ArrayList<EmitFeatureTemplate>();
		
		emitFeatures.add(templates.new EmitIndicatorFeature(uTd, wi, td, tM));
		
		// -templates
		if (useStandardFeatures) {
			
			// Shape / orthographic
			emitFeatures.add(templates.new InitialCapitalIndicatorFeature(uTd, wi, td, tM));
			emitFeatures.add(templates.new CapitalizationFeatures(uTd, wi, td, tM));
			emitFeatures.add(templates.new ContainsHyphenIndicatorFeature(uTd, wi, td, tM));
			emitFeatures.add(templates.new ContainsDigitIndicatorFeature(uTd, wi, td, tM));
			for (int i = 1; i <= lengthNGramSuffixFeature; ++i) {
				emitFeatures.add(templates.new NGramSuffixIndicatorFeature(uTd, wi, td, tM, i));
			}
			
			// Twitter specific
			emitFeatures.add(templates.new StartsWithAtSymbolIndicatorFeature(uTd, wi, td, tM));
			emitFeatures.add(templates.new StartsWithHashSymbolIndicatorFeature(uTd, wi, td, tM));
			emitFeatures.add(templates.new ContainsHTTPIndicatorFeature(uTd, wi, td, tM));
			emitFeatures.add(templates.new RTIndicatorFeature(uTd, wi, td, tM));
			
			// These 3 replace "OldMetaphoneFeatures"
			emitFeatures.add(templates.new POSDictFeatures(uTd, wi, td, tM));
			emitFeatures.add(templates.new MetaphoneLexical(uTd, wi, td, tM));
			emitFeatures.add(templates.new MetaphonePOSProjection(uTd, wi, td, tM));
			
			// names feature
			if (namesArray != null) {
				emitFeatures.add(templates.new NameIndicatorFeature(uTd, wi, td, tM, namesArray));
			}
			
			// distributional similarity features
			if (distSimTable != null) {
				emitFeatures.add(templates.new DistSimFeature(uTd, wi, td, tM, distSimTable));
			}
//			emitFeatures.add(templates.new OldMetaphoneFeatures(uTd, wi, td, tM));
//			emitFeatures.add(templates.new VariousLexicalAspectsIndicatorFeature(uTd, wi, td, tM, noahsFeatures));
			
		}
		log.info("Emit features:");
		for (EmitFeatureTemplate feat : emitFeatures) {
			log.info(feat.getName());
		}
		return emitFeatures;
	}	

	public static boolean isEmissionValid(boolean useTagDictionary, 
			String word, 
			Map<String, Integer> wordToIndex, 
			int[][] tagDictionary,
			int label,
			int[][] tagMapping) {
		if (!useTagDictionary) {
			return true;
		}		
		word = word.toLowerCase();
		if (!wordToIndex.containsKey(word)) {
			return true;
		}
		int index = wordToIndex.get(word);
		if (index >= tagDictionary.length) {
			return true;
		}
		int[] allowedTags = tagDictionary[index];
		if (allowedTags == null) {
			return true;
		}
		Set<Integer> mappedTags 
				= new HashSet<Integer>();
		for (int tag: allowedTags) {
			if(tagMapping[tag] == null) {
				continue;
			}
			for (int i = 0; i < tagMapping[tag].length; i++) {
				mappedTags.add(tagMapping[tag][i]);
			}
		}
		int size = mappedTags.size();
		if (size == 0) {
			return true;
		}
		Integer[] expandedTags = new Integer[size];
		mappedTags.toArray(expandedTags);
		Arrays.sort(expandedTags);
		if (Arrays.binarySearch(expandedTags, label) >= 0) {
			return true;
		} else {
			return false;
		}
	}
}
