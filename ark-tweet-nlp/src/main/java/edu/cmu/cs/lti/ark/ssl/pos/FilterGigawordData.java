package edu.cmu.cs.lti.ark.ssl.pos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cmu.arktweetnlp.util.BasicFileIO;

import fig.basic.Pair;


public class FilterGigawordData {
	
	public static void main(String[] args) {
		String gigaWordData = 
			"/Users/dipanjand/work/fall2010/SSL/pos_tagging_data/"	+ 
			"questions_starting_with_wh_words_random_wsj.txt";
		
		String gigaWordDataFiltered = 
			"/Users/dipanjand/work/fall2010/SSL/pos_tagging_data/"	+ 
			"questions_starting_with_wh_words_random_wsj_filtered.txt";
		
		
		String questionBankData = 
			"/Users/dipanjand/work/fall2010/SSL/pos_tagging_data/"	+ 
			"4000qs.cleaned.txt";
		
		Collection<Pair<List<String>, List<String>>> sequences =
			PennTreeBankPOSSequenceReader.readPOSSeqences(questionBankData, 
					40000, 
					Integer.MAX_VALUE);
		
		List<String> qSentences = new ArrayList<String>();
		for (Pair<List<String>, List<String>> sequence: sequences) {
			List<String> sentList = sequence.getFirst();
			String sent = "";
			for (String word: sentList) {
				sent += word + " ";
			}
			sent = sent.trim().toLowerCase();
			qSentences.add(sent);
		}
		
		System.out.println("Finished reading questions.");
		String[] qSentArray = new String[qSentences.size()];
		qSentences.toArray(qSentArray);
		Arrays.sort(qSentArray);
		
		BufferedReader gReader = BasicFileIO.openFileToRead(gigaWordData);
		String line = BasicFileIO.getLine(gReader);
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(gigaWordDataFiltered);
		int count = 0;
		while (line != null) {
			String orgLine = line.trim();
			line = line.trim().toLowerCase();
			String[] toks = TabSeparatedFileReader.getToks(line);
			line = "";
			for (String tok: toks) {
				line += tok + " ";
			}
			line = line.trim();
			if (Arrays.binarySearch(qSentArray, line) >= 0) {
				System.out.println(line);
			} else {
				BasicFileIO.writeLine(bWriter, orgLine);
			}
			count++;
//			if (count % 100 == 0) {
//				System.out.print(count + " ");
//			}
//			if (count % 1000 == 0) {
//				System.out.println();
//			}
			line = BasicFileIO.getLine(gReader);
		}
		BasicFileIO.closeFileAlreadyRead(gReader);
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}	
}
