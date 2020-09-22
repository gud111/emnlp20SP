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
#import emot
import string
import twNormalizer as tn

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
    

    
def processPanaceaData(idtsfile, conttsvfiles, outdir, \
                       emojidict, termdict, htagdict):
    
    
    idcol=0
    cleantextcol=1
    htcol = 6
    emojicol = 7
    emojiseparator="GSDASEM"
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
  
    tfout = open(outdir+"/textcontent.mallet.in", "w")
    emfout = open(outdir+"/emojicontent.mallet.in", "w")
   
    for conttsvfile in conttsvfiles:
    
        fin = open (conttsvfile, "r")
            
      #  header = fin.readline() #not doing anything
        
        
        line = fin.readline()
        
        
        while line:
            
            parts = line.split("\t")    
            twid = parts[idcol].strip()
            tweet = parts[cleantextcol]
            htags = parts[htcol].split(htseparator)
            emojis = parts[emojicol].split(emojiseparator)
            
            if twid in timeinfo:
                
                emtext=""
                httext=""
                for emoji in emojis:
                    emlist = tn.parseEmojis(emoji)
                    for em in emlist:
                        if em in emojidict:
                            emtext+=" "+emojiseparator+"-"+str(emojidict[em])
                
                for htag in htags:
                    htag = htag.lower().translate(punctable)
                    if htag in htagdict:
                        httext+=" "+htseparator+"-"+htag #str(htagdict[htag])
                    
                newsent=""
                words = tweet.strip().split()
                for word in words:
                    if word.startswith("@") or word.startswith("#"):
                        continue
                    
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
                    emtext = emojiseparator+"-NONE"
                if httext=="":
                    httext = htseparator+"-NONE"
                
                if len(newtext)>0:
                    tfout.write(twid+"\t"+timeinfo[twid]+"\t"+newtext+" "+httext+"\n")
                    emfout.write(twid+"\t"+timeinfo[twid]+"\t"+emtext+"\n")
                
                    
            lx += 1 
            line = fin.readline()
            if lx%1000==0:
                print ("lx="+str(lx))
                tfout.flush()
                emfout.flush()
            
        fin.close()
        print ("lx="+str(lx))
       
    
    tfout.close()
    emfout.close()
    
    
    
inpdir = "/home/sdas/cord/ecir/tsv"
idtsfile="/home/sdas/cord/ecir/newsample.tsv"
dictdir="/home/sdas/cord/ecir/dicts"
outdir="/home/sdas/cord/ecir/data"

flist =   os.listdir(inpdir)
toprocess=[]

for fname in flist:
    if fname.startswith("content_") and fname.endswith(".tsv"):
        toprocess.append(inpdir+"/"+fname)
        
        
emojidict = loadDictionary(dictdir+"/emdict.txt")
termdict = loadDictionary(dictdir+"/termdict.txt")
htagdict = loadDictionary(dictdir+"/htagsdict.txt")
processPanaceaData(idtsfile, toprocess, outdir, emojidict, termdict, htagdict)

    
    
    