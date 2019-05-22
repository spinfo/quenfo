package quenfo.lucene;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.junit.Test;

import quenfo.de.uni_koeln.spinfo.classification.db_io.Class_DBConnector;

public class BuildIndex {

	private String indexDirPath = "data/index";

	@Test
	public void test() throws IOException {

		Directory dir = new SimpleFSDirectory(new File(indexDirPath).toPath());
		File folder = new File(indexDirPath);
		folder.mkdirs();

		IndexWriterConfig writerConfig = new IndexWriterConfig(new StandardAnalyzer());
		IndexWriter writer = new IndexWriter(dir, writerConfig);

		writer.deleteAll();

		Map<String, String> jobAds = null;
		try {
			jobAds = readJobAds();
		} catch (ClassNotFoundException e) {
			
		} catch (SQLException e) {
			
		}
		System.out.println(jobAds.size());
		List<Document> docs = convertToLuceneDoc(jobAds);
		System.out.println(docs.size());
		for (Document doc : docs) {

			writer.addDocument(doc);
		}

		writer.close();

	}

	private Map<String, String> readJobAds() throws ClassNotFoundException, SQLException {
		String inputDB = "classification/db/JobAds.db";
		String tableName = "DL_ALL_Spinfo_2011";
		int queryLimit = -1;
		int currentID = 0;
		int fetchSize = 100;
		
		Connection inputConnection = null;
		if (!new File(inputDB).exists()) {
			System.out
					.println("Database '" + inputDB + "' don't exists \nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = Class_DBConnector.connect(inputDB);
		}
		
		// get data from db
				int done = 0;
				String query = null;
				int zeilenNr = 0, jahrgang = 0;
				;
				int jobAdCount = 0;
				int paraCount = 0;
				query = "SELECT ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM " + tableName + " LIMIT ? OFFSET ?;";

				PreparedStatement prepStmt = inputConnection.prepareStatement(query);
				prepStmt.setInt(1, queryLimit);
				prepStmt.setInt(2, currentID);
				prepStmt.setFetchSize(fetchSize);
				// execute
				ResultSet queryResult = prepStmt.executeQuery();

				// total entries to process:
				if (queryLimit < 0) {

					String countQuery = "SELECT COUNT(*) FROM " + tableName + ";";
					Statement stmt = inputConnection.createStatement();
					ResultSet countResult = stmt.executeQuery(countQuery);
					int tableSize = countResult.getInt(1);
					stmt.close();
					stmt = inputConnection.createStatement();
					ResultSet rs = null;
					rs = stmt.executeQuery("SELECT COALESCE(" + tableSize + "+1, 0) FROM " + tableName + ";");

					queryLimit = rs.getInt(1);
				}

				boolean goOn = true;
				boolean askAgain = true;

				Map<String, String> toReturn = new HashMap<String, String>();
				while (queryResult.next() && goOn) {
					jobAdCount++;
					String jobAd = null;
					zeilenNr = queryResult.getInt("ZEILENNR");
					jahrgang = queryResult.getInt("Jahrgang");
					jobAd = queryResult.getString("STELLENBESCHREIBUNG");
					// if there is an empty job description, classifying is of no use,
					// so skip
					if (jobAd == null) {
						System.out.println("________________________________________________________________");
						System.out.println("JobAd ist null");
						System.out.println("Zeilennummer: " + zeilenNr);
						System.out.println("Jahrgang: " + jahrgang);
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss ");
						Date currentTime = new Date();
						System.out.println("Zeit und Datum : " + formatter.format(currentTime));
						System.out.println("_________________________________________________________________");
						continue;
					}
					if (jobAd.isEmpty()) {
						System.out.println("__________________________________________________________________");
						System.out.println(" JobAd ist leer!");
						System.out.println("Zeilennummer: " + zeilenNr);
						System.out.println("Jahrgang: " + jahrgang);
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss ");
						Date currentTime = new Date();
						System.out.println("Zeit und Datum : " + formatter.format(currentTime));
						System.out.println("___________________________________________________________________");
						continue;
					}
					
					toReturn.put("SteA"+zeilenNr, jobAd);
				}
		
		
		
		return toReturn;
	}

	private List<Document> convertToLuceneDoc(Map<String, String> jobAds) {

		List<Document> luceneDocLists = new ArrayList<Document>();
		for (Map.Entry<String, String> e : jobAds.entrySet()) {
			Document doc = new Document();
			doc.add(new TextField("content", e.getValue(), Field.Store.YES));
			doc.add(new TextField("filename", e.getKey(), Field.Store.YES));
			luceneDocLists.add(doc);
		}
		return luceneDocLists;

	}

}
