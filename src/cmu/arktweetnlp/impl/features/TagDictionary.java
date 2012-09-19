package cmu.arktweetnlp.impl.features;
import java.io.*;
import java.util.*;

import org.apache.commons.codec.language.Metaphone;

import cmu.arktweetnlp.util.BasicFileIO;

public class TagDictionary {
	public static Map<String, List<String>> WORD_TO_POS;

	static {
		WORD_TO_POS = null;
		
        try {
			WORD_TO_POS = loadData();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public static TagDictionary instance() {
        return new TagDictionary();
	}

	static Map<String, List<String>> loadData() throws IOException {
//		log.info("loading POS tag dictionary...");
		Metaphone _metaphone = new Metaphone();
		_metaphone.setMaxCodeLen(100);
        HashMap<String, List<String>> wordToPos  =
                new HashMap<String, List<String>>();
		BufferedReader in = BasicFileIO.getResourceReader("/cmu/arktweetnlp/tagdict.txt");
		String line;
        try {
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
                wordToPos.put(word, Collections.unmodifiableList(arr));
}
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Collections.unmodifiableMap(wordToPos);
	}
	public static void main(String args[]) {
		instance();
	}
}
