#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sat May  2 08:41:15 2020

@author: sdas
"""
import random


samplesz=200000


seen={}
fin = open("agg_sampled.tsv", "r")
line = fin.readline()
line = fin.readline()
while line:
    
    parts = line.strip().split('\t')
    twid = parts[0].strip()
    seen[twid]=""
    line = fin.readline()
    
fin.close()
print ("len seen"+str(len(seen)))


#fin=open("ss.tsv", "r")
fin=open("full_dataset-clean.tsv", "r")

line = fin.readline()
#headers = line.strip().split('\t')
line = fin.readline()
linesByWeek={}
lx=2
while line:
    
    parts = line.strip().split('\t')
    
    twid = parts[0].strip()
    if twid in seen:
        line = fin.readline()
        lx+=1
        continue

    dateparts = parts[1].split("-")
    monthstr=""
    weekstr=""
    
    if int(dateparts[1])==1:
        monthstr="Jan"
    elif int(dateparts[1])==2:
        monthstr="Feb"
    elif int(dateparts[1])==3:
        monthstr="Mar"
    else:
        monthstr="Apr"
    
    
    if int(dateparts[2])<=7:
        weekstr="1"
    elif int(dateparts[2])>7 and int(dateparts[2])<=15:
        weekstr="2"
    elif int(dateparts[2])>15 and int(dateparts[2])<=21:
        weekstr="3"
    else:
        weekstr="4"
    
    key = monthstr+":"+weekstr
    
    if key not in linesByWeek:
        linesByWeek[key]=[]
        
    
    linesByWeek[key].append(lx)
        
    
    line = fin.readline()
    lx+=1
    
fin.close()

sampledLines={}

for key in linesByWeek:
    print (str(key)+" "+str(len(linesByWeek[key])))
    
    random.shuffle(linesByWeek[key])
    
    
    if len(linesByWeek[key])>samplesz:
        linesByWeek[key]=linesByWeek[key][0:samplesz]
        
    for linenum in linesByWeek[key]:
        sampledLines[linenum]=""
                
        
    
del linesByWeek



print (str(len(sampledLines)))




#fin=open("ss.tsv", "r")
fin=open("full_dataset-clean.tsv", "r")
#fout=open("sampled2.tsv", "w")
fout=open("newsample.tsv", "w")
line = fin.readline()
#headers = line.strip().split('\t')
fout.write(line)
line = fin.readline()




lx=2
while line:
    
    #parts = line.strip().split('\t')    
    
    if lx in sampledLines:
        fout.write(line)
        fout.flush()
        
    line = fin.readline()
    lx+=1
    
fin.close()
fout.close()   
