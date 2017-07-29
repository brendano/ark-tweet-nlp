__author__ = 'KevinZhao'
# You can use this to run the CMU Tweet NLP package(http://www.ark.cs.cmu.edu/TweetNLP/)
# First, download the package at https://github.com/brendano/ark-tweet-nlp/
# Second, put everything in the project directory where you are running the python script

import subprocess
import codecs
import os
import psutil
import tempfile

def runFile(fileName):
    p = subprocess.Popen('java -XX:ParallelGCThreads=2 -Xmx500m -jar ark-tweet-nlp-0.3.2.jar "'+ fileName + '"',stdout=subprocess.PIPE)
    file_name = 'tagged_tweets_%s.txt' % os.getpid()
    o = codecs.open(file_name,'w','utf-8')
    while p.poll() is None:
        l = p.stdout.readline()
        o.write(l.decode('utf-8'))
        o.flush()
    o.close()

def runString(s):
    file_name = 'temp_file_%s.txt' % os.getpid()
    o = codecs.open(file_name,'w','utf-8')
    uniS = s.decode('utf-8')
    o.write(uniS)
    o.close()
    l = ''
    p = subprocess.Popen('java -XX:ParallelGCThreads=2 -Xmx500m -jar ark-tweet-nlp-0.3.2.jar ' + file_name,stdout=subprocess.PIPE)

    while p.poll() is None:
        l = p.stdout.readline()
        break

    p.kill()
    psutil.pids()

    os.remove(file_name)
    #Running one tweet at a time takes much longer time because of restarting the tagger
    #we recommend putting all sentences into one file and then tag the whole file, use the runFile method shown above
    return l
