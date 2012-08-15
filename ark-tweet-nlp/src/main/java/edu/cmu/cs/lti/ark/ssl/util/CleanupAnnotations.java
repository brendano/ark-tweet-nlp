package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.StringTokenizer;

import cmu.arktweetnlp.util.BasicFileIO;

public class CleanupAnnotations {
	public static void main(String[] args) {
		String file = args[0];
		String outFile = args[1];
		String idFile = args[2];
		BufferedReader bReader = BasicFileIO.openFileToRead(file);
		BufferedWriter bWriter = BasicFileIO.openFileToWrite(outFile);
		BufferedWriter bWriter2 = BasicFileIO.openFileToWrite(idFile);
		
		String line = BasicFileIO.getLine(bReader);
		int countValidExamples = 0;
		while (line != null) {
			StringTokenizer st = new StringTokenizer(line.trim(), "\t", true);
			String id = st.nextToken();
			st.nextToken();
			String next = st.nextToken();
			if (next.equals("\t")) {
				String sent = st.nextToken().trim();
				st.nextToken();
				String pos = st.nextToken().trim();
				System.out.println(sent);
				System.out.println(pos);
				String[] sentToks = sent.split(" ");
				String[] posToks = pos.split(" ");
				boolean flag = true;
				for (int i = 0; i < sentToks.length; i++) {
					String tok = sentToks[i].trim();
					if (tok.equals("")) {
						flag = false;
					}
				}
				if (!flag) {
					System.out.println("Problem with spaces. Continuing.");
					line = BasicFileIO.getLine(bReader);
					continue;
				}
				if (sentToks.length != posToks.length) {
					System.out.println("Problem with the number of tokens. Continuing.");
					line = BasicFileIO.getLine(bReader);
					continue;
				}
				int len = sentToks.length;
				for (int i = 0; i < len; i++) {
					System.out.println(sentToks[i] + "\t" + posToks[i]);
					BasicFileIO.writeLine(bWriter, sentToks[i] + "\t" + posToks[i]);
				}
				System.out.println();
				BasicFileIO.writeLine(bWriter, "");
				BasicFileIO.writeLine(bWriter2, id);
				countValidExamples++;
			}
			line = BasicFileIO.getLine(bReader);
		}
		BasicFileIO.closeFileAlreadyRead(bReader);
		BasicFileIO.closeFileAlreadyWritten(bWriter);
		BasicFileIO.closeFileAlreadyWritten(bWriter2);
		System.out.println("Total number of valid examples:" + countValidExamples);
	}
}