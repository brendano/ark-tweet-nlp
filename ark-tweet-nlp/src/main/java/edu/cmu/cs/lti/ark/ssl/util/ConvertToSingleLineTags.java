package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.cmu.cs.lti.ark.ssl.pos.TabSeparatedFileReader;
import fig.basic.Pair;

public class ConvertToSingleLineTags {
	public static void main(String[] args) {
//		String[] langs = {"bg", "da", "de", "es", "it", "nl", "pt", "sl", "sv", "tr"};
//		for (int i = 0; i < langs.length; i++) {
//			System.out.println(langs[i]);
//			convertTnTOutput(langs[i]);
//		}	
		treebanks(args);
	}
	
	public static void convertTnTOutput(String lang) {
		String directory = "/mal2/dipanjan/experiments/SSL/UnsupervisedPOS/data/epaligned";
		String file = "";
		if (lang.equals("tr")) {
			file = "original.filt.en.replaced.out.tab";
		} else {
			file = "europarl-v6." + lang + "-en.en.tokenized.lc.filt.replaced.out.tab"; 
		}
		String inFile = directory + "/" + file;
		file = file.replace("out.tab", "");
		String outFile = directory + "/" + file + "tpos";
		BufferedReader bReader = BasicFileIO.openFileToRead(inFile);
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
		String line = BasicFileIO.getLine(bReader);
		String outLine = "";
		while (line != null) {
			line = line.trim();
			if (line.equals("")) {
				outLine = outLine.trim();
				BasicFileIO.writeLine(bWriter, outLine);
				outLine = "";
			} else {
				ArrayList<String> toks = ProjectAlignedTags.getTokens(line);
				if (toks.size() != 2) {
					System.out.println("Problem with lang:" + lang +" line:" + line);
					System.exit(-1);
				}
				outLine += toks.get(0) + "_" + toks.get(1) + " ";
			}
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		BasicFileIO.closeFileAlreadyWritten(bWriter);
	}
	
	public static void treebanks(String[] args) {
		Collection<Pair<List<String>, List<String>>>  gSequences =
			TabSeparatedFileReader.readPOSSeqences(args[0], 
					Integer.MAX_VALUE, 
					Integer.MAX_VALUE);
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(args[1]);
		for (Pair<List<String>, List<String>> p: gSequences) {
			List<String> tags = p.getFirst();
			String tagString = "";
			for (String tag: tags) {
				tagString += tag + " ";
			}
			tagString = tagString.trim();
			BasicFileIO.writeLine(bWriter, tagString);
		}
		BasicFileIO.closeFileAlreadyWritten(bWriter);		
	}
}