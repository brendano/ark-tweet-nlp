package edu.cmu.cs.lti.ark.ssl.pos;
import java.io.*;
import java.util.*;

import org.apache.commons.codec.language.Metaphone;

import fig.basic.Pair;

public class PhoneticDB {
	public HashMap<String, String> metaphone2pos;
	public HashMap<String, String> word2pos;
	
	public PhoneticDB() {
		metaphone2pos = new HashMap<String,String>();
		word2pos = new HashMap<String,String>();
	}
	public void loadData(String tabFilePath) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(tabFilePath));
			String line;
			while((line = in.readLine()) != null) {			    
			    line = line.trim();
			    String[] parts = line.split("\t");
			    String key = parts[0];
			    String pos = parts[1];
			    if (key.contains("***")) {
			    	key = key.replace("***", "");
			    	metaphone2pos.put(key, pos);
			    } else {
			    	word2pos.put(key, pos);
			    }
			                       
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
