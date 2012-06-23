package edu.cmu.cs.lti.ark.ssl.pos.crf;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import edu.cmu.cs.lti.ark.ssl.pos.TabSeparatedFileReader;
import fig.basic.Pair;

public class Accuracy {
	 
	public static void main(String[] args) {
		String goldFile = 
			"/mal2/dipanjan/experiments/SSL/data/4000qs.cleaned.test.tab";
		
		String autoFile = 
			"/mal2/dipanjan/experiments/SSL/tmp/4000qs.cleaned.test.tab.out";
		
		Collection<Pair<List<String>, List<String>>>  gSequences =
			TabSeparatedFileReader.readPOSSeqences(goldFile, 
					Integer.MAX_VALUE, 
					Integer.MAX_VALUE);
		
		Collection<Pair<List<String>, List<String>>>  aSequences =
			TabSeparatedFileReader.readPOSSeqences(autoFile, 
					Integer.MAX_VALUE, 
					Integer.MAX_VALUE);
		Iterator<Pair<List<String>, List<String>>> aItr = 
			aSequences.iterator();
		double total = 0.0;
		double correct = 0.0;
		for (Pair<List<String>, List<String>> gSeq: gSequences) {
			List<String> gPOS = gSeq.getSecond();
			List<String> aPOS = aItr.next().getSecond();
			for (int i = 0; i < gPOS.size(); i++) {
				if (gPOS.get(i).equals(aPOS.get(i))) {
					correct++;
				}
				total++;
			}
		}
		System.out.println("Accuracy : " + (correct/total));
	}
	
}