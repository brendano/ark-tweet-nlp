package cmu.arktweetnlp.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cmu.arktweetnlp.impl.Sentence;
import cmu.arktweetnlp.util.BasicFileIO;


/**
 * Read a simplified version of the CoNLL format.  Two columns
 *   Word \t POSTag
 *
 * With a blank line separating sentences.
 * 
 * Returns 'null' for the input record string
 */
public class CoNLLReader {
	public static ArrayList<Sentence> readFile(String filename) throws IOException {
		BufferedReader reader = BasicFileIO.openFileToReadUTF8(filename);
		ArrayList<Sentence> sentences = new ArrayList<Sentence>();

		ArrayList<String> curLines = new ArrayList<String>();
		String line;
		while ( (line = reader.readLine()) != null ) {
			if (line.matches("^\\s*$")) {
				if (curLines.size() > 0) {
					// Flush
					sentences.add(sentenceFromLines(curLines));
					curLines.clear();
				}
			} else {
				curLines.add(line);
			}
		}
		if (curLines.size() > 0) {
			sentences.add(sentenceFromLines(curLines));
		}
		return sentences;
	}
//	private static Pair<String,Sentence> wrap(Sentence s) {
//		return new Pair<String,Sentence>(null, s);
//	}

	private static Sentence sentenceFromLines(List<String> lines) {
		Sentence s = new Sentence();

		for (String line : lines) {
			String[] parts = line.split("\t");
			assert parts.length == 2;
			s.tokens.add( parts[0].trim() );
			s.labels.add( parts[1].trim() );
		}
		//        System.out.println(s);
		return s;
	}
}
