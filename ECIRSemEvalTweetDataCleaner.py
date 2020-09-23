#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed May  6 15:07:18 2020

@author: sdas
"""

from operator import itemgetter
import regex as re
import numpy as np
import emot
import string
import twNormalizer as tn
from nltk.corpus import stopwords  
stop_words = set(stopwords.words('english')) 

punctable = str.maketrans('', '', string.punctuation)
emojiseparator="GSDASEM"
htseparator="GSDASHT"
noneindicator="NONE"


    
tfthresh=5
htfthresh=2
emthresh=2
punctable = str.maketrans('', '', string.punctuation)
    
def loadDictionary(inpfile):
    
    fin = open (inpfile, "r")
    lines = fin.readlines()
    fin.close()
    
    thisdict={}
    for lx, line in enumerate(lines):
        parts = line.strip().split()
        term = line.replace(parts[len(parts)-1],"").strip()
        thisdict[term] = lx
 
    print ("Loaded dictionary of length "+str(len(thisdict)))
    return thisdict
    
def collectDictionaries(tsvfile, outdir):
    
    
    #idcol=0
    cleantextcol=1
    
    
    
    lx=0
    
    htdict={}
    termdict={}
    emojidict={}
    
   
    
    print ("processing "+tsvfile)
    fin = open (tsvfile, "r")
        
    header = fin.readline() #not doing anything
    
    
    line = fin.readline()
    
    
    while line:
        
        parts = line.split("\t")    
        #twid = parts[idcol].strip()
        tweet = parts[cleantextcol]
    
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

        
        
            
        newsent=""
        words = tweet.strip().split()
        for word in words:
            if word.startswith("@") or word.startswith("http"):
                continue
            elif word.startswith("#"):
                htag = word.lower().translate(punctable).strip()
                if htag=="":
                    continue
            
                if htag not in htdict:
                    htdict[htag]=1
                elif htag in htdict:
                    htdict[htag] += 1
                    
            newsent +=" "+word

        
        newsent = newsent.translate(punctable).lower()
        words = re.sub("[^\P{P}-]+", " ", newsent).strip().split()
        
        if len(newsent)>0:
            
            for word in words:
                if word.strip()!="":                    
                    if word not in termdict:
                        termdict[word]=1
                    elif word in termdict:
                        termdict[word] += 1
        lx += 1 
        line = fin.readline()
        if lx%1000==0:
            print ("lx="+str(lx))
            print ("termdict.sz "+str(len(termdict)))
            print ("htdict.sz "+str(len(htdict)))
            print ("emojidict.sz "+str(len(emojidict)))
    
    fin.close()

    print ("termdict.sz "+str(len(termdict)))
    print ("htdict.sz "+str(len(htdict)))
    print ("emojidict.sz "+str(len(emojidict)))
            

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
    


def processSemEvalData(tsvfile, outdir, \
                       emojidict, termdict, htagdict):
    
    
    idcol=0
    cleantextcol=1
    
    lx=0
  
    tfout = open(outdir+"/textcontent.mallet.in", "w")
    emfout = open(outdir+"/emojicontent.mallet.in", "w")
   
    
    
    fin = open (tsvfile, "r")
        
    header = fin.readline() #not doing anything
    hparts = header.strip().split("\t")
    print (len(hparts))
    print (hparts)
    line = fin.readline()
    
    
    while line:
        
        parts = line.split("\t")    
        twid = parts[idcol].strip()
        tweet = parts[cleantextcol]
        labels=""
        for px in range(cleantextcol+1, len(parts)):
            if int(parts[px])!=0:
                labels+=":"+hparts[px]
        
        #print (labels)
            
        emtext=""
        httext=""
        emojis = tn.parseEmojis(tweet)
        temp = emot.emoticons(tweet) #tn.removeHTAtEmoji(tweet))
        emoticons=[]
        if 'value' in temp:
            emoticons = temp['value']

        for emoji in emojis:
            if emoji in emojidict:
                emtext+=" "+emojiseparator+"-"+str(emojidict[emoji])
            
        
        for emoticon in emoticons:
            if emoticon in emojidict:
                emtext+=" "+emojiseparator+"-"+str(emojidict[emoticon])
            
        newsent=""
        words = tweet.strip().split()
        for word in words:
            if word.startswith("@") or word.startswith("http"):
                continue
            elif word.startswith("#"):
                htag = word.lower().translate(punctable)
                if htag in htagdict:
                    httext+=" "+htseparator+"-"+htag #str(htagdict[htag])
            else:
                newsent +=" "+word
            
        newsent = newsent.translate(punctable).lower()
        words = re.sub("[^\P{P}-]+", " ", newsent).strip().split()
        newtext=""
        if len(newsent)>0:
            
            for word in words:
                if word in termdict:
                    newtext +=" "+word
           
        
        emtext = emtext.strip()
        httext = httext.strip()
        newtext = newtext.strip()
        
        if emtext=="":
            emtext = emojiseparator+"-"+noneindicator
        if httext=="":
            httext = htseparator+"-"+noneindicator
        
        if len(newtext)>0:
            tfout.write(twid+"\t"+labels+"\t"+newtext+" "+httext+"\n")
            emfout.write(twid+"\t"+labels+"\t"+emtext+"\n")
            print(twid+"\t"+labels+"\t"+newtext+" "+httext)
            print(twid+"\t"+labels+"\t"+emtext)
            
        lx += 1 
        line = fin.readline()
        if lx%1000==0:
            print ("lx="+str(lx))
            tfout.flush()
            emfout.flush()
        
    
    
    
    fin.close()
    tfout.close()
    emfout.close()
    
    return
   
inptrfile="/home/sdas/data/SemEval2018-Task1-all-data/English/E-c/2018-E-c-En-train.txt"
dictdir="/home/sdas/cord/ecir/semeval/dicts"
outdir="/home/sdas/cord/ecir/semeval/trdata"

collectDictionaries(inptrfile, dictdir)

emojidict = loadDictionary(dictdir+"/emdict.txt")

fout = open (outdir+"/emodict4lda.dat", "w")

for emoji in emojidict:
    fout.write(emojiseparator+"-"+str(emojidict[emoji])+" "+emoji+"\n")
    
fout.write(emojiseparator+"-"+noneindicator+" "+str(len(emojidict))+"\n")
fout.close()

termdict = loadDictionary(dictdir+"/termdict.txt")
htagdict = loadDictionary(dictdir+"/htagsdict.txt")
processSemEvalData(inptrfile, outdir, emojidict, termdict, htagdict)
inptsfile="/home/sdas/data/SemEval2018-Task1-all-data/English/E-c/2018-E-c-En-test-gold.txt"
outdir="/home/sdas/cord/ecir/semeval/tsdata"
processSemEvalData(inptsfile, outdir, emojidict, termdict, htagdict)
