package cmu.arktweetnlp.impl.features;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import cmu.arktweetnlp.Twokenize;
import cmu.arktweetnlp.impl.DefaultPatternContext;
import cmu.arktweetnlp.impl.features.FeatureExtractor.FeatureExtractorInterface;
import cmu.arktweetnlp.impl.features.FeatureExtractor.PositionFeaturePairs;

public class MiscFeatures {
	
	public static class NextWord implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			ArrayList<String> normtoks = FeatureUtil.normalize(tokens);
			for (int t=0; t < tokens.size()-1; t++) {
				pairs.add(t, "nextword|"+tokens.get(t+1), .5);
				pairs.add(t, "nextword|"+normtoks.get(t+1), .5);
				pairs.add(t, "currnext|"+normtoks.get(t)+"|"+normtoks.get(t+1));
			}
			pairs.add(tokens.size()-1, "currnext|"+normtoks.get(tokens.size()-1)+"|<END>");
			pairs.add(tokens.size()-1, "nextword|<END>");
		}
	}
	public static class Next2Words implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			if (tokens.size()>1){
				ArrayList<String> normtoks = FeatureUtil.normalize(tokens);
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
	public static class PrevWord implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			ArrayList<String> normtoks = FeatureUtil.normalize(tokens);
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
	public static class Prev2Words implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) { 	
			if (tokens.size()>1){
				ArrayList<String> normtoks = FeatureUtil.normalize(tokens);
				pairs.add(1, "prev2words|<START>|"+normtoks.get(0)+"|"+normtoks.get(1));
				for (int t=2; t < tokens.size(); t++) {
					pairs.add(t, "prev2words|"+tokens.get(t-2)+"|"+tokens.get(t-1));
					pairs.add(t, "prev2words|"+normtoks.get(t-2)+"|"+normtoks.get(t-1));
				}
			}
		}
	}
	public static class PrevNext implements FeatureExtractorInterface{
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			ArrayList<String> normtoks = FeatureUtil.normalize(tokens);
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

	public static class CapitalizationFeatures implements FeatureExtractorInterface {	
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
	public static class SimpleOrthFeatures implements FeatureExtractorInterface {
		public Pattern hasDigit = Pattern.compile("[0-9]");
		/** TODO change to punctuation class, or better from Twokenize **/
		//Pattern allPunct = Pattern.compile("^[^a-zA-Z0-9]*$");
		Pattern allPunct = Pattern.compile("^\\W*$");
		Pattern emoticon = Pattern.compile(DefaultPatternContext.emoticon);
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
					String[] splithyph = FeatureUtil.normalize(tok).split("-", 2);
					//pairs.add(t, "preHyph|"+splithyph[0]);//quote -Ben Franklin
					//pairs.add(t, "postHyph|"+splithyph[1]);//-esque
					for (String part:splithyph){
						pairs.add(t, "hyph|" + part);
					}
				}
			}
		}    
	}
	public static class URLFeatures implements FeatureExtractorInterface {	
		Pattern validURL = Pattern.compile(DefaultPatternContext.url);
		Pattern validEmail = Pattern.compile(DefaultPatternContext.Email);
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
	public static class WordformFeatures implements FeatureExtractorInterface {
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = tokens.get(t);
				String normalizedtok=tok.replaceAll("[‘’´`]", "'").replaceAll("[“”]", "\"");
				pairs.add(t, "Word|" + normalizedtok);
				pairs.add(t, "Lower|" + FeatureUtil.normalize(normalizedtok));
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
	public static class NgramPrefix implements FeatureExtractorInterface {
		int ngram=3;
		public NgramPrefix(int i) {
			ngram=i;
		}
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = FeatureUtil.normalize(tokens.get(t).replaceAll("[‘’´`]", "'").replaceAll("[“”]", "\""));
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
	public static class NgramSuffix implements FeatureExtractorInterface {
		int ngram=3;
		public NgramSuffix(int i) {
			ngram=i;
		}
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < tokens.size(); t++) {
				String tok = FeatureUtil.normalize(tokens.get(t).replaceAll("[‘’´`]", "'").replaceAll("[“”]", "\""));
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
	public static class Positions implements FeatureExtractorInterface {	
		public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
			for (int t=0; t < Math.min(tokens.size(), 4); t++) {
				pairs.add(t, "t="+t);
			}
			for (int t=tokens.size()-1; t > Math.max(tokens.size()-4, -1); t--) {
				pairs.add(t, "t=-"+t);
			}
		}
	}


}
