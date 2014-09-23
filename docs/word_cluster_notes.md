A few details on the data and preprocessing for the word clusters, beyond what's mentioned in the NAACL 2013 paper.
Tobi ran all this in summer 2012.
The paths refer to inside a particular server at CMU.  If someone can get an
official decision from Twitter whether it's OK by their TOS to share this data,
with or without metadata (perhaps, bare text without metadata?) please let us
know. `daily.100k` refers to a random sample of 100,000 tweets from each day,
drawn from the greater gardenhose/decahose stream.

From a Tobi/Kevin/Brendan March 2014 email, here is the uniqued tokenized data used to train the Brown clustering:

    /cab1/ooo/clusters/new/big100kuniq

and backed-up to

    /cab0/brendano/twi/pos/ooo_clusters_backup_of_2012_tobi_experiments

From a Tobi June 2014 email, here's the procedure to get that data:

The twitter thing was from the daily.100k sample from September 10, 2008 to August 14, 2012, filtering out non-English using langid.py and filtering out any tweets that matched text from the Daily547 test set.
The normalization process was running the tokenizer, then running each token through:

```
public static Pattern URL = Pattern.compile(Twokenize.url);
public static Pattern justbase = Pattern.compile("(?!www\\.|ww\\.|w\\.)[a-zA-Z0-9]+\\.[A-Za-z0-9\\.]+");
public static Pattern AtMention = Pattern.compile("[@@][a-zA-Z0-9_]+");

public static String normalize(String str) {
    str = str.toLowerCase();
    if (URL.matcher(str).matches()){            
        String base = "";
        Matcher m = justbase.matcher(str);
        if (m.find())
            base=m.group().toLowerCase();
        return "<URL-"+base+">";
    }
    if (AtMention.matcher(str).matches())
        return "<@MENTION>";
    return str;
}
```

