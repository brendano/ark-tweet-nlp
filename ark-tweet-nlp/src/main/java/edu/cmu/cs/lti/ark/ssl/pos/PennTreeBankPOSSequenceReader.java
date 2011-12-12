package edu.cmu.cs.lti.ark.ssl.pos;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.syntax.Trees;
import fig.basic.Pair;

public class PennTreeBankPOSSequenceReader {
	
	public static Collection<Pair<List<String>, List<String>>> readPOSSeqences(String path, int numSequences, int maxSequenceLength) {
		Collection<Pair<List<String>, List<String>>> sequences = new ArrayList<Pair<List<String>,List<String>>>();
		try {
			Reader r = new FileReader(new File(path));
			Trees.PennTreeReader trees = new Trees.PennTreeReader(r);
			while (trees.hasNext()) {
				Tree<String> t = trees.next();
				List<String> words = new ArrayList<String>();
				List<String> poss = new ArrayList<String>();
				List<String> terminals = t.getTerminalYield();
				List<String> preterminals = t.getPreTerminalYield();
				for (int i=0; i<terminals.size(); ++i) {
					String word = terminals.get(i);
					String pos = preterminals.get(i);
					if (!pos.equals("-NONE-")) {
						words.add(word);
						poss.add(pos);
					}
				}
				if (words.size() <= maxSequenceLength) {
					sequences.add(Pair.makePair(words, poss));
				}
				if (numSequences != -1 && sequences.size() >= numSequences) {
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sequences;
	}
	
	public static void main(String[] args) {
		try {
			Reader r = new FileReader(new File("/Users/tberg/corpora/EnglishTreebank/WSJ.mrg"));
			Trees.PennTreeReader trees = new Trees.PennTreeReader(r);
			Tree<String> t = trees.next();
			System.out.println(t);
			System.out.println(t.getTerminalYield());
			System.out.println(t.getPreTerminalYield());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
