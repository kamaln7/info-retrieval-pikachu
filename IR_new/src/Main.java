import java.io.File;
import java.nio.file.Path;

public class Main {
	public static void main(String[] args) {
		String nfL6path = "../scripts/nfL6.json", synPyPath = "../scripts/syn.py";
		Path cacheDirPath = new File("./cache/index").toPath();

		String query = "For college admission, is it better to take AP classes and get Bs or easy classes and get As?";

		try {
			Pikachu pikachu = new Pikachu(nfL6path, cacheDirPath, synPyPath);
			pikachu.Search(query);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
