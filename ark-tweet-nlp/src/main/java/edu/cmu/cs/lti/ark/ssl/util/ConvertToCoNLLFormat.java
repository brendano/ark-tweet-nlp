package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;

import cmu.arktweetnlp.util.BasicFileIO;

public class ConvertToCoNLLFormat {
	public static final String[] inputArr = {"wsj-02-21.MRG.MST.suited", 
									"wsj-22.MRG.MST.suited",
									"wsj-23.MRG.MST.suited"};
	
	public static final String[] outputArr = 
		{"english_proj_train.conll", 
		"english_proj_dev.conll",
		"english_proj_test.conll"};
	
	public static void main(String[] args) {
		allTags();
	}
	
	public static void allTags() {
		String inputFile = "/usr2/dipanjan/experiments/SSL/DepGraphBasedSSL/data/AP_1m.all.lemma.tags";
		String outputFile ="/usr2/dipanjan/experiments/SSL/DepGraphBasedSSL/data/AP_1m.conll";
		BufferedReader bReader = BasicFileIO.openFileToRead(inputFile);
		String line = BasicFileIO.getLine(bReader);
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outputFile);
		while (line != null) {
			ArrayList<String> toks = ProjectAlignedTags.getTokens(line);
			int numTokens = new Integer(toks.get(0));
			String outLine = "";
			for (int i = 0; i < numTokens; i++) {
				outLine = (i+1) + "\t" + toks.get(i+1).toLowerCase() + "\t";
				outLine += toks.get(i+1).toLowerCase() + "\t" + toks.get(i+1+numTokens) + "\t" + toks.get(i+1+numTokens) + "\t";
				outLine += "_\t" + toks.get(i + 1 + 3*numTokens) + "\t" + toks.get(i + 1 + 2*numTokens);
				BasicFileIO.writeLine(bWriter, outLine);
			}			
			BasicFileIO.writeLine(bWriter, "");
			line = BasicFileIO.getLine(bReader);
		}		
		BasicFileIO.closeFileAlreadyRead(bReader);
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}
	public static void mstSuited(String[] args) {
		String inputDirectory = "/usr2/dipanjan/experiments/SSL/DepGraphBasedSSL/data";
		String outputDirectory = 
			"/usr2/dipanjan/experiments/SSL/TurboParser/languages/english_wsj/data";
		for (int i = 0; i < 3; i++) {
			String inputFile = inputDirectory + "/" + inputArr[i];
			String outputFile = outputDirectory + "/" + outputArr[i];
			BufferedReader bReader = BasicFileIO.openFileToRead(inputFile);
			BufferedWriter bWriter = BasicFileIO.openFileToWrite(outputFile);
			String line1 = BasicFileIO.getLine(bReader);
			while (line1 != null) {
				ArrayList<String> words = ProjectAlignedTags.getTokens(line1);
				String line2 = BasicFileIO.getLine(bReader);
				ArrayList<String> poss = ProjectAlignedTags.getTokens(line2);
				String line3 = BasicFileIO.getLine(bReader);
				ArrayList<String> labels = ProjectAlignedTags.getTokens(line3);
				String line4 = BasicFileIO.getLine(bReader);
				ArrayList<String> parents = ProjectAlignedTags.getTokens(line4);
				int size = words.size();
				for (int j = 0; j < size; j++) {
					String outLine = "" + (j+1) + "\t";
					outLine += words.get(j).toLowerCase() + "\t" + words.get(j).toLowerCase() + "\t";
					outLine += poss.get(j) + "\t" + poss.get(j) + "\t";
					outLine += "_\t" + parents.get(j) + "\t" + labels.get(j);
					BasicFileIO.writeLine(bWriter, outLine);
				}
				BasicFileIO.writeLine(bWriter, "");
				BasicFileIO.getLine(bReader);
				line1 = BasicFileIO.getLine(bReader);
			}
			BasicFileIO.closeFileAlreadyRead(bReader);
			BasicFileIO.closeFileAlreadyWritten(bWriter);
		}
	}
}