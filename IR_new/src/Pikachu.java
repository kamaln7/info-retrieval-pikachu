import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

public class Pikachu {
	public static final String BODY_FIELD = "body";
	public static final String[] WH_STOP_WORDS = { "how", "what", "where", "who", "when", "which", "whom", "whose",
			"why", "do" };
	public static final Map<String, List<String>> hardcodedSynonyms = new HashMap<String, List<String>>() {
		public static final long serialVersionUID = 1L;

		{
			put("why", Arrays.asList(new String[] { "because", "coz", "cuz" }));
			put("when", Arrays.asList(new String[] { "at" }));
			put("which", Arrays.asList(new String[] { "one" }));
		}
	};
	public String nfL6path, synPyPath;
	public Path cacheDirPath = new File("./cache/index").toPath();

	public Directory cacheDirectory;
	public Analyzer analyzer;
	public QueryParser qp;
	public IndexSearcher searcher;
	public DirectoryReader reader;

	public Pikachu(String nfL6path, Path cacheDirectory, String synPyPath) throws IOException {
		this.nfL6path = nfL6path;
		this.cacheDirPath = cacheDirectory;
		this.synPyPath = synPyPath;

		this.setAnalyzer();
		this.qp = new QueryParser(BODY_FIELD, this.analyzer);
		this.cacheDirectory = FSDirectory.open(cacheDirPath);
		this.reader = DirectoryReader.open(this.cacheDirectory);
		this.searcher = new IndexSearcher(reader);
		searcher.setSimilarity(new BM25Similarity());
		// Index
		if (Files.notExists(cacheDirPath)) {
			buildIndex();
		}
	}

	private IndexWriterConfig newIndexWriterConfig(Analyzer analyzer) {
		return new IndexWriterConfig(analyzer).setOpenMode(OpenMode.CREATE).setCodec(new SimpleTextCodec())
				.setCommitOnClose(true);
	}

	public void buildIndex() throws IOException {
		// parse json
		Gson gson = new Gson();
		nfL6Entry[] entries;
		try (JsonReader nfL6Reader = new JsonReader(new FileReader(nfL6path))) {
			entries = gson.fromJson(nfL6Reader, nfL6Entry[].class);
		}

		// build index
		try (IndexWriter writer = new IndexWriter(this.cacheDirectory, newIndexWriterConfig(this.analyzer))) {
			for (nfL6Entry entry : entries) {
				ArrayList<String> answers = new ArrayList<String>();
				answers.addAll(entry.NBestAnswers);

				for (String answer : answers) {
					Document doc = new Document();

					// THIS IS NOT INDEXED!
					// StoredField: Stored-only value for retrieving in summary results
					// we only store the question ID in case we need to use it later
					doc.add(new StoredField("questionID", entry.ID));

					doc.add(new TextField(BODY_FIELD, answer, Store.YES));
					writer.addDocument(doc);
				}
			}
		}
	}

	public void Search(String query) throws Exception {
		List<String> query_words = new LinkedList<String>();

		// delete: ? , . ! " ^
		query = query.replaceAll("[\\?,\\.!\"\\^]*$", "");

		for (String word : query.split("\\s+")) {
			query_words.add(word);

			String synKey = word.toLowerCase();
			if (hardcodedSynonyms.containsKey(synKey)) {
				for (String synonym : hardcodedSynonyms.get(synKey)) {
					query_words.add(synonym);
				}
			}
		}

		query_words = addSynonyms(query_words);

		String new_query = query_words.stream().map((x) -> String.format("body:%s", x))
				.collect(Collectors.joining(" "));

		System.out.printf("Original Query: %s\n", query);
		System.out.printf("Modified Query: %s\n", new_query);

		runQuery(new_query);
	}

	void runQuery(String query) throws ParseException, IOException, FileNotFoundException {
		Query q = this.qp.parse(query);
		System.out.printf("Parsed Query: %s\n\n", q);

		TopDocs td = searcher.search(q, 300);

		ArrayList<Answer> answer_list = new ArrayList<Answer>();
		Answers big_answer = new Answers();

		int count = 0;
		int count_300 = 0;

		PrintStream out = new PrintStream(new FileOutputStream("../scripts/top_300_answers.txt"));
		for (ScoreDoc sd : td.scoreDocs) {
			Document doc = searcher.doc(sd.doc);

			Answer answer = new Answer();
			answer.setAnswer(doc.get(BODY_FIELD));
			answer.setScore(sd.score);

			if (count == 4) {
				answer_list.add(answer);
				big_answer.setAnswers(answer_list);
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				String strJson = gson.toJson(big_answer);
				// System.out.println(String.format("doc=%d, score=%.4f, text=%s snippet=%s",
				// sd.doc, sd.score,
				// doc.get(BODY_FIELD), Arrays.stream(snippets).collect(Collectors.joining("
				// "))));

				System.out.println(strJson);
				// break;
			}

			answer_list.add(answer);
			count++;

			if (count_300 < 300) {
				out.println(doc.get(BODY_FIELD));
				count_300++;

				if (count_300 < 50) {
					System.out.println(
							String.format("doc=%d, score=%.4f, text=%s", sd.doc, sd.score, doc.get(BODY_FIELD)));
				}
			}
		} // end of top-docs 3

		// String[] most_freq_words = countMostFreqWord();
	}

	private List<String> addSynonyms(List<String> query_words) throws IOException, InterruptedException {
		ArrayList<String> result = new ArrayList<String>();

		Map<String, List<String>> allSynonyms = getSynonyms(query_words);

		for (String word : query_words) {
			result.add(String.format("%s^2", word));

			String key = word.toLowerCase();
			if (allSynonyms.containsKey(key)) {
				// replace underscores with spaces and quote each phrase
				// give priority to original query ^2 above
				result.addAll(allSynonyms.get(key).stream().map((x) -> String.format("\"%s\"", x.replace('_', ' ')))
						.collect(Collectors.toList()));
			}
		}

		return result;
	}

	private Map<String, List<String>> getSynonyms(List<String> words) throws IOException, InterruptedException {
		ArrayList<String> argsList = new ArrayList<String>();
		argsList.add("/usr/local/python3");
		argsList.add(this.synPyPath);
		argsList.addAll(words);

		String[] argsArr = new String[argsList.size()];
		argsArr = argsList.toArray(argsArr);

		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec(argsArr);
		pr.waitFor();

		Gson gson = new Gson();
		SynPyResult[] results;
		try (JsonReader synPyReader = new JsonReader(new InputStreamReader(pr.getInputStream()))) {
			results = gson.fromJson(synPyReader, SynPyResult[].class);
		}

		Map<String, List<String>> synonyms = new HashMap<String, List<String>>();

		for (SynPyResult synonym : results) {
			synonyms.put(synonym.word, synonym.synonyms);
		}

		return synonyms;
	}

	public void setAnalyzer() {
		CharArraySet stopWords = CharArraySet.copy(StopAnalyzer.ENGLISH_STOP_WORDS_SET);

		for (String stopWord : WH_STOP_WORDS) {
			stopWords.add(stopWord.toLowerCase());
		}
		this.analyzer = new EnglishAnalyzer(stopWords);
	}
}
