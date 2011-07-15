package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.lti.ark.ssl.pos.TabSeparatedFileReader;
import fig.basic.Pair;

public class ConvertTreebanks {
	public static void main(String[] args) {
		String inFile = args[0];
		String outFile = args[1];
		Collection<Pair<List<String>, List<String>>> col = 
			 TabSeparatedFileReader.readPOSSeqences(inFile, Integer.MAX_VALUE, Integer.MAX_VALUE);
		Iterator<Pair<List<String>, List<String>>> it = 
			col.iterator();	 
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
		while (it.hasNext()) {
			Pair<List<String>, List<String>> pair = it.next();
			String line = "";
			List<String> first = pair.getFirst();
			for (String s: first) {
				line += s + " ";
			}
			line = line.trim();
			BasicFileIO.writeLine(bWriter, line);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}
}