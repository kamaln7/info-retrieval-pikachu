/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required byOCP applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.PerFieldSimilarityWrapper;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.google.common.collect.Iterables;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class HandsOnDemo {
    private static final String BODY_FIELD = "body";

    private static final FieldType TERM_VECTOR_TYPE;
    static {
        TERM_VECTOR_TYPE = new FieldType(TextField.TYPE_STORED);
        TERM_VECTOR_TYPE.setStoreTermVectors(true);
        TERM_VECTOR_TYPE.setStoreTermVectorPositions(true);
        TERM_VECTOR_TYPE.setStoreTermVectorOffsets(true);
        TERM_VECTOR_TYPE.freeze();
    }

    public static void main(String[] args) throws Exception {
    	
    		String path = "/Users/emiljashshan/git/IR/src/nfL6.json";
        BufferedReader bufferedReader = new BufferedReader(new FileReader(path));

        Gson gson = new Gson();
        
        //parse Json file
        Data[] data = gson.fromJson(bufferedReader, Data[].class);
        
        try (Directory dir = newDirectory();
        Analyzer analyzer = newAnalyzer()) 
        {
            // Index
            try (IndexWriter writer = new IndexWriter(dir, newIndexWriterConfig(analyzer))) 
            {
                for (int i = 0; i < data.length; i++)
                {
	                	for (String answer : data[i].getNbestanswers()) 
	                	{
	                		final Document doc = new Document();
                        doc.add(new TextField("body", answer, Store.YES));
                        writer.addDocument(doc);
	            		}
                }
            }

            // Search
            try (DirectoryReader reader = DirectoryReader.open(dir)) 
            {
                final QueryParser qp = new QueryParser(BODY_FIELD, analyzer);
                
                String query = "How can I behave in a more spontaneous way?";
                
                if(query.charAt(query.length()-1) == '?') 
                {
                		query = query.substring(0, query.length() - 1);
                }
                
                String[] words = query.split("\\s+");
                StringBuilder tmp_new_query = new StringBuilder();
                
                for(int i=0 ; i< words.length ; i++)
                {
                		Runtime rt = Runtime.getRuntime();
                		Process pr = rt.exec("/usr/local/python3 /Users/emiljashshan/Documents/main/Current_Courses/Information_recovery/python/syn.py " + words[i]);
                		pr.waitFor();
                		
                		BufferedReader bfr = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                		String line = "";
                		String word_add = "";
                        
                		while((line = bfr.readLine()) != null) 
                    {
                			System.out.println(line);
                        	word_add = line;
                    	}
                		
                		tmp_new_query.append("body: " + words[i] + " ");
                		if(word_add != "")
                		{
                			StringBuilder add_query = new StringBuilder();
                			//add_query.append(q);
                			add_query.append(" body:" + word_add);
                			tmp_new_query.append("body: " + word_add + " ");
                		}
                }
                String new_query = tmp_new_query.toString();
                
                if(new_query.charAt(new_query.length()-1) == ' ') 
                {
                		new_query = new_query.substring(0, new_query.length() - 1);
                }
                
                final Query q = qp.parse(new_query);
                System.out.println("Query: " + q);
                System.out.println();  
                
                final IndexSearcher searcher = new IndexSearcher(reader);
                //searcher.setSimilarity(new ClassicSimilarity());
                searcher.setSimilarity(new BM25Similarity());
                final TopDocs td = searcher.search(q, 25);

                ArrayList<answer> answer_list = new ArrayList<answer>();
                answers big_answer = new answers();
                
                final FastVectorHighlighter highlighter = new FastVectorHighlighter();
                final FieldQuery fieldQuery = highlighter.getFieldQuery(q, reader);
                
                int count=0;
                for (final ScoreDoc sd : td.scoreDocs) 
                {
                		answer answer1 = new answer();
                		
                    final String[] snippets = highlighter.getBestFragments(fieldQuery, reader, sd.doc, BODY_FIELD, 100, 3);
                    final Document doc = searcher.doc(sd.doc);
                    
                    answer1.setAnswer(doc.get(BODY_FIELD));
                    answer1.setScore(sd.score);
                    
                    if(count == 4)
                    {
                    		answer_list.add(answer1);
                    		big_answer.setAnswers(answer_list);
                    		gson = new GsonBuilder().setPrettyPrinting().create();
                    		String strJson = gson.toJson(big_answer);
                    		System.out.println(String.format("doc=%d, score=%.4f, text=%s snippet=%s", sd.doc, sd.score,
                                    doc.get(BODY_FIELD), Arrays.stream(snippets).collect(Collectors.joining(" "))));
                    		break;
                    }
                    
                    answer_list.add(answer1);
                    count++;
                    System.out.println(String.format("doc=%d, score=%.4f, text=%s snippet=%s", sd.doc, sd.score,
                            doc.get(BODY_FIELD), Arrays.stream(snippets).collect(Collectors.joining(" "))));
                }
            }
        }
    }

    private static Directory newDirectory() throws IOException {
        // return new RAMDirectory();
        return FSDirectory.open(new File("d:/tmp/ir-class/demo").toPath());
    }
    
    protected TokenStreamComponents createComponents(String string) 
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static Analyzer newAnalyzer() 
    {
        return new EnglishAnalyzer();
    }

    private static IndexWriterConfig newIndexWriterConfig(Analyzer analyzer) 
    {
        return new IndexWriterConfig(analyzer)
                .setOpenMode(OpenMode.CREATE)
                .setCodec(new SimpleTextCodec())
                // .setUseCompoundFile(false)
                .setCommitOnClose(true);
    }
}
