#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed May  6 15:07:18 2020

@author: sdas
"""

from operator import itemgetter
#import os
import regex as re
import numpy as np
#import emot
#import string
import twNormalizer as tn
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



    
def processPanaceaData(idtsfile, conttsvfile, outdir):
    
    
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
    bydatecounts={}
    htdict={}
    termdict={}
    emojidict={}
    
    id2text={}
    id2htags={}
    id2emoji={}
    
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
            
            for emoji in emojis:
                emlist = tn.parseEmojis(emoji)
                for em in emlist:
                    if em not in emojidict:
                        emojidict[em]=1
                    else:
                        emojidict[em]+=1
            
            for htag in htags:
                htag = htag.lower()
                if htag not in htdict and htag not in ignoreht:
                    htdict[htag]=1
                elif htag in htdict:
                    htdict[htag] += 1
                
            newsent=""
            words = tweet.strip().split()
            for word in words:
                if word.startswith("@"):
                    continue
                
                found=False
                for htag in htags:
                    if "#"+htag in word.lower():
                        found=True
                        break
                
                if not found:
                    newsent +=" "+word
    
            newsent = newsent.strip()
            
            
            if len(newsent)>0:
                
                datei=timeinfo[twid]
                
                if datei in bydatecounts:
                    bydatecounts[datei]+=1
                else:
                    bydatecounts[datei]=1
                
                
                words = re.sub("[^\P{P}-]+", " ", newsent).lower().strip().split()
                
                for word in words:
                    if word.strip()!="":
                        
                        if word not in termdict and word not in ignoreht:
                            termdict[word]=1
                        elif word in termdict:
                            termdict[word] += 1
               
                id2text[twid]=newsent
                id2htags[twid]=htags
                id2emoji[twid]=emojis
                
                
                
        lx += 1 
        line = fin.readline()
        
        
    fin.close()
            
    
    print ("lx="+str(lx))
    for datei in bydatecounts:
        print (datei+" "+str(bydatecounts[datei]))
    