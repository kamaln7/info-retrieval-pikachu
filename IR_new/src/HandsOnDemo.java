import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

public class HandsOnDemo {
	private static final String BODY_FIELD = "body";
	private static final String[] WH_STOP_WORDS = { "how", "what", "where", "who", "when", "which", "whom", "whose",
			"why", "do" };
	private static final String path = "../scripts/corpus_answers.txt";
	private static final String nfL6path = "../scripts/nfL6.json";
	private static final FieldType TERM_VECTOR_TYPE;
	static {
		TERM_VECTOR_TYPE = new FieldType(TextField.TYPE_STORED);
		TERM_VECTOR_TYPE.setStoreTermVectors(true);
		TERM_VECTOR_TYPE.setStoreTermVectorPositions(true);
		TERM_VECTOR_TYPE.setStoreTermVectorOffsets(true);
		TERM_VECTOR_TYPE.freeze();
	}

	private static Path cacheDirPath = new File("./cache/index").toPath();

	private static IndexWriterConfig newIndexWriterConfig(Analyzer analyzer) {
		return new IndexWriterConfig(analyzer).setOpenMode(OpenMode.CREATE).setCodec(new SimpleTextCodec())
				.setCommitOnClose(true);
	}

	public static void buildIndex(Directory dir, Analyzer analyzer) throws IOException {
		// parse json
		Gson gson = new Gson();
		nfL6Entry[] entries;
		try (JsonReader nfL6Reader = new JsonReader(new FileReader(nfL6path))) {
			entries = gson.fromJson(nfL6Reader, nfL6Entry[].class);
		}

		// build index
		try (IndexWriter writer = new IndexWriter(dir, newIndexWriterConfig(analyzer))) {
			for (nfL6Entry entry : entries) {
				ArrayList<String> answers = new ArrayList<String>();
				answers.addAll(entry.NBestAnswers);

				for (String answer : answers) {
					Document doc = new Document();

					// THIS IS NOT INDEXED!
					// StoredField: Stored-only value for retrieving in summary results
					// we only store the question ID in case we need to use it later
					doc.add(new StoredField("questionID", entry.ID));

					doc.add(new TextField("body", answer, Store.YES));
					writer.addDocument(doc);
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		try {
			Directory dir = FSDirectory.open(cacheDirPath);
			Analyzer analyzer = newAnalyzer();
			// Index
			if (Files.notExists(cacheDirPath)) {
				buildIndex(dir, analyzer);
			}

			// Search
			try (DirectoryReader reader = DirectoryReader.open(dir)) {
				final QueryParser qp = new QueryParser(BODY_FIELD, analyzer);

				final List<String> query_words = new LinkedList<String>();

				String query = "For colIege admission, is it better to take AP classes and get Bs or easy classes and get As?";

				if (query.charAt(query.length() - 1) == '?') {
					query = query.substring(0, query.length() - 1);
				}

				String[] query_words_array = query.split("\\s+");

				for (int i = 0; i < query_words_array.length; i++) {
					query_words.add(query_words_array[i]);
				}

				for (int i = 0; i < query_words.size(); i++) {
					if (query_words.get(i).toLowerCase().equals("why")) {
						query_words.add(i, "because");
						i++;
						query_words.add(i, "coz");
						i++;
						query_words.add(i, "cuz");
						i++;
					}
					if (query_words.get(i).toLowerCase().equals("when")) {
						query_words.add(i, "at");
						i++;
					}
					if (query_words.get(i).toLowerCase().equals("which")) {
						query_words.add(i, "one");
						i++;
					}
				}

				for (int i = 0; i < query_words.size(); i++) {
					for (int j = 0; j < WH_STOP_WORDS.length; j++) {
						if (query_words.get(i).toLowerCase().equals(WH_STOP_WORDS[j])) {
							query_words.remove(i);
						}
					}
				}

				StringBuilder tmp_new_query = new StringBuilder();

				for (int i = 0; i < query_words.size(); i++) {
					Runtime rt = Runtime.getRuntime();
					Process pr = rt.exec("/usr/local/python3 ../scripts/syn.py " + query_words.get(i).toLowerCase());
					pr.waitFor();

					BufferedReader bfr = new BufferedReader(new InputStreamReader(pr.getInputStream()));
					String line = "";
					String word_add = "";

					while ((line = bfr.readLine()) != null) {
						System.out.println(line);
						word_add = line;
					}

					tmp_new_query.append("body: " + query_words.get(i) + " ");
					if (word_add != "") {
						StringBuilder add_query = new StringBuilder();
						add_query.append(" body:" + word_add);
						tmp_new_query.append("body: " + word_add + " ");
					}
				}
				String new_query = tmp_new_query.toString();

				if (new_query.charAt(new_query.length() - 1) == ' ') {
					new_query = new_query.substring(0, new_query.length() - 1);
				}

				final Query q = qp.parse(new_query);
				System.out.println("Query: " + q);
				System.out.println();

				final IndexSearcher searcher = new IndexSearcher(reader);
				// searcher.setSimilarity(new ClassicSimilarity());
				searcher.setSimilarity(new BM25Similarity());
				final TopDocs td = searcher.search(q, 300);

				ArrayList<answer> answer_list = new ArrayList<answer>();
				answers big_answer = new answers();

				final FastVectorHighlighter highlighter = new FastVectorHighlighter();
				final FieldQuery fieldQuery = highlighter.getFieldQuery(q, reader);

				int count = 0;
				int count_300 = 0;

				PrintStream out = new PrintStream(new FileOutputStream("../scripts/top_300_answers.txt"));

				for (final ScoreDoc sd : td.scoreDocs) {
					answer answer1 = new answer();

					final String[] snippets = highlighter.getBestFragments(fieldQuery, reader, sd.doc, BODY_FIELD, 100,
							3);
					final Document doc = searcher.doc(sd.doc);

					answer1.setAnswer(doc.get(BODY_FIELD));
					answer1.setScore(sd.score);

					if (count == 4) {
						answer_list.add(answer1);
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

					answer_list.add(answer1);
					count++;

					if (count_300 < 300) {
						out.println(doc.get(BODY_FIELD));
						count_300++;

						if (count_300 < 50) {
							System.out.println(String.format("doc=%d, score=%.4f, text=%s snippet=%s", sd.doc, sd.score,
									doc.get(BODY_FIELD), Arrays.stream(snippets).collect(Collectors.joining(" "))));
						}
					}
				} // end of top-docs 3

				// String[] most_freq_words = countMostFreqWord();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected TokenStreamComponents createComponents(String string) {
		throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
																		// Tools | Templates.
	}

	private static Analyzer newAnalyzer() {
		return new EnglishAnalyzer();
	}
}
