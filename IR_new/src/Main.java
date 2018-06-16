import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Main {

	private static void printAsJSON(List<Question> questions) {
		Answers answers = new Answers();

		for (Question q : questions) {
			answers.answers = q.answers;
			answers.id = q.id;
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println(gson.toJson(answers));
	}

	public static void main(String[] args) {
		String nfL6path = "../scripts/nfL6.json", synPyPath = "../scripts/syn.py";
		Path cacheDirPath = new File("./cache/index").toPath();

		ArrayList<Question> questions = new ArrayList<Question>();
		Question q1 = new Question();
		q1.id = "123";
		q1.body = "For college admission, is it better to take AP classes and get Bs or easy classes and get As?";
		questions.add(q1);

		try {
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
