#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri Apr 24 18:11:00 2020

@author: sdas
"""
import operator
import numpy as np
import twNormalizer as tn

prefix="/home/sdas/cord/covidtweets"
outdir="/home/sdas/cord/covidtweets/tweetsforLDA"

dmapped={}
counts=np.zeros(4)
with open (prefix+"/id_timestamp.tsv", "r") as f:
    
    lines = f.readlines()
    for lx, line in enumerate(lines):
        if lx>0:
            parts = line.split('\t')
            twid = parts[0]
            dmapped[twid]=parts[1]
            dateinfo = parts[1].split("-")[2]
            if int(dateinfo)<=7:
                counts[0]+=1
            elif int(dateinfo)>7 and int(dateinfo)<=14:
                counts[1]+=1
            elif int(dateinfo)>14 and int(dateinfo)<=21:
                counts[2]+=1
            else:
                counts[3]+=1
                
                
f.close()
print (counts)
encnt=0
usercnts={}
fout = open(prefix+"/ss_hydrated.mallet.in", "w")

with open (prefix+"/ss_hydrated.tsv", "r") as f:
    cnt=0
    line = f.readline() #header
    
    while line:
        
        if cnt==0:
            line = f.readline()
            cnt += 1
            continue
        
        parts = line.strip().split('\t')
        twid = parts[0]
        if twid in dmapped:
            twtxt = parts[1]
            lng = parts[4]
            userid = parts[5]
            if lng=="en":
#               encnt+=1             
#               if userid in usercnts:
#                   usercnts[userid]+=1
#               else:
#                   usercnts[userid]=1
                
                ntweet = tn.normalizeRemoveEmoji(twtxt).strip()
                if len(ntweet)>0:
                    fout.write(twid+" "+dmapped[twid]+" "+ntweet+"\n")
                
               
        cnt += 1
        line = f.readline()
    
    
print ('count '+str(cnt))
print ('en count '+str(encnt))
f.close()
fout.close()

def runExpt():
        
    vals=[]
    for userid in usercnts:    
        vals.append((userid, usercnts[userid]))
        
        
    vals.sort(key = operator.itemgetter(1), reverse = True)
    topusers={}
    for vx, val in enumerate(vals):
        (userid, cnt) = val
        
        if vx<20:
            print (val)
        if vx==1000:
            break
        else:
            topusers[userid]=cnt
    
    
    print ("#topusers "+str(len(topusers)))
    
    with open (prefix+"/ss_hydrated.tsv", "r") as f:
        cnt=0
        line = f.readline() #header
        
        while line:
            
            if cnt==0:
                line = f.readline()
                cnt += 1
                continue
            
            if cnt%2000==0:
                print('processed '+str(cnt))
            
                
            parts = line.strip().split('\t')
            twid = parts[0]
            
            if twid in dmapped:
                twtxt = parts[1]
                lng = parts[4]
                userid = parts[5]
                if lng=="en" and userid in topusers:
                    tempf = open(outdir+"/"+userid+".txt", "a")
                    ntweet = tn.normalizeTweet(twtxt).strip()
                    if len(ntweet)>0:
                        tempf.write(ntweet+"\n")
                        tempf.close()
    
    
            cnt += 1
            line = f.readline()
            
            
    f.close()
        