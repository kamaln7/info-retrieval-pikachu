from itertools import chain
from nltk.corpus import wordnet
from itertools import product
import sys

# print("Hello World from python")

# synonyms = wordnet.synsets('big')
# list1 = ['big']
# list2 = set(chain.from_iterable([word.lemma_names() for word in synonyms]))
# if list2:
#    print(list(list2)[0], list(list2)[1], list(list2)[2], list(list2)[3], list(list2)[4])

# for word1, word2 in product(list1, list2):
#    wordFromList1 = wordnet.synsets(word1)
#    wordFromList2 = wordnet.synsets(word2)
#    if wordFromList1 and wordFromList2:
#        s = wordFromList1[0].wup_similarity(wordFromList2[0])
#        print(s)

wordx = sys.argv[1]
# wordx = 'european'
sem1 = wordnet.synsets(wordx.lower())
flag = 1
# maxscore = 0.0
# score = 0.0

for ss in sem1:
    if flag == 0:
        break
    for word in ss.lemma_names():
        if word.lower() != wordx:
            print(word)
            flag = 0
            break

# sem1 = sem1[0].lemma_names()
# print(sem1)
# for i in sem1:
    # score = i.wup_similarity(sem1[0])
    # i = i.name().split(".")[0].replace('_', ' ')
    # if i != wordx:
        # print(i)
        # break
    # if score and maxscore < score:
        # maxscore = score
        # wordx1 = i
        # wordx2 = sem1[0]

# wordx1 = wordx1.name().split(".")[0].replace('_', ' ')
# wordx2 = wordx2.name().split(".")[0].replace('_', ' ')

# if wordx1 == wordx:
    # print(wordx2)
# else:
    # print(wordx1)

# print(maxscore, wordx1, wordx2)



