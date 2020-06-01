#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sat May  2 11:26:31 2020

@author: sdas
"""
from operator import itemgetter
import twNormalizer as tn
import emot




prefix = "/home/sdas/submissions/coling2020/expts/unified_datasets"
semevalf=prefix + "/semval2018_unified.txt"
outfile = "/home/sdas/cord/covidtweets/panacea/may12data/saifemojis.thresh10.list"
fin = open(semevalf, 'r')
lines = fin.readlines()
fin.close()

emocounts={}    
emoid2name={}

emojiemocounts={}
emojicounts={}




for lx, line in enumerate(lines):
    
    
    parts=line.strip().split('\t')
    #print (len(parts))
    if lx==0:
        
        for px in range(2,len(parts)):
            emocounts[parts[px]]=0
            emoid2name[px]=parts[px]    
        
        
    else:
        tweet=(parts[1])
        emojis = tn.parseEmojis(tweet)
        temp2 = emot.emoticons(tweet) #tn.removeHTAtEmoji(tweet))
        emoticons=[]
        if 'value' in temp2:
            emoticons = temp2['value']
        
        emojis.extend(emoticons)
        
        emosthistweet=[]        
        for px in range(2,len(parts)):
                
            emoval = int(parts[px])
            if emoval!=0:
                emosthistweet.append(emoid2name[px])

        if len(emosthistweet)>0 and len(emojis)>0:
            
            for emoji in emojis:
                if emoji not in emojicounts:
                    emojicounts[emoji]=0
                
                emojicounts[emoji]+=1
                
                for emo in emosthistweet:
                    
                    if emoji not in emojiemocounts:
                        emojiemocounts[emoji]={}
                    if emo not in emojiemocounts[emoji]:
                        emojiemocounts[emoji][emo]=0
                    
                    emojiemocounts[emoji][emo]+=1
                        
                    

outf = open (outfile, "w")
for emoji in emojicounts:        
    if emojicounts[emoji]>5:
        temp=[]
        for emo in emojiemocounts[emoji]:
            temp.append((emo, emojiemocounts[emoji][emo]))
        
        sorted_by_second = sorted(temp, key=lambda tup: tup[1], reverse=True)
        print (emoji+" "+str(sorted_by_second[0:2]))
        outf.write (emoji+" "+str(sorted_by_second[0:2])+"\n")
            

outf.close()



def extractPats1():
    
    prefix = "/home/sdas/submissions/coling2020/expts/unified_datasets"
    semevalf=prefix + "/semval2018_unified.txt"
        
    fin = open(semevalf, 'r')
    lines = fin.readlines()
    fin.close()
    
    emocounts={}
    
    emoid2name={}
    
    emocounts["neutral"]=0
    emoemojicounts={}
    emojiemocounts={}
    emojicounts={}
    noemojis=0
    
    
    
    for lx, line in enumerate(lines):
        
        
        parts=line.strip().split('\t')
        #print (len(parts))
        if lx==0:
            
            for px in range(2,len(parts)):
                emocounts[parts[px]]=0
                emoid2name[px]=parts[px]    
            
            
        else:
            tweet=(parts[1])
            emojis = tn.parseEmojis(tweet)
            temp2 = emot.emoticons(tweet) #tn.removeHTAtEmoji(tweet))
            emoticons=[]
            if 'value' in temp2:
                emoticons = temp2['value']
            
            emojis.extend(emoticons)
            
            foundnz=False
    
            emosthistweet=[]        
            for px in range(2,len(parts)):
                    
                emoval = int(parts[px])
                if emoval!=0:
                    foundnz=True
                    emocounts[emoid2name[px]]+=1
                    emosthistweet.append(emoid2name[px])
    
            if len(emojis)==0:
                noemojis+=1            
                emojis.append("NOemoji")
    
    
                    
            if foundnz==False:
                emocounts["neutral"]+=1
                emosthistweet.append("neutral")
    
            
            
            for emo in emosthistweet:
                
                if emo not in emoemojicounts:
                    emoemojicounts[emo]={}
                
                temp = emoemojicounts[emo]
                for emoji in emojis:
                    if emoji not in temp:
                        temp[emoji]=0
                    
                    temp[emoji]+=1
                
                
                emoemojicounts[emo]=temp
            
             
            for emoji in emojis:
                
                if emoji not in emojiemocounts:
                    emojiemocounts[emoji]={}
                    
                    
                    
                temp = emojiemocounts[emoji]
                for emo in emosthistweet:
                    if emo not in temp:
                        temp[emo]=0
                        
                    temp[emo]+=1
                    
                emojiemocounts[emoji] = temp
            
    
    print ("total lines "+str(len(lines)))
    print ("total noemoji lines "+str(noemojis))
    print (emoid2name)
    print (emocounts)
    
    print ("#emojis seen "+str(len(emoemojicounts)))
    cthresh=5
    fout = open("/home/sdas/cord/covidtweets/panacea/may12emorun/saif.emojis_stats.txt", "w")
    
    for emoji in emojiemocounts:
        temp = emojiemocounts[emoji]
        for emo in temp:
            if temp[emo]>cthresh:
                fout.write(emoji+" "+emo+" "+str(temp[emo])+"\n")            
        
    
    fout.close()
    
    
    
#cthresh=5
#for emo in emoemojicounts:
#    
#    fout = open("/home/sdas/cord/covidtweets/panacea/processed/"+emo+".emojis.txt", "w")
#    emojicounts = emoemojicounts[emo]
#    for emoji in emojicounts:
#        
#        if emojicounts[emoji] > cthresh:
#            fout.write(emoji+" "+str(emojicounts[emoji])+"\n")
#            
#        
#    fout.close()
    
       
       
#tosort=[]
#for emoji in emojicounts:
    #tosort.append((emoji, emojicounts[emoji]))
#    
#tosort = sorted(tosort , key=itemgetter(1), reverse=True)
#
#
#for tx, emojituple in enumerate(tosort):
#    
#    if tx==20: 
#        break
#    
#    (emoji, count) = emojituple
#    
#    print (emoji+" "+str(count))
#    
#    temp = (emoemojicounts[emoji])
#
#    tosort=[]
#    for emo in temp:
#        tosort.append((emo, temp[emo]))
#    
#    tosort = sorted(tosort , key=itemgetter(1), reverse=True)
#    print (tosort)
#    