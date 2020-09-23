#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed May  6 15:07:18 2020

@author: sdas
"""

from operator import itemgetter
import os
import regex as re
import numpy as np
import emot
import string
import twNormalizer as tn
from nltk.corpus import stopwords  
stop_words = set(stopwords.words('english')) 

#URLPAT="https://t.co/[0-9|a-z|A-Z]*"

##http://www.panacealab.org/covid19/
##COVD19, CoronavirusPandemic, COVID-19, 2019nCoV, CoronaOutbreak,coronavirus , 
##WuhanVirus, covid19, coronaviruspandemic, covid-19, 2019ncov, coronaoutbreak, wuhanvirus. 

ignoreht={}
ignoreht["covd19"]=""
ignoreht["coronaviruspandemic"]=""
ignoreht["covid-19"]=""
ignoreht["2019ncov"]=""
ignoreht["coronaoutbreak"]=""
ignoreht["coronavirus"]=""
ignoreht["wuhanvirus"]=""
ignoreht["covid19"]=""
ignoreht["ncov"]=""
ignoreht["covid"]=""
ignoreht["virus"]=""
ignoreht["sarscov2"]=""
ignoreht["amp"]=""


tfthresh=20
htfthresh=5
emthresh=3
punctable = str.maketrans('', '', string.punctuation)
    
def processPanaceaData(idtsfile, conttsvfiles, outdir):
    
    
    idcol=0
    cleantextcol=1
    htcol = 6
 
    htseparator="GSDASHT"
    
    timeinfo={}
    
    fin = open (idtsfile, "r")
    
 #   header = fin.readline()
    line = fin.readline()
    lx=0
    
    while line:
        
        parts = line.split("\t")    
        twid = parts[0].strip()
        date = parts[1]
        timeinfo[twid] = date
        lx += 1 
        line = fin.readline()
        
        
    fin.close()
    
    print ("lx="+str(lx))
    print ("len(timeinfo)="+str(len(timeinfo)))
    
    
    
    lx=0
    bydatecounts={}
    htdict={}
    termdict={}
    emojidict={}
    
   
    for conttsvfile in conttsvfiles:
        print ("processing "+conttsvfile)
        fin = open (conttsvfile, "r")
            
      #  header = fin.readline() #not doing anything
        
        
        line = fin.readline()
        
        
        while line:
            
            parts = line.split("\t")    
            twid = parts[idcol].strip()
            tweet = parts[cleantextcol]
            htags = parts[htcol].split(htseparator)
            
            
            if twid in timeinfo:
                
                emojis = tn.parseEmojis(tweet)
                temp = emot.emoticons(tweet) #tn.removeHTAtEmoji(tweet))
                emoticons=[]
                if 'value' in temp:
                    emoticons = temp['value']
        
                for emoji in emojis:
                    if emoji not in emojidict:
                        emojidict[emoji]=1
                    else:
                        emojidict[emoji]+=1
                
                for emoticon in emoticons:
                    if emoticon not in emojidict:
                        emojidict[emoticon]=1
                    else:
                        emojidict[emoticon]+=1
        
                
                
                for htag in htags:
                    htag = htag.lower().translate(punctable).strip()
                    if htag=="":
                        continue
                    
#                    ismatch=False
#                    for ht in ignoreht:
#                        if ht in htag:
#                            ismatch=True
#                            break
#                    
#                    if ismatch:
#                        continue
                    
                    if htag not in htdict and htag not in ignoreht:
                        htdict[htag]=1
                    elif htag in htdict:
                        htdict[htag] += 1
                    
                newsent=""
                words = tweet.strip().split()
                for word in words:
                    if word.startswith("@") or word.startswith("#"):
                        continue
                    
                    newsent +=" "+word
        
                
                newsent = newsent.translate(punctable).lower()
                words = re.sub("[^\P{P}-]+", " ", newsent).strip().split()
                
                if len(newsent)>0:
                    
                    datei=timeinfo[twid]
                    
                    if datei in bydatecounts:
                        bydatecounts[datei]+=1
                    else:
                        bydatecounts[datei]=1
                    
                    
                    for word in words:
                        if word.strip()!="":
                            
                            if word not in termdict and word not in ignoreht:
                                termdict[word]=1
                            elif word in termdict:
                                termdict[word] += 1
                   
                
                    
                    
                    
            lx += 1 
            line = fin.readline()
            if lx%1000==0:
                print ("lx="+str(lx))
                print ("bydatecounts.sz "+str(len(bydatecounts)))
                print ("termdict.sz "+str(len(termdict)))
                print ("htdict.sz "+str(len(htdict)))
                print ("emojidict.sz "+str(len(emojidict)))
            
        fin.close()
        print ("lx="+str(lx))
        print ("bydatecounts.sz "+str(len(bydatecounts)))
        print ("termdict.sz "+str(len(termdict)))
        print ("htdict.sz "+str(len(htdict)))
        print ("emojidict.sz "+str(len(emojidict)))
            
    fout = open(outdir+"/data.stats.txt", "w")
    for datei in bydatecounts:
        print (datei+" "+str(bydatecounts[datei]))
        fout.write(datei+" "+str(bydatecounts[datei])+"\n")
    fout.close()

    fout = open(outdir+"/termdict.txt", "w")
    for word in termdict:
        if termdict[word]>tfthresh and word.isalpha() and word not in stop_words and len(word)>1:
            fout.write(word+" "+str(termdict[word])+"\n")
    fout.close()
    
    fout = open(outdir+"/htagsdict.txt", "w")
    for word in htdict:
        if htdict[word]>htfthresh:
            fout.write(word+" "+str(htdict[word])+"\n")
    fout.close()
    
    fout = open(outdir+"/emdict.txt", "w")
    for word in emojidict:
        if emojidict[word]>emthresh:
            fout.write(word+" "+str(emojidict[word])+"\n")
    fout.close()
    
    
    
inpdir = "/home/sdas/cord/ecir/tsv"
idtsfile="/home/sdas/cord/ecir/newsample.tsv"
outdir="/home/sdas/cord/ecir/dicts"
flist =   os.listdir(inpdir)
toprocess=[]

for fname in flist:
    if fname.startswith("content_") and fname.endswith(".tsv"):
        toprocess.append(inpdir+"/"+fname)
        
processPanaceaData(idtsfile, toprocess, outdir)

    
    
    