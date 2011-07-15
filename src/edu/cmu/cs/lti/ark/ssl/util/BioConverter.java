package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Collection;
import java.util.List;

import edu.cmu.cs.lti.ark.ssl.pos.TabSeparatedFileReader;
import edu.cmu.cs.lti.ark.ssl.pos.UnlabeledSentencesReader;


public class BioConverter {

	public static void main(String[] args) {
		unlabeled();
	}	

	public static void unlabeled() {
		String in = "/mal2/dipanjan/experiments/SSL/data/bio/unlabeled/biomed_ul.100k";
		String out = "/mal2/dipanjan/experiments/SSL/data/unlabeled_bio.txt";
		
		Collection<List<String>> sequences =
			UnlabeledSentencesReader.readSequences(in, Integer.MAX_VALUE, Integer.MAX_VALUE);	
		
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(out);
		for (List<String> sequence: sequences) {
			String line = "";
			for (String word: sequence) {
				line += word + " ";
			}
			line = line.trim();
			line = line.replace("(", "-LRB-");
			line = line.replace(")", "-RRB-");
			line = line.replace("[", "-LSB-");
			line = line.replace("]", "-RSB-");
			line = line.replace("{", "-LCB-");
			line = line.replace("}", "-RCB-");
			BasicFileIO.writeLine(bWriter, line);
		}		
		BasicFileIO.closeFileAlreadyWritten(bWriter);		
	}
	
	public static void labeled() {
		String in = "/mal2/dipanjan/experiments/SSL/data/bio/labeled_full/" +
		"onco_train.561";
		String out =  "/mal2/dipanjan/experiments/SSL/data/bio/labeled_full/" +
		"bio.test.tab";

		BufferedReader bReader = BasicFileIO.openFileToRead(in);
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(out);
		String line = BasicFileIO.getLine(bReader);
		String posLine = BasicFileIO.getLine(bReader);
		int count = 0;
		while (line != null && posLine != null) {
			line = line.trim();
			line = line.replace("(", "-LRB-");
			line = line.replace(")", "-RRB-");
			line = line.replace("[", "-LSB-");
			line = line.replace("]", "-RSB-");
			line = line.replace("{", "-LCB-");
			line = line.replace("}", "-RCB-");
			String[] toks = TabSeparatedFileReader.getToks(line.trim());
			String[] posTags = TabSeparatedFileReader.getToks(posLine.trim());

			int len = toks.length;
			for (int i = 0; i < len; i++) {
				BasicFileIO.writeLine(bWriter, toks[i]+"\t"+posTags[i]);
			}			
			BasicFileIO.writeLine(bWriter, "");
			line = BasicFileIO.getLine(bReader);
			posLine = BasicFileIO.getLine(bReader);
			count++;
		}
		System.out.println("Total number of sentences:" + count);
		BasicFileIO.closeFileAlreadyRead(bReader);
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}

}