import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
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
	private CharArraySet stopWords;

	public Pikachu(String nfL6path, Path cacheDirectory, String synPyPath) throws IOException {
		this.nfL6path = nfL6path;
		this.cacheDirPath = cacheDirectory;
		this.synPyPath = synPyPath;

		System.out.println("Starting lucene");
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
		System.out.println("building index");
		// parse json
		Gson gson = new Gson();
		nfL6Entry[] entries;
		System.out.println("reading nfL6.json");
		try (JsonReader nfL6Reader = new JsonReader(new FileReader(nfL6path))) {
			entries = gson.fromJson(nfL6Reader, nfL6Entry[].class);
		}

		// build index
		System.out.println("writing index");
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

	public List<Answer> Search(String originalQuery) throws Exception {
		String query = originalQuery;
		System.out.printf("Original Query: %s\n", query);
		List<String> query_words = new LinkedList<String>();

		// keep only alphanumeric chars
		query = this.cleanQuery(query);

		for (String word : query.split("\\s+")) {
			query_words.add(word);

			String synKey = word.toLowerCase();
			// TODO: fix this
			if (false && hardcodedSynonyms.containsKey(synKey)) {
				for (String synonym : hardcodedSynonyms.get(synKey)) {
					query_words.add(synonym);
				}
			}
		}

		query_words = addSynonyms(query_words);

		query = query_words.stream().collect(Collectors.joining(" "));

		System.out.printf("Modified Query: %s\n", query);

		// get top 300 answers
		System.out.println("finding top 300 answers");
		TopDocs top300 = runQuery(query, 300);

		// extract top 5 words from the top 300 answers
		System.out.println("counting words");
		Map<String, Integer> wordCount = new HashMap<String, Integer>();
		for (ScoreDoc sd : top300.scoreDocs) {
			Document doc = searcher.doc(sd.doc);
			String answer = this.cleanQuery(doc.get(BODY_FIELD));

			try (TokenStream ts = this.analyzer.tokenStream(null, answer)) {
				ts.reset();
				while (ts.incrementToken()) {
					String key = ts.getAttribute(CharTermAttribute.class).toString().toLowerCase();

					Integer count = wordCount.getOrDefault(key, 0);
					count++;
					wordCount.put(key, count);
				}
			}
		}

		System.out.println("finding top 8 words");
		List<String> top5words = wordCount.entrySet().stream().sorted((e1, e2) -> e2.getValue() - e1.getValue())
				.limit(8).map((e) -> e.getKey()).collect(Collectors.toList());

		System.out.println("adding words to the query");
		query = String.format("%s %s", query,
				top5words.stream().map((x) -> String.format("\"%s\"^2", x)).collect(Collectors.joining(" ")));

		System.out.println("getting top 5 answers");
		TopDocs top5 = this.runQuery(query, 5);
		List<Answer> answers = this.formatToAnswers(top5);
		answers.stream().limit(5).forEach((a) -> {
			System.out.printf("doc=%d, score=%.4f, text=%s\n", a.doc, a.score, a.answer);
		});

		return answers;
	}

	public String cleanQuery(String query) {
		// return query.replaceAll("[\\?,\\.!\"\\^]$", "");
		return query.replaceAll("[^A-Za-z0-9\\s]", "");
	}

	TopDocs runQuery(String query, Integer count) throws ParseException, IOException, FileNotFoundException {
		Query q = this.qp.parse(query);
		System.out.printf("Parsed Query: %s\n", q);

		TopDocs td = searcher.search(q, count);
		return td;
	}

	List<Answer> formatToAnswers(TopDocs docs) throws IOException {
		ArrayList<Answer> answers = new ArrayList<Answer>();

		for (ScoreDoc sd : docs.scoreDocs) {
			Document doc = searcher.doc(sd.doc);

			Answer answer = new Answer();
			answer.answer = doc.get(BODY_FIELD);
			answer.score = sd.score;
			answer.doc = sd.doc;
			answers.add(answer);
		}

		return answers;
	}

	private List<String> addSynonyms(List<String> query_words) throws IOException, InterruptedException {
		ArrayList<String> result = new ArrayList<String>();

		Map<String, List<String>> allSynonyms = getSynonyms(query_words);

		for (String word : query_words) {
			result.add(String.format("%s^3", word));

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

		this.stopWords = stopWords;
		this.analyzer = new EnglishAnalyzer(stopWords);
	}
}
