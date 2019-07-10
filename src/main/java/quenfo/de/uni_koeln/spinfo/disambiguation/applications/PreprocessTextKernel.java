package quenfo.de.uni_koeln.spinfo.disambiguation.applications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.atlas.logging.Log;

import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.FeatureUnitTokenizer;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_reduction.Normalizer;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_reduction.Stemmer;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_reduction.StopwordFilter;
import quenfo.de.uni_koeln.spinfo.classification.db_io.Class_DBConnector;
import quenfo.de.uni_koeln.spinfo.disambiguation.workflow.SentenceWriter;
import quenfo.de.uni_koeln.spinfo.disambiguation.workflow.WSDJobs;

public class PreprocessTextKernel {

	static String inputDB = "classification/db/text_kernel_cleaned.db";

	static String inputTable = "jobs_textkernel";

	static int fetchSize = 100;

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

		// Connect to input database
		Connection inputConnection = null;
		if (!new File(inputDB).exists()) {
			System.out
					.println("Database '" + inputDB + "' don't exists \nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = Class_DBConnector.connect(inputDB);
		}
		
		List<ClassifyUnit> jobAds = readDB(inputConnection);
		System.out.println(jobAds.size() + " JobAds gefunden");
		
		SentenceWriter.writeSentences(jobAds);
		System.out.println("Sentences written");
		WSDJobs jobs = new WSDJobs();
		jobAds = jobs.preprocessJobAds(jobAds);
		
		for(ClassifyUnit cu : jobAds) {
			System.out.println(Arrays.asList(cu.getFeatureVector()));
		}

	}

	private static List<ClassifyUnit> readDB(Connection inputDb) throws SQLException {
		String query = "SELECT ZEILENNR, Jahrgang, STELLENBESCHREIBUNG FROM " + inputTable + ";";

		PreparedStatement prepStmt = inputDb.prepareStatement(query);
		prepStmt.setFetchSize(fetchSize);
		// execute
		ResultSet queryResult = prepStmt.executeQuery();
		
		String countQuery = "SELECT COUNT(*) FROM " + inputTable + ";";
		Statement stmt = inputDb.createStatement();
		ResultSet countResult = stmt.executeQuery(countQuery);
		int tableSize = countResult.getInt(1);
		stmt.close();
		stmt = inputDb.createStatement();
		ResultSet rs = null;
		rs = stmt.executeQuery("SELECT COALESCE(" + tableSize + "+1, 0) FROM " + inputTable + ";");

		int queryLimit = rs.getInt(1);
		boolean goOn = true;
		
		String zeilenNr = "";
		String jahrgang = "";
		
		List<ClassifyUnit> toReturn = new ArrayList<>();
		
		
		while (queryResult.next() && goOn) {

			String jobAd = null;
			zeilenNr = queryResult.getString("ZEILENNR");
			jahrgang = queryResult.getString("Jahrgang");
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
			toReturn.add(new ClassifyUnit(jobAd));			
		}		
		
		return toReturn;
	}

	private static void preprocessJobAds(List<ClassifyUnit> jobAds) throws IOException {
		
		
		
		
	}

}
