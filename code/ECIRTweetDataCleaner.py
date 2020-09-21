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
URLPAT="https://t.co/[0-9|a-z|A-Z]*"

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
ignoreht["tldr"]=""


def processDictionaries(inpmalletfile, outdictsdir):

    inpf = inpmalletfile
    outdir=outdictsdir
   
    fin = open (inpf, "r")
       
    line = fin.readline()
    lx=0
    hashtags={}
    termcounts={}
    tothtcount=0
    tottermcount=0
    while line:
        
        parts = line.split(" ")    
        twid = parts[0]
        twdate = parts[1]
        tweet = line.replace(twid,"").replace(twdate,"").strip()
        
        tweet = tweet.replace("`","").replace("'","").replace(".","").replace("-","").replace(",","")
        
        words=tweet.split()
        newtweet=""
        
        for word in words:
            
            if word.startswith("#"):
               htag = re.sub("[^\P{P}-]+", "", word[1:])
               
               htag = htag.lower().strip()
               
               
               
               if len(htag)<1:
                   continue
               
               tothtcount+=1                         
               if htag in hashtags:
                   hashtags[htag]+=1
               else:
                   hashtags[htag]=1                   
            
               continue
                
            if word.startswith("@"):
                continue
            
            newwords = re.sub("[^\P{P}-]+", " ", word).lower().strip().split()
            for nword in newwords:            
                if len(nword)>0:
                    newtweet+=" "+nword
                
        
        
            
        
        newtweet = newtweet.encode('ascii', 'ignore').decode('ascii').lower().strip()
        twwords = newtweet.split()
        if lx%50000==0:
            print (tweet)
            print (newtweet)
            
        for term in twwords:
            
            term = term.strip()
            if len(term)<1:
                   continue
            tottermcount+=1
            if term in termcounts:
                termcounts[term]+=1
            else:
                termcounts[term]=1
#        
#        
#        
#        if len(tweet)>0:
#            words
            
            
            
            
        lx+=1
        line = fin.readline()
        
        
        
    fin.close()

    print (len(hashtags))
    print (len(termcounts))
    
    fout = open (outdir+"/hashtags.dict", "w")
    for word in hashtags:
        if hashtags[word]>10:
            if "corona" in word or "ncov" in word or "covid" in word or "wuhan" in word or "virus" in word \
            or "sarscov2" in word or "china" in word or "chinese" in word or "covd19" in word:
                print ("discarding "+word)
                continue
            idf = np.log(tothtcount/hashtags[word])
            fout.write(word+" "+str(hashtags[word])+" "+str(idf)+"\n")

    fout.close()
    
    keep={}
    fout = open (outdir+"/terms.dict", "w")
    for word in termcounts:
        if termcounts[word]>10:
            idf = np.log(tottermcount/termcounts[word])
            fout.write(word+" "+str(termcounts[word])+" "+str(idf)+"\n")
            termid=len(keep)
            keep[word]=termid       
            
    fout.close()

    print ("len(keep)="+str(len(keep)))
    
    
    return


    



def splitTweetContent(inpf, \
                           termdictf, htdictf, emojidictf, slangdictf, \
                           outemof, outcontf, outemodictf, outhtlistf):
    
    normhtdict={}
    
    print ("Ignorelist "+str(len(ignoreht)))
    
    fout = open(outemodictf, "w")
    emojidict={}
    fin = open (emojidictf, "r")
    for line in fin.readlines():
        words = line.strip().split()
        emoji_emoticon = line.replace(words[len(words)-1],"").strip()
        emojidict [emoji_emoticon] = len(emojidict)
        fout.write("EM"+str(emojidict [emoji_emoticon])+" "+str(emoji_emoticon)+"\n")
    fin.close()
    print ("sz of emoji dict "+str(len(emojidict)))
    
    slangdict={}
    fin = open (slangdictf, "r")
    for line in fin.readlines():
        
        slangexpr = line.strip().lower()
        slangdict [slangexpr] = len(slangdict)
        fout.write("SL"+str(slangdict [slangexpr])+" "+str(slangexpr)+"\n")
        
    fin.close()
    print ("sz of slang dict "+str(len(slangdict)))
    fout.close()
    
    normhtdict={}
    fin = open (htdictf, "r")
    for line in fin.readlines():
        
        words = line.strip().split()
        line = line.replace(words[len(words)-1],"").replace(words[len(words)-2],"").strip()
        normhtdict [line] = len(normhtdict)        
        
    fin.close()
    print ("sz of htdict "+str(len(normhtdict)))
    
    
    termdict={}
    fin = open (termdictf, "r")
    for line in fin.readlines():
        
        words = line.strip().split()
        line = line.replace(words[len(words)-1],"").replace(words[len(words)-2],"").strip()
        termdict [line] = len(termdict)        
        
    fin.close()
    print ("sz of termdict "+str(len(termdict)))
    
    
    
    
    
    
    
    
    
    
    
    fin = open (inpf, "r")
    lx=0   
    line = fin.readline()
    foute = open (outemof, "w")
    foutc = open (outcontf, "w")
    while line:
    
        if lx%10000==0:
            print ("Processed "+str(lx)+" tweets")
            
        
        parts = line.split(" ")    
        twid = parts[0]
        twdate = parts[1]
        tweet = line.replace(twid,"").replace(twdate,"").strip()
        emojis = tn.parseEmojis(tweet)
        temp = emot.emoticons(tweet) #tn.removeHTAtEmoji(tweet))
        emoticons=[]
        if 'value' in temp:
            emoticons = temp['value']
        
        tempe  = ""
        for emoticon in emoticons:
            if emoticon in emojidict:
                tempe += " EM"+str(emojidict[emoticon])
        
        for emoji in emojis:
            if emoji in emojidict:
                tempe += " EM"+str(emojidict[emoji])
        
        tweetnoemoji = tn.removeAtEmoji(tweet)
      
        tweetnorm = tweetnoemoji.lower().replace("`","").replace("'","").replace("."," ").replace("-"," ").replace(","," ")
        for slang in slangdict:
            
            if (" "+slang+" ") in tweetnorm:
                tweetnorm = tweetnorm.replace(" "+slang+" "," ")
                if lx<10000:
                    print("before slang removal "+tweet)    
                    print("after slang removal "+tweetnorm) 
                tempe +=" SL"+str(slangdict[slang])
            
            if tweetnorm.startswith(slang+" "):
                tweetnorm = tweetnorm.replace(slang+" "," ")
                tempe +=" SL"+str(slangdict[slang])
                if lx<10000:
                    print("before slang removal "+tweet) 
                    print("after slang removal "+tweetnorm) 
            if tweetnorm.endswith(" "+slang):
                tweetnorm = tweetnorm.replace(" "+slang," ")
                tempe +=" SL"+str(slangdict[slang])
                if lx<10000:
                    print("before slang removal "+tweet) 
                    print("after slang removal "+tweetnorm) 
                
        hts = {}
        
        
        tempc=""
        words = tweetnorm.split()
        for word in words:
            if word.startswith("#"):
                shouldignore=False
                for iword in ignoreht:                    
                    if word.startswith("#"+iword):
                       shouldignore=True 
                       break
                
                if shouldignore==False:
                    httmp = word.replace("#","GSDASHT").translate(str.maketrans("","", string.punctuation)).strip()
                    if httmp!="GSDASHT":
                        hts[httmp]=""  
                        if httmp not in normhtlist:
                            normhtlist[httmp]={}
                            normhtcounts[httmp]=0
                        
                        normhtlist[httmp][word]=""
                        normhtcounts[httmp]+=1
                        
            else:
                t = str.maketrans(dict.fromkeys(string.punctuation, " "))
                word = word.translate(t).strip()
                swords = word.split()
                for wordp in swords:
                    if wordp not in ignoreht:
                        tempc += " "+wordp
        
        for ht in hts:
            tempc += " "+ht
        
        
        tempc = tempc.strip()
        tempe = tempe.strip()
        if len(tempe)>0 and len(tempc)>0:
            
            foute.write(twid+" "+twdate+" "+tempe+"\n")        
            foutc.write(twid+" "+twdate+" "+tempc+ " ""\n")
        
        if lx<500:
            print (line.strip())            
            print (tempe)
            print (tempc)
            print()
        
        lx+=1
        line = fin.readline()
        
    
    
    fin.close()
    foutc.close()
    foute.close()
    
    outf = open (outhtlistf, "w")
    
    for normhtag in normhtlist:
        htags=normhtlist[normhtag]
        top=""
        for htag in htags:
            top+=" "+htag
    
        outf.write(normhtag+" "+str(normhtcounts[normhtag])+" "+top.strip()+"\n")
        
        
    outf.close()



    
def processPanaceaData(idtsfile, contfile, outdir):
    
    
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
    fout = open (outdir+"/entweets.mallet.in", "w")
    
    fin = open (contfile, "r")
        
 #   header = fin.readline()
    line = fin.readline()
    
    
    while line:
        
        parts = line.split("\t")    
        twid = parts[0].strip()
        tweet = parts[1]
        lang = parts[5].strip()
        
        if lang=="en" and twid in timeinfo:
            
            cleanedtweet = re.sub(URLPAT," ", tweet)        
            newsent=""
            words = cleanedtweet.strip().split()
            for word in words:
                if word.startswith("@"):
                    continue
                
                newsent +=" "+word
    
            newsent = newsent.strip()
            
            if len(newsent)>0:
                
                datei=timeinfo[twid]
                fout.write(twid+" "+datei+" "+newsent+"\n")
                
                if datei in bydatecounts:
                    bydatecounts[datei]+=1
                else:
                    bydatecounts[datei]=1
                    
                
        lx += 1 
        line = fin.readline()
        
        
    fin.close()
            
    fout.close()
    print ("lx="+str(lx))
    for datei in bydatecounts:
        print (datei+" "+str(bydatecounts[datei]))
    