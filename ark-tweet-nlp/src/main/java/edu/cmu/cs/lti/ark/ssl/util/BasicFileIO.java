package edu.cmu.cs.lti.ark.ssl.util;

import java.io.*;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BasicFileIO {

	/*
	 * A logger for the class.
	 */
	private static Logger log = Logger.getLogger(BasicFileIO.class.getCanonicalName());

	public static BufferedReader openFileToRead(String file) {
		try {
			BufferedReader bReader = null;
			if (file.endsWith(".gz")) {
				bReader = new BufferedReader(new InputStreamReader(
						new GZIPInputStream(new FileInputStream(file))));
			} else {
				bReader = new BufferedReader(new FileReader(file));
			}
			return bReader;
		} catch (IOException e) {
			e.printStackTrace();
			log.severe("Could not open file:" + file);
			System.exit(-1);
		}
		return null;
	}

	public static BufferedWriter openFileToWrite(String file) {
		try {
			BufferedWriter bWriter = null;
			if (file.endsWith(".gz")) {
				bWriter = new BufferedWriter(new OutputStreamWriter(
						new GZIPOutputStream(new FileOutputStream(file))));
			} else {
				bWriter = new BufferedWriter(new FileWriter(file));
			}
			return bWriter;
		} catch (IOException e) {
			e.printStackTrace();
			log.severe("Could not open file for writing:" + file);
			System.exit(-1);
		}
		return null;
	}

	public static void closeFileAlreadyRead(BufferedReader bReader) {
		try {
			bReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			log.severe("Could not close file.");
			System.exit(-1);
		}
	}

	public static void closeFileAlreadyWritten(BufferedWriter bWriter) {
		try {
			bWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			log.severe("Could not close file.");
			System.exit(-1);
		}
	}

	public static String getLine(BufferedReader bReader) {
		try {
			String line = bReader.readLine();
			return line;
		} catch(IOException e) {
			e.printStackTrace();
			log.severe("Could not read line from file.");
			System.exit(-1);
		}
		return null;
	}

	public static void writeLine(BufferedWriter bWriter, String line) {
		try {
			bWriter.write(line + "\n");
		} catch(IOException e) {
			e.printStackTrace();
			log.severe("Could not write line to file.");
			System.exit(-1);
		}
	}

	public static void writeSerializedObject(String file, Object object) {
		try{
			OutputStream oFile = new FileOutputStream(file);
			OutputStream buffer = new BufferedOutputStream(oFile);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try{
				output.writeObject(object);
			}
			finally{
				output.close();
			}
		}
		catch(IOException ex){
			log.severe("Cannot perform output.");
			ex.printStackTrace();
			System.exit(-1);
		}
	}
    public static Object readSerializedObject(String file) {
        try {
            return readSerializedObject(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            log.severe("Cannot perform input.");
            throw new RuntimeException(e);
        }
    }

	public static Object readSerializedObject(InputStream iFile) {
		Object object = null;
		try{
			InputStream buffer = new BufferedInputStream(iFile);
			ObjectInput input = new ObjectInputStream(buffer);
			try{
				object = input.readObject();
			}
			finally{
				input.close();
			}
		}
		catch (ClassNotFoundException e) {
			log.severe("Cannot perform input.");
            throw new RuntimeException(e);
		}
		catch(IOException ex){
			log.severe("Cannot perform input.");
            throw new RuntimeException(ex);
		}
		return object;
	}
}
