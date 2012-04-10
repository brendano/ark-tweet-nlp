package edu.cmu.cs.lti.ark.ssl.pos.eval;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.berkeley.nlp.util.Counter;
import edu.cmu.cs.lti.ark.ssl.pos.POSModel;
import edu.cmu.cs.lti.ark.ssl.util.BasicFileIO;

public class ModelDump {
	public static void main(String[] args) {
		String file = args[0];
		String outFile = args[1];
		POSModel model = (POSModel) BasicFileIO.readSerializedObject(file);
		ArrayList<String> indexToFeature = model.getIndexToFeature();
		double[] weights = model.getWeights();
		try {
			FileWriter w = new FileWriter(outFile);
			Counter<String> weightsCounter = new Counter<String>();
			for (int f=0; f<indexToFeature.size(); ++f) {
				weightsCounter.setCount(indexToFeature.get(f), weights[f]);
			}
			List<String> sortedFeatures = weightsCounter.getSortedKeys();
			for (int f=0; f<sortedFeatures.size(); ++f) {
				w.write(String.format("%s\t%f\n", sortedFeatures.get(f), weightsCounter.getCount(sortedFeatures.get(f))));
				w.flush();
			}
			w.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}