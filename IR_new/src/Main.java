import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

public class Main {

	private static void printAsJSON(List<Question> questions) {

		ArrayList<Answers> answersList = new ArrayList<Answers>();

		for (Question q : questions) {
			Answers answers = new Answers();
			answers.answers = q.answers;
			answers.id = q.id;
			answersList.add(answers);
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().setLongSerializationPolicy(LongSerializationPolicy.STRING)
				.create();

		System.err.println(gson.toJson(answersList));
	}

	private static ArrayList<Question> readQuestions(String filePath) throws IOException {
		ArrayList<Question> questions = new ArrayList<Question>();
		try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
			stream.forEach((line) -> {
				ArrayList<String> lineParts = new ArrayList<>(Arrays.asList(line.split("\\s+")));

				Question q = new Question();
				q.id = lineParts.get(0);
				lineParts.remove(0);
				q.body = lineParts.stream().collect(Collectors.joining(" "));
				questions.add(q);
			});
		}

		return questions;
	}

	public static void main(String[] args) {
		String nfL6path = "../scripts/nfL6.json", synPyPath = "../scripts/syn.py", questionsPath = "../questions.txt";
		Path cacheDirPath = new File("./cache/index").toPath();

		try {
			ArrayList<Question> questions = readQuestions(questionsPath);

			Pikachu pikachu = new Pikachu(nfL6path, cacheDirPath, synPyPath);
			System.out.println("Starting search");

			for (Question q : questions) {
				q.answers = pikachu.Search(q.body);
			}

			printAsJSON(questions);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
