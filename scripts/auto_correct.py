import AutoCorrect
import re

my_dictionary = AutoCorrect.Dictionary()

filepath = './all_words.txt'
with open(filepath) as fp:
    line = fp.readline()
    while line:
        # print(line)
        my_dictionary.learn_word(line)
        line = fp.readline()

filepath = './corpus_answers.txt'
text_file = open("Output1.txt", "w")

'''with open(filepath) as fp:
    line = fp.readline()
    while line:
        wordList = re.sub("[^\w]", " ", line).split()

        for word in wordList:
            line.replace(word, my_dictionary.correct_text(text=word))
        print(1)
        text_file.write("%s" % line)
        line = fp.readline()
'''

#text_file.close()
corrected_text = my_dictionary.correct_text(text='helo')
print(corrected_text)
