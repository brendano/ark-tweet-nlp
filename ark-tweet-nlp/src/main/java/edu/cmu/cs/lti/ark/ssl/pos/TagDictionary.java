package edu.cmu.cs.lti.ark.ssl.pos;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.codec.language.Metaphone;

public class TagDictionary {
	public final static Map<String, List<String>> WORD_TO_POS;
    static {
        WORD_TO_POS = loadData();
    }
    //FIXME(alexander) get rid of this
	public static TagDictionary instance() {
        return new TagDictionary();
	}
	private static Logger log = Logger.getLogger(POSFeatureTemplates.class.getCanonicalName());

	static Map<String, List<String>> loadData() {
//		log.info("loading POS tag dictionary...");
		Metaphone _metaphone = new Metaphone();
		_metaphone.setMaxCodeLen(100);
        HashMap<String, List<String>> wordToPos  =
                new HashMap<String, List<String>>();
			BufferedReader in = new BufferedReader(
                   new InputStreamReader(
                           TagDictionary.class.getResourceAsStream("tagdict.txt")));
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
