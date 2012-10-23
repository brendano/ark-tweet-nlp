package cmu.arktweetnlp.util;

import java.io.*;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

/**
 * 
 * @author Dipanjan Das
 *
 */
public class BasicFileIO {
	// taken from Dipanjan's codebase
	

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
    public static BufferedReader openFileToReadUTF8(String file) {
        try {
            BufferedReader bReader = null;
            if (file.endsWith(".gz")) {
                bReader = new BufferedReader(new InputStreamReader(
                        new GZIPInputStream(new FileInputStream(file)), "UTF-8"));
            } else {
                bReader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file), "UTF-8"));
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
    public static BufferedWriter openFileToWriteUTF8(String file) {
        try {
            BufferedWriter bWriter = null;
            if (file.endsWith(".gz")) {
                bWriter = new BufferedWriter(new OutputStreamWriter(
                        new GZIPOutputStream(new FileOutputStream(file)), "UTF-8"));
            } else {
                bWriter = new BufferedWriter(new OutputStreamWriter(
                	    new FileOutputStream(file), "UTF-8"));
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

    public static String getLine(JsonParser jParse) {
        //returns the next "text" field or null if none left
        try {
            while(jParse.getText()!=null){
                if ("hashtags".equals(jParse.getCurrentName())
                        |"retweeted_status".equals(jParse.getCurrentName())) {
                    jParse.nextToken();
                    jParse.skipChildren();
                }				
                if ("text".equals(jParse.getCurrentName())) {
                    jParse.nextToken(); // move to value
                    String tweet = jParse.getText();
                    jParse.nextToken();
                    if(tweet.length()>0) //because tagger crashes on 0-length tweets
                        return tweet;
                }
                jParse.nextToken();
            }
        } catch(JsonParseException e){
            e.printStackTrace();
            log.severe("Error parsing JSON.");
            System.exit(-1);			  
        }
        catch(IOException e) {
            e.printStackTrace();
            log.severe("Could not read line from file.");
            System.exit(-1);
        }

        return null;	//jParse is null (EOF)	
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
    
    
	/**
	 * Please only use absolute paths, e.g. /cmu/arktweetnlp/6mpaths
	 * 
	 * e.g. http://stackoverflow.com/questions/1464291/how-to-really-read-text-file-from-classpath-in-java
	 * 
	 * (added by Brendan 2012-08-14)
	 * @throws IOException 
	 */
	public static BufferedReader getResourceReader(String resourceName) throws IOException {
		assert resourceName.startsWith("/") : "Absolute path needed for resource";
		
		InputStream stream = BasicFileIO.class.getResourceAsStream(resourceName);
		if (stream == null) throw new IOException("failed to find resource " + resourceName);
		//read in paths file
		BufferedReader bReader = new BufferedReader(new InputStreamReader(
			stream, Charset.forName("UTF-8")));
		return bReader;
	}
	
	/** Try to get a file, if it doesn't exist, backoff to a resource. 
	 * @throws IOException **/
	public static BufferedReader openFileOrResource(String fileOrResource) throws IOException {
		try {
			if (new File(fileOrResource).exists()) {
				return openFileToReadUTF8(fileOrResource);
			} else {
				return getResourceReader(fileOrResource);
			}			
		} catch (IOException e) {
			throw new IOException("Neither file nor resource found for: " + fileOrResource);
		}
	}
}
