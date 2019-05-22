package quenfo.lucene;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Test;

public class SearchIndex {

	private String indexDirPath = "data/index";

	@Test
	public void test() throws IOException, ParseException {

		Directory dir = FSDirectory.open(Paths.get(indexDirPath));
		IndexReader reader = DirectoryReader.open(dir);
		IndexSearcher searcher = new IndexSearcher(reader);
		
		
		queryIndex("Bedienung", searcher, reader.numDocs());
		

	}

	private void queryIndex(String queryString, IndexSearcher searcher, int limit) throws IOException, ParseException {
		QueryParser qp = new QueryParser("content", new StandardAnalyzer());
		Query idQuery = qp.parse(queryString);
		TopDocs hits = searcher.search(idQuery, limit);
		System.out.println(hits.totalHits);
		for (ScoreDoc sd : hits.scoreDocs) {
			Document d = searcher.doc(sd.doc);
			System.out.println(String.format(d.get("content")));
		}
	}

}
