package edu.cmu.cs.lti.ark.ssl.pos;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import edu.cmu.cs.lti.ark.ssl.util.BasicFileIO;



public class UnlabeledSentencesReader {
	
	public static Collection<List<String>> readSequences(String path, int numSequences, int maxSequenceLength) {
		Collection<List<String>> sequences = new ArrayList<List<String>>();
		try {
			BufferedReader reader = BasicFileIO.openFileToRead(path);
			String line = BasicFileIO.getLine(reader);
			int countLines = 0;
			while (line != null) {
				line = line.trim();
				String[] toks = TabSeparatedFileReader.getToks(line);
				if (toks.length > maxSequenceLength) {
					line = BasicFileIO.getLine(reader);
					continue;
				}
				List<String> seq = Arrays.asList(toks);
				sequences.add(seq);
				countLines++;
				if (countLines >= numSequences) {
					break;
				}
				line = BasicFileIO.getLine(reader);
			}
			BasicFileIO.closeFileAlreadyRead(reader);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sequences;
	}
	
	
}