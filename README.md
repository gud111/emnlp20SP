# emnlp20SP


#Processing the twitter data
#Download tweets from Panacea (For example, https://zenodo.org/record/4027658#.X2gKnhERUgY )

#Use SMMT toolkit to obtain the tweets
-Sample, choose subset to get and then run get_metadata.py in data_acquisition (https://github.com/thepanacealab/SMMT)

#Now our methods kick in 

Step 1: Use methods in TweetDataCleaner
-> processPanaceaData
-> processDictionaries
-> splitTweetContent

# Now we are in position to run our mallet tools

-> Use mallet import-file to import data for the content part
-> For the emos, we use raw format
-> Specify all paths in STWithELDA to run








