Brendan O'Connor 2017-07-24
Retrieving the original tweets for the ARK Twitter POS annotated data 
(which are used in Tweebank too, I believe)

Data:

There is one json.tsv file for each of the two "supertsv" files 
(daily547 and oct27) from twpos-data-v0.3.

In the .json.tsv files, each line has three fields:

tweetid \t timestamp \t full tweet in JSON format

---
Internal note: data collected from 
hobbes:~brenocon/twipos/get_orig_tweets_for_ark_pos_data
