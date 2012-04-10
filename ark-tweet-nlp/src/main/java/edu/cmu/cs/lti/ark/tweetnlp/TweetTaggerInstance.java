package edu.cmu.cs.lti.ark.tweetnlp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import edu.cmu.cs.lti.ark.ssl.pos.POSFeatureTemplates;
import edu.cmu.cs.lti.ark.ssl.pos.POSModel;
import edu.cmu.cs.lti.ark.ssl.pos.POSOptions;
import edu.cmu.cs.lti.ark.ssl.pos.SemiSupervisedPOSTagger;
import edu.cmu.cs.lti.ark.ssl.util.BasicFileIO;
import fig.basic.Pair;

/** Wraps SemiSupervisedPOSTagger for easier inference-only usage (i.e. to tag new sentences) */
public class TweetTaggerInstance {
	private SemiSupervisedPOSTagger tagger = null;
	private POSModel model = null;

    private static TweetTaggerInstance ttInstance;

    static final POSModel NOAHS_MODEL;
	public static TweetTaggerInstance getInstance() {
        if (ttInstance == null) {
            ttInstance = new TweetTaggerInstance();
        }
		return ttInstance;
	}
    static {
        NOAHS_MODEL = deserializeNoahsModel();
    }
    private static POSModel deserializeNoahsModel(){
        return (POSModel) BasicFileIO.readSerializedObject(
                        TweetTaggerInstance.class.getResourceAsStream("tweetpos.model"));
    }
	private TweetTaggerInstance() {
		List<String> argList = new ArrayList<String>();
		argList.add("--trainOrTest");
		argList.add("test");
	    argList.add("--useGlobalForLabeledData");
	    argList.add("--useStandardMultinomialMStep");
	    argList.add("--useStandardFeatures");
	    argList.add("--regularizationWeight");
	    argList.add("0.707");
	    argList.add("--regularizationBias");
	    argList.add("0.0");
	    argList.add("--initialWeightsLower");
	    argList.add("-0.01");
	    argList.add("--initialWeightsUpper");
	    argList.add("0.01");
	    argList.add("--iters");
	    argList.add("1000");
	    argList.add("--printRate");
	    argList.add("100");
	    argList.add("--execPoolDir");
	    argList.add("/tmp");
	    argList.add("--modelFile");
	    argList.add("lib/tweetpos.model");
	    argList.add("--useDistSim");
	    argList.add("--useNames");
	    argList.add("--numLabeledSentences");
	    argList.add("100000");
	    argList.add("--maxSentenceLength");
	    argList.add("200");
	    String[] args = new String[argList.size()];
	    argList.toArray(args);
	    POSOptions options = new POSOptions(args);
		options.parseArgs(args);
	    tagger = new SemiSupervisedPOSTagger(options);
        model = NOAHS_MODEL; // FIXME(alexander) clone
	    tagger.initializeDataStructures();

        POSFeatureTemplates.log.setLevel(Level.WARNING);
        SemiSupervisedPOSTagger.log.setLevel(Level.WARNING);
	}

	public void renew() {
	    model = deserializeNoahsModel();
	}

	public List<String> getTagsForOneSentence(List<String> words) {
        // BTO: i don't get this, does tagger.testCRF need a dummy list or something? can we delete?
        ArrayList<String> dTags = new ArrayList<String>();
        for (String tok : words) {
            dTags.add("N");
        }

		List<Pair<List<String>, List<String>>> col =
			new ArrayList<Pair<List<String>, List<String>>>();
		col.add(new Pair<List<String>, List<String>>(words, dTags));
		List<List<String>> col1 = tagger.testCRF(col, model);
		if (col1.size() != 1) {
            throw new RuntimeException("Problem with the returned size of the collection. Should be 1.");
		}
		List<String> tags = col1.get(0);
		return tags;
	}
}
