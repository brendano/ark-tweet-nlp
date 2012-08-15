package edu.cmu.cs.lti.ark.ssl.util;

import edu.cmu.cs.lti.ark.ssl.pos.PennTreeBankPOSSequenceReader;
import fig.basic.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import cmu.arktweetnlp.util.BasicFileIO;



public class TabSeparatedPOSPrinting {
	
	public static void main(String[] args) {
		String[] langs = {"bg", "da", "de", "es", "it", "nl", "pt", "sl", "sv", "tr"};
		for (int i = 0; i < langs.length; i++) {
			System.out.println(langs[i]);
			convertBackToTnTFormat(langs[i]);
		}
	}
	
	private static void convertBackToTnTFormat(String lang) {
		String directory = "/mal2/dipanjan/experiments/SSL/UnsupervisedPOS/data/epaligned";
		String file = lang + ".projected.tpos";
		String inFile = directory + "/" + file;
		String outFile = directory + "/" + file + ".tab";
		String line = null;
		BufferedReader bReader = BasicFileIO.openFileToRead(inFile);
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
		line = BasicFileIO.getLine(bReader);
		while (line != null) {
			line = line.trim();
			ArrayList<String> toks = ProjectAlignedTags.getTokens(line);
			for (int i = 0; i < toks.size(); i++) {
				String tok = toks.get(i).trim();
				int li = tok.lastIndexOf("_");
				String word = tok.substring(0, li);
				String POS = tok.substring(li+1);
				BasicFileIO.writeLine(bWriter, word + "\t" + POS);
			}
			BasicFileIO.writeLine(bWriter, "");
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}
	
	private static void printDummyEuroparlData(String lang) {
		String directory = "/mal2/dipanjan/experiments/SSL/UnsupervisedPOS/data/epaligned";
		String file = "";
		if (lang.equals("tr")) {
			file = "original.filt.en.replaced";
		} else {
			file = "europarl-v6." + lang + "-en.en.tokenized.lc.filt.replaced"; 
		}
		String inFile = directory + "/" + file;
		String outFile = directory + "/" + file + ".in.tab";
		String line = null;
		BufferedReader bReader = BasicFileIO.openFileToRead(inFile);
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
		line = BasicFileIO.getLine(bReader);
		while (line != null) {
			line = line.trim();
			ArrayList<String> toks = ProjectAlignedTags.getTokens(line);
			for (int i = 0; i < toks.size(); i++) {
				BasicFileIO.writeLine(bWriter, toks.get(i) + "\tNN");
			}
			BasicFileIO.writeLine(bWriter, "");
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}
	
	private static void printQuestions() {
		String inFile = "/mal2/dipanjan/experiments/SSL/data/4000qs.cleaned.dev";
		String outFile = "/mal2/dipanjan/experiments/SSL/data/4000qs.cleaned.dev.tab";
		
		Collection<Pair<List<String>, List<String>>> sequences =
			PennTreeBankPOSSequenceReader.readPOSSeqences(inFile, 
					Integer.MAX_VALUE, 
					Integer.MAX_VALUE);
		
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
		for (Pair<List<String>, List<String>> pair: sequences) {
			List<String> sent = pair.getFirst();
			List<String> pos = pair.getSecond();
			int i = 0;
			for (String w : sent) {
				BasicFileIO.writeLine(bWriter, w + "\t" + pos.get(i));
				++i;
			}
			BasicFileIO.writeLine(bWriter, "");
		}
	}
}