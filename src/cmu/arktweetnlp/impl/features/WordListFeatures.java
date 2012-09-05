package cmu.arktweetnlp.impl.features;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import cmu.arktweetnlp.Twokenize;
import cmu.arktweetnlp.impl.features.FeatureExtractor.FeatureExtractorInterface;
import cmu.arktweetnlp.impl.features.FeatureExtractor.PositionFeaturePairs;
import cmu.arktweetnlp.util.BasicFileIO;

public class WordListFeatures {

	public static class POSTagDict implements FeatureExtractorInterface {
		Pattern URL = Pattern.compile(Twokenize.url);
		Pattern letter = Pattern.compile("[A-Za-z]{3,}");
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
	
				List<String> poses = TagDictionary.WORD_TO_POS.get(tok);
				if (poses == null && letter.matcher(tok).find() && (!URL.matcher(tok).matches())) {
					String normtok = FeatureUtil.normalizecap(tok);
					poses = TagDictionary.WORD_TO_POS.get(normtok);
					if (poses==null) {
						ArrayList<String> fuzz = new ArrayList<String>(); 
						fuzz.addAll(FeatureUtil.fuzztoken(normtok, true));
						fuzz.addAll(FeatureUtil.fuzztoken(FeatureUtil.normalize(tok), true));
				    	for (String f:fuzz){
				    		poses = TagDictionary.WORD_TO_POS.get(f);
				    		if (poses != null){
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

	/*
	 * If you add a new list, make sure to run "mvn install -q" so it's copied
	 * to the target folder or else you'll get a null pointer exception
	 */
	public static class Listofnames implements FeatureExtractorInterface {
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

	public static class MetaphonePOSDict implements FeatureExtractorInterface {
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
				if(tok.length()>1){
					//tok=tok.replace("4", "four").replace("2", "two").replace("3","three");
					String metaphone_word = MetaphoneFeatures.getMetaphone().encode(tok);
					String key = String.format("**MP**%s", metaphone_word);
					if (TagDictionary.WORD_TO_POS.containsKey(key)) {
						List<String> poses = TagDictionary.WORD_TO_POS.get(key);
						for (String pos : poses) {
							pairs.add(t, "metaph_POSDict|"+pos);
						}    				
					}
				}
			}
		}
	}

	private static HashSet<String> initDict(String dict) {
		BufferedReader bReader = BasicFileIO.getResourceReader("/cmu/arktweetnlp/" + dict);
		HashSet<String> dictset = new HashSet<String>();
		String line=BasicFileIO.getLine(bReader);
		while(line != null){
			dictset.add(line.toLowerCase());
			line = BasicFileIO.getLine(bReader);
		}
		return dictset;
	}

}
