#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed Sep 23 15:02:18 2020

@author: sdas
"""
from operator import itemgetter

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






dictdir="/home/sdas/cord/ecir/semeval/dicts"
termdict = loadDictionary(dictdir+"/termdict.txt")






trmalletf="/home/sdas/cord/ecir/semeval/trdata/textcontent.mallet.in"
tsmalletf="/home/sdas/cord/ecir/semeval/tsdata/textcontent.mallet.in"
assignmentsf="/home/sdas/cord/ecir/semeval/data/assignments.txt"
outfile="/home/sdas/cord/ecir/semeval/data/ldfeats.dat"

fin = open (trmalletf, "r")
lines = fin.readlines()
fin.close()

trids={}
for line in lines:
    parts = line.split()    
    instid = parts[0]
    trids[instid]=""



fin = open (tsmalletf, "r")
lines = fin.readlines()
fin.close()
tsids={}
for line in lines:
    parts = line.split()    
    instid = parts[0]
    tsids[instid]=""

print ("#train "+str(len(trids)))
print ("#test "+str(len(tsids)))

fin = open (assignmentsf, "r")
lines = fin.readlines()
fin.close()

counts={}
labels2id={}
for line in lines:
    parts = line.split()
    
    instid = parts[0]
    if instid not in trids:
        continue
    emoassignment = (parts[len(parts)-1])
    
    labels = parts[1][1:].split(":")
    for label in labels:
        if label not in labels2id:
            labels2id[label]=len(labels2id)+1
            
        key = emoassignment
        if key not in counts:
            counts[key]={}
        if label not in counts[key]:
            counts[key][label]=0
        counts[key][label]+=1

ldfeats={}
for key in counts:
    print (key)
    tot=0
    for emo in counts[key]:
        tot += counts[key][emo]
    
    temp=[]
    for emo in counts[key]:
        temp.append((labels2id[emo], counts[key][emo]/tot))

    ldfeats[key]=sorted(temp, key=itemgetter(0))
    
fout = open(outfile, "w")
for line in lines:
    parts = line.split()
    
    instid = parts[0]
    emoassignment = (parts[len(parts)-1])
    
    top=""
    for p in ldfeats[emoassignment]:
        (lid, val) = p
        top +=" "+str(lid)+":"+str(val)
    
    fout.write(instid+" "+top.strip()+"\n")

fout.close()