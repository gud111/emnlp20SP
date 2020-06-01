#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu May  7 14:19:42 2020

@author: sdas
"""
import re
import string
import twNormalizer as tn

ignoreht={}
ignoreht["corona"]=""
ignoreht["covd19"]=""
ignoreht["ncov"]=""
ignoreht["covid"]=""
ignoreht["wuhan"]=""
ignoreht["ncov"]=""
ignoreht["virus"]=""
ignoreht["sarscov2"]=""
ignoreht["china"]=""
ignoreht["chinese"]=""

def splitByWeek(inpf, outd):
    
    fin = open (inpf, "r")
    
    line = fin.readline()
    lx=0
    sets={}
    while line:
        
        parts = line.split(" ")    
        #twid = parts[0]
        twdate = parts[1].strip()
        month = int(twdate.split("-")[1])
        day = int(twdate.split("-")[2])
        weekstr=""
        if day <= 7:
            weekstr="w1"
        elif day >7 and day<=14:
            weekstr="w2"
        elif day >14 and day<=21:
            weekstr="w3"
        else:
            weekstr="w4"
        
        monthstr=""
        if month==1:
            monthstr="jan"
        elif month==2:
            monthstr="feb"
        elif month==3:
            monthstr="mar"
        elif month==4:
            monthstr="apr"
        else:
            monthstr="may"
            
        key = weekstr+":"+monthstr
        
        if key not in sets:
            sets[key]=[]
            
        sets[key].append(line.replace(twdate, key).strip())

        lx+=1
        line = fin.readline()
        
    
    fin.close()
    
    
    for key in sets:
        outf = outd +"/"+key+".mallet.in"
        lines = sets[key]
        print ("#lines for "+key+" "+str(len(lines)))
               
        if len(lines)>1000:
            
            fout = open (outf, "w")
            for line in lines:
                fout.write(line.strip()+"\n")
        
            fout.close()
    
    return
    



def splitFiles(inpf, outdir):
    
    fin = open (inpf, "r")
    
    sets={}
    htagsbyset={}
    
    line = fin.readline()
    lx=0

    while line:
        
        parts = line.split(" ")    
        twid = parts[0]
        twdate = parts[1]

        tweet = line.replace(twid,"").replace(twdate,"").strip()
        tweet = tweet.replace("`","").replace("'","").replace(".","").replace("-","").replace(",","")

                
        words=tweet.split()
        
        for word in words:
            if word.startswith("#"):
                
                htag = word.translate(str.maketrans("","", string.punctuation)).lower().strip()
                foundi=False
                
                if len(htag)==0:
                    continue
                
                for iword in ignoreht:
                    if iword in htag:
                        foundi=True
                        break
                
                if not foundi:
                    if twdate not in htagsbyset:
                        htagsbyset[twdate] = {}
                        
                    htagtmp = htagsbyset[twdate]
                    if htag in htagtmp:
                        htagtmp[htag]+=1
                    else:
                        htagtmp[htag]=1
                    
                    htagsbyset[twdate] = htagtmp
            
#            elif word.startswith("@"):
#                continue
            
        if twdate not in sets:
            sets[twdate]={}
        
        sets[twdate][lx]=""
            
        lx+=1
        line = fin.readline()
        
        
    fin.close()
    
    
    for twdate in sets:
        print (twdate)        
        
        print (len(sets[twdate]))
        if twdate in htagsbyset:
            print (len(htagsbyset[twdate]))
            fout = open (outdir+"/"+twdate+".htags.txt", "w")
        
            for htag in htagsbyset[twdate]:
                fout.write(htag+" "+str(htagsbyset[twdate][htag])+"\n")
        
            fout.close()
            
            
        if len(sets[twdate]) < 500:
            continue
        
        print ("Processing "+twdate)
        
        fout = open (outdir+"/"+twdate+".mallet.in", "w")
        fin = open (inpf, "r")
        
        
        line = fin.readline()
        lx=0

        while line:
        
            parts = line.split(" ")    
            twid = parts[0]
            twdate = parts[1]
    
            if lx in sets[twdate]:

                tweet = line.replace(twid,"").replace(twdate,"").strip()
                tweet = tweet.replace("`","").replace("'","").replace(".","").replace("-","").replace(",","")            
                newtweet = tn.removeHTAtEmoji(tweet)
                if len(newtweet)>0:
                    fout.write(twid+" "+twdate+" "+newtweet.strip()+"\n")
    
    
            lx+=1
            line = fin.readline()
        
        
        fin.close()
        fout.close()
    