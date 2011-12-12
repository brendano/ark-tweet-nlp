package edu.cmu.cs.lti.ark.ssl.pos;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.codec.language.Metaphone;

import fig.basic.Pair;

public class TagDictionary {
	public HashMap<String, ArrayList> word2poses;
//	public HashMap<String, ArrayList> metaphone2poses;
	
	public TagDictionary() {
//		metaphone2poses = new HashMap();
		word2poses = new HashMap();
	}
	private static TagDictionary _instance = null;
	public static TagDictionary instance() {
		if (_instance == null) {
			_instance = new TagDictionary();
			_instance.loadData("lib/tagdict.txt");
		}
		return _instance;
	}
	private static Logger log = Logger.getLogger(POSFeatureTemplates.class.getCanonicalName());

	public void loadData(String tabFilePath) {
		log.info("loading POS tag dictionary...");
		Metaphone _metaphone = new Metaphone();
		_metaphone.setMaxCodeLen(100);

		try {
			BufferedReader in = new BufferedReader(new FileReader(tabFilePath));
			String line;
			while((line = in.readLine()) != null) {			    
			    String[] parts = line.trim().split("\t");
			    if (parts.length != 2) {
			    	System.out.println(parts.length);
			    	System.out.println("wtf " + line.trim() + " | " + parts.length);
			    	continue;
			    }
			    String word = parts[0];
			    String poses = parts[1].trim();
			    ArrayList<String> arr = new ArrayList(); //new String[poses.length()];
			    for (int i=0; i < poses.length(); i++) {
			    	arr.add(poses.substring(i,i+1));
			    }
			    	
			    word2poses.put(word, arr);			    	
		    }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void main(String args[]) {
		instance();
	}
}
