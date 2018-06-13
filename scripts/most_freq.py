from collections import Counter
import re


# read file
filepath = './top_300_answers.txt'
file = open(filepath)
data_set = file.read()


data_set = re.sub(r'\.', ' ', data_set)

# split to words
split_it = data_set.split()

# return most_common
Counter = Counter(split_it)
most_occur = Counter.most_common(10)

print(most_occur)
