package edu.cmu.cs.lti.ark.ssl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class EncodingConversion {
	public static void main(String[] args) {
		String path = 
			"/mal2/dipanjan/experiments/SSL/UnsupervisedPOS/data/turkish/original.eng";
		String outPath = 
			"/mal2/dipanjan/experiments/SSL/UnsupervisedPOS/data/turkish/original.en.utf8";
		try {	
			FileInputStream fi = new FileInputStream(path);
			InputStreamReader is = new InputStreamReader(fi, "ISO-8859-1");
			BufferedReader bReader = new BufferedReader(is);
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(outPath));
			String line = null;
			while ((line = bReader.readLine()) != null) {
				System.out.println(line);
				bWriter.write(line + "\n");
			}
			bReader.close();
			bWriter.close();
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}