package edu.cmu.cs.lti.ark.ssl.pos;

import java.io.Serializable;
import java.util.List;

import edu.berkeley.nlp.crf.LabeledInstanceSequence;
import fig.basic.Pair;

public class POSLabeledInstanceSequence implements LabeledInstanceSequence<String, String, String>, Serializable {	
		/**
		 * 
		 */
		private static final long serialVersionUID = -2235155617439156365L;
		private List<String> observations;
		private List<String> labels;
		
		public POSLabeledInstanceSequence(Pair<List<String>, List<String>> datum) {
			observations = datum.getFirst();
			labels = datum.getSecond();
		}		
		
		public String getGoldLabel(int index) {
			return labels.get(index);
		}

		public String getEdgeInstance(int index, String previousLabel) {
			return previousLabel;
		}

		public int getSequenceLength() {
			return observations.size();
		}

		public String getVertexInstance(int index) {
			return observations.get(index);
		}
		
	}