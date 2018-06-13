from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
import sys

stop_words = set(stopwords.words('english'))

file1 = open("nfL6.json", encoding="utf-8")
line = file1.read()
words = line.split()
for r in words:
    if not r in stop_words:
        appendFile = open('filteredtext.txt', 'a')
        appendFile.write(" "+r)
        appendFile.close()
