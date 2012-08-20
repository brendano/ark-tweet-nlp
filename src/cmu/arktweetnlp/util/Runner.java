package cmu.arktweetnlp.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cmu.arktweetnlp.Twokenize;
import com.twitter.Regex;


public class Runner {
	public static Pattern URL = Pattern.compile(Twokenize.OR(Twokenize.url, Twokenize.Email));
	public static Pattern justbase = Pattern.compile("(?!www\\.|ww\\.|w\\.|@)[a-zA-Z0-9]+\\.[A-Za-z0-9\\.]+"); 
	
	public static String normalize(String str) {
	    str = str.toLowerCase();
		if (URL.matcher(str).matches()){	    	
	    	String base = "";
	    	Matcher m = justbase.matcher(str);
	    	if (m.find())
	    		base=m.group().toLowerCase();
	    	return "<URL-"+base+">";
	    }
	    if (Regex.VALID_MENTION_OR_LIST.matcher(str).matches())
	    	return "<@MENTION>";
	    return str;
    }
	//same as normalize but retains capitalization
	public static String normalizecap(String str) {
		if (URL.matcher(str).matches()){	    	
	    	String base = "";
	    	Matcher m = justbase.matcher(str);
	    	if (m.find())
	    		base=m.group().toLowerCase();
	    	return "<URL-"+base+">";
	    }
	    if (Regex.VALID_MENTION_OR_LIST.matcher(str).matches())
	    	return "<@MENTION>";
	    return str;
    }
	public static ArrayList<String> normalize(List<String> toks){
		ArrayList<String> normtoks = new ArrayList<String>(toks.size());
		for (String s:toks){
			normtoks.add(normalize(s));
		}
		return normtoks;
	}
	public static ArrayList<String> normalizecap(List<String> toks){
		ArrayList<String> normtoks = new ArrayList<String>(toks.size());
		for (String s:toks){
			normtoks.add(normalizecap(s));
		}
		return normtoks;
	}
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Supply the file to be tokenized.");
		} else {
			String Filename = args[0];
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(Filename), "UTF-8"));
			BufferedWriter writer;
			if (args.length>1){
				writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(args[1]), "UTF-8"));
			}
			else{
				writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream("newer" + Filename), "UTF-8"));
			}
			String line;
			while ((line = reader.readLine()) != null) {
				List<String> tline = Twokenize.tokenizeRawTweetText(line);
				for (String str : tline) {
					writer.write(str + " ");
				}
				writer.write("\n");
				writer.flush();
			}
			writer.close();
		}
	}	
}