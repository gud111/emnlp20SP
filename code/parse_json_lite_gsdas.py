#!/usr/bin/python
# -*- coding: utf-8 -*-
#
#   /$$$$$$  /$$      /$$ /$$      /$$ /$$$$$$$$
#  /$$__  $$| $$$    /$$$| $$$    /$$$|__  $$__/
# | $$  \__/| $$$$  /$$$$| $$$$  /$$$$   | $$   
# |  $$$$$$ | $$ $$/$$ $$| $$ $$/$$ $$   | $$   
#  \____  $$| $$  $$$| $$| $$  $$$| $$   | $$   
#  /$$  \ $$| $$\  $ | $$| $$\  $ | $$   | $$   
# |  $$$$$$/| $$ \/  | $$| $$ \/  | $$   | $$   
#  \______/ |__/     |__/|__/     |__/   |__/  
#
#
# Developed during Biomedical Hackathon 6 - http://blah6.linkedannotation.org/
# Authors: Ramya Tekumalla, Javad Asl, Juan M. Banda
# Contributors: Kevin B. Cohen, Joanthan Lucero

import pandas as pd
import numpy as np
import json
import sys
import string
import re
# This will load the fields list
import fields
from emot.emo_unicode import UNICODE_EMO, EMOTICONS
import emoji

fieldsFilter = fields.fields


def remove_emoticons(text):
    emoticon_pattern = re.compile(u'(' + u'|'.join(k for k in EMOTICONS) + u')')
    return emoticon_pattern.sub(r'', text)

def remove_emoji(text):
    emoji_pattern = re.compile("["
                           u"\U0001F600-\U0001F64F"  # emoticons
                           u"\U0001F300-\U0001F5FF"  # symbols & pictographs
                           u"\U0001F680-\U0001F6FF"  # transport & map symbols
                           u"\U0001F1E0-\U0001F1FF"  # flags (iOS)
                           u"\U00002702-\U000027B0"
                           u"\U000024C2-\U0001F251"
                           "]+", flags=re.UNICODE)
    return emoji_pattern.sub(r'', text)

def give_em_content(text):
    emoticon_pattern = re.compile(u'(' + u'|'.join(k for k in EMOTICONS) + u')')
    tmp1 =  re.findall(emoticon_pattern, text)

    emoji_pattern = re.compile("["
                           u"\U0001F600-\U0001F64F"  # emoticons
                           u"\U0001F300-\U0001F5FF"  # symbols & pictographs
                           u"\U0001F680-\U0001F6FF"  # transport & map symbols
                           u"\U0001F1E0-\U0001F1FF"  # flags (iOS)
                           u"\U00002702-\U000027B0"
                           u"\U000024C2-\U0001F251"
                           "]+", flags=re.UNICODE)
    tmp2 = re.findall(emoji_pattern, text)
    emtext=""
    for em in tmp1:
        for ele in em:
            emtext += "GSDASEM"+ele
    for em in tmp2:
        for ele in em:
            emtext += "GSDASEM"+ele
    
    return emtext

def give_ht_content(x):

    httext=""
    for entry in x:
        httext += "GSDASHT"+entry['text']

    return httext

def remove_urls(text):
    result = re.sub(r"http\S+", "", text)
    return(result)

def remove_twitter_urls(text):
    clean = re.sub(r"pic.twitter\S+", "",text)
    return(clean)

def give_emoji_free_text(text):
    return emoji.get_emoji_regexp().sub(r'', text)

def process(data, preprocess, partnum, outfile):
    tweet_df = pd.io.json.json_normalize(data)
    # Cleaner solution in case some of the fields in the list are non existent and/or have typos
    tweet_df = tweet_df.loc[:, tweet_df.columns.isin(fieldsFilter)]
    
    tweet_df['text'] = tweet_df['text'].str.replace('\n','')
    tweet_df['text'] = tweet_df['text'].str.replace('\r','')
    
    if preprocess == 'p':
    	
        #print(tweet_df['text'])
        tweet_df['text'] = tweet_df['text'].apply(lambda x : remove_urls(x))
        tweet_df['text'] = tweet_df['text'].apply(lambda x : remove_twitter_urls(x))
        tweet_df['emojis'] = tweet_df['text'].apply(lambda x : give_em_content(x))
        #print(tweet_df['emojis'])
        
        tweet_df['text'] = tweet_df['text'].apply(lambda x : remove_emoticons(x))
        tweet_df['text'] = tweet_df['text'].apply(lambda x : remove_emoji(x))
        tweet_df['text'] = tweet_df['text'].apply(lambda x : give_emoji_free_text(x))
        tweet_df['entities.hashtags'] = tweet_df['entities.hashtags'].apply(lambda x : give_ht_content(x))
        print(tweet_df['entities.hashtags'])
    

    
    
    
    with open(outfile+"_"+str(part)+".tsv",'w') as write_tsv:
        write_tsv.write(tweet_df.to_csv(sep='\t', index=False))



fileN = sys.argv[1]
preprocess = sys.argv[2]
outfile = sys.argv[3]

data = []
part=0
with open(fileN, 'r') as f:
    for line in f:
        data.append(json.loads(line))
        if len(data)>100000:
            print ("read "+str(part)+"-1M records, processing")
            process(data, preprocess, part, outfile)
            data.clear()
            part += 1