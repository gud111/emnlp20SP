#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Apr 28 12:14:23 2020

@author: sdas
"""

import string
import regex as re 
from emojificate.filter import emojificate




def normalize(sentence):
    
    newsent=""
    words = sentence.split()
    for word in words:
        if word.startswith("@") or "http" in word:
            continue
        
        word = word.replace("--"," ")
        word = re.sub("[^\P{P}-]+", " ", word).lower()
        word = word.replace("tldr"," ").replace("amp"," ").strip()
        nword = re.sub("\d+", "", word).strip()
        if len(nword)==0:
            continue
                
        nword = re.sub("[^\P{P}]+", "", word).strip()
        
        if len(nword)<2:
            continue
        
        
        newsent += " "+word        

    return newsent.strip()

def parseEmojis(tweet):

    ptweet = emojificate(tweet)
    matches = ptweet.split("Emoji:")

    toret=[]
    
    for m in matches:
        #print ("zzz" + m)      
        
        sindx = m.find("title=")
        eindx = m.find(" aria-label=")
        if sindx>0 and eindx>0:
            toret.append (m[sindx:eindx].replace("title=",""))


    return toret



def normalizeTweet(tweet):

    emojis = parseEmojis(tweet);
    extra=""
    for emoji in emojis:
        extra += " "+emoji.lower().replace("\"","")
            
    return ((normalize(tweet)+" "+extra.strip()).strip())



def removeAtEmoji(tweet):

    
    newsent=""
    words = tweet.split()
    for word in words:
        if word.startswith("@") or "http" in word:
            continue
        else:
            newsent += " "+word.strip()
        
    return newsent.encode('ascii', 'ignore').decode('ascii').strip()

def getDict(inptsvf, outf, stopwordsf):
    
    stopwords={}
    with open (stopwordsf, "r") as f:
        for line in f.readlines():
            stopwords[line.strip()]=""
    
    f.close()
    print ("#stopwords "+str(len(stopwords)))
    
    termcounts={}
    with open (inptsvf, "r") as f:
        
        line = f.readline() #header
        line = f.readline() #first line
        
        while line:
            
            parts = line.split('\t')
            #twid = parts[0]
            if len(parts) == 6:
                twtxt = parts[1]
                lng = parts[4]
                
                if lng=="en":
                    ntweet = normalizeRemoveEmoji(twtxt).strip()
                    if len(ntweet)>0:
                        words = ntweet.split();
                        for word in words:
                            word = word.strip()
                            if word in termcounts:
                                termcounts[word]+=1
                            else:
                                termcounts[word]=1
                        
            line = f.readline()


    f.close()
    print ("#terms before filtering "+str(len(termcounts)))
    cnt=0
    fout=open(outf, "w")
    for word in termcounts:
        if word in stopwords:
            continue
        if termcounts[word]>3:
            fout.write(word+" "+str(termcounts[word])+"\n")
            cnt +=1

    fout.close()
    print ("#terms after filtering "+str(cnt))

