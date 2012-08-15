package cmu.arktweetnlp.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import cmu.arktweetnlp.Twokenize;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.twitter.Regex;


public class Runner {
	public static Pattern URL = Pattern.compile(Twokenize.url);
	public static Pattern notChinese = Pattern.compile("[\\u0400-\\u1D7F\\u2E80-\\uDFFF\\uF900-\\uFAFF\\uFB50-\\uFDFF\\uFE20-\\uFE4F\\uFE70-\\uFEFF]{2}");
	public static Pattern justbase = Pattern.compile("(?!www\\.|ww\\.|w\\.)[a-zA-Z0-9]+\\.[A-Za-z0-9\\.]+"); 
	public static void oldmain(String[] args) throws IOException, InterruptedException {
		if (args.length < 1) {
			System.out.println("Supply the file to be tokenized.");
		} else {
			final JsonFactory factory = new JsonFactory();
			for(String files:args){
				File f = new File(files);
				if (! new File("out/"+f.getName()).exists()){
					GZIPInputStream gz = new GZIPInputStream(new FileInputStream(f));
					JsonParser jp = factory.createJsonParser(gz);
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream("out/"+f.getName()), "UTF-8"));
					String line;
					if(jp.nextToken()==JsonToken.START_OBJECT){
						while ((line = getLine(jp)) != null && !line.isEmpty()) {
							if(!(notChinese.matcher(line).find())){
								writer.write(normalize(Twokenize.tokenizeToString(line)));
								writer.newLine();
							}
						}
					}
					gz.close();
					jp.close(); 
			        writer.close();
				}
			}
		}
	}
	
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
	public static void JSONParse(String[] args) throws Exception{
        JsonFactory factory = new JsonFactory();
        JsonParser jp = factory.createJsonParser(new File(args[0]));
        BufferedWriter out;
        if(args.length==2){
        	out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8"));}
        else{
        	out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream("tweets.txt"), "UTF-8"));}
        String line;
        jp.nextValue();
        while((line = BasicFileIO.getLine(jp)) != null) {
        	out.write(line);
        }
        jp.close();  
        out.close();
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
				List<String> tline = Twokenize.tokenizeForTagger(line);
				for (String str : tline) {
					writer.write(str + " ");
				}
				writer.write("\n");
				writer.flush();
			}
			writer.close();
		}
	}	
	public static String getLine(JsonParser jParse) {
	    //returns the next "text" field or null if none left
		//boolean english = false;
	    try {
	        while(jParse.getText()!=null){
	            if ("entities".equals(jParse.getCurrentName())
	               |"retweeted_status".equals(jParse.getCurrentName())
	               |"user".equals(jParse.getCurrentName())) {
	                jParse.nextToken();
	                jParse.skipChildren();
	            }    
	            if ("text".equals(jParse.getCurrentName())) {
	                jParse.nextToken(); // move to value
	                String tweet = jParse.getText();
	                //if(english)
	                return tweet;
	            }
	            jParse.nextToken();
	        }
	    } catch(JsonParseException e){
	        //e.printStackTrace();
	        System.err.println("Error parsing JSON.");
	        return null;
	    }
	    catch(IOException e) {
	        //e.printStackTrace();
	        System.err.println("Could not read line from file.");
	        return null;
	    }
	
	    return null;	//jParse is null (EOF)	
	}
}