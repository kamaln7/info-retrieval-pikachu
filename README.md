This is our submission for the Information Retrieval course of Spring 2018.

Submitters:

* Kamal Nasser
* Emil Jahshan

## Structure

The Eclipse project is located in the `IR_new` directory. Java libraries are located in `IR_new/lib`, and our source code is in `IR_new/src`.

## Dependencies

The python scripts in the scripts folder require python 3. To install the required python dependencies, run:

```
pip3 install --upgrade nltk
```

Then, inside a python3 REPL, run:

```python
>>> import nltk
>>> nltk.download('all')
```

This will download the latest nltk corpi.

## Running Pikachu

To run the Pikachu search engine:

Either adjust the paths in Main.java and Pikachu.java and execute the `pikachu.jar` file inside `IR_new` (in that directory!); or:

1. Compile the code into an executable .jar file using Eclipse.
2. Prepare nfL6.json and questions.txt (per the evaluation format).
3. Place questions.txt outside next to the IR_new directory and name it finalEval.txt
4. Place nfL6.json in the scripts directory. It was removed from the repository due to GitHub not allowing files larger than 100MB.
5. `cd` into the `IR_new` directory. The program must be run there.
6. Run the jar file `java -jar pikachu.jar`, inside `IR_new`.