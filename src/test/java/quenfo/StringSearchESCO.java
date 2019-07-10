package quenfo;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import com.opencsv.CSVReader;

import is2.lemmatizer.Lemmatizer;
import is2.tag.Tagger;
import is2.tools.Tool;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.FeatureUnitTokenizer;
import quenfo.de.uni_koeln.spinfo.classification.db_io.Class_DBConnector;
import quenfo.de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.preprocessing.TrainingDataGenerator;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.preprocessing.ExtractionUnitBuilder;

/**
 * sucht die ESCO Skills (Token & Lemma) in den Stellenausschreibungen (JobAds.db und klassifizierte Paragraphen)
 * @author Johanna Binnewitt
 *
 */
public class StringSearchESCO {

	private File escoCSV = new File("information_extraction/data/competences/esco/skills_de.csv");
	
	private Map<String, Integer> tokenDocFreqs = new HashMap<String, Integer>();
	private Map<String, Integer> lemmaDocFreqs = new HashMap<String, Integer>();

	@Test
	public void test() throws IOException {
		List<ExtractionUnit> jobAds = null;
		try {
			jobAds = readJobAds();
			
		} catch (ClassNotFoundException e) {

		} catch (SQLException e) {

		}
		System.out.println(jobAds.size());

		preprocess(jobAds);
		System.out.println(tokenDocFreqs.size() + " Tokens");
		System.out.println(lemmaDocFreqs.size() + " Lemmas");

		List<String> esco = readESCO();
		for (String e : esco) {
			e = e.toLowerCase();
			if (tokenDocFreqs.containsKey(e))
				System.out.println(e + ": in " + tokenDocFreqs.get(e) + " Stellenausschreibungen");
			
			if(lemmaDocFreqs.containsKey(e))
				System.out.println(e + ": in " + lemmaDocFreqs.get(e) + " Stellenausschreibungen");

		}

	}


	private void preprocess(List<ExtractionUnit> jobAds) {

		FeatureUnitTokenizer tokenizer = new FeatureUnitTokenizer();
		for (ExtractionUnit eu : jobAds) {
			List<String> tokens = tokenizer.tokenize(eu.getSentence());
			for (String t : tokens) {
				t = t.toLowerCase();
				int freq = 0;
				if (tokenDocFreqs.containsKey(t))
					freq = tokenDocFreqs.get(t);
				tokenDocFreqs.put(t, freq + 1);
			}
			
			String[] lemmas = eu.getLemmata();
			for (String t : lemmas) {
				t = t.toLowerCase();
				int freq = 0;
				if (lemmaDocFreqs.containsKey(t))
					freq = lemmaDocFreqs.get(t);
				lemmaDocFreqs.put(t, freq + 1);
			}			
			
		}

	}

	private List<String> readESCO() throws IOException {
		CSVReader reader = new CSVReader(new FileReader(escoCSV));

		String[] line = null;

		// skills und synonyme extrahieren
		List<String> esco = new ArrayList<String>();
		while ((line = reader.readNext()) != null) {
			String preffered = line[4];
			String[] syns = line[5].split("\\n");
			if (!esco.add(preffered))
				System.out.println(preffered);
			if (syns != null)
				for (int i = 0; i < syns.length; i++) {
					String syn = syns[i].toLowerCase();
					if (syn.isEmpty())
						continue;
					if (!esco.add(syn))
						System.out.println(syn);
				}

		}

		reader.close();

		return esco;
	}
	


	private List<ExtractionUnit> readJobAds() throws ClassNotFoundException, SQLException, IOException {
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

		String query = null;
		int zeilenNr = 0, parentID;
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

		List<ClassifyUnit> classifyUnits = new ArrayList<ClassifyUnit>();
		while (queryResult.next()) {
			String jobAd = null;
			zeilenNr = queryResult.getInt("ZEILENNR");
			parentID = queryResult.getInt("Jahrgang");
			jobAd = queryResult.getString("STELLENBESCHREIBUNG");
			// if there is an empty job description, classifying is of no use,
			// so skip
			if (jobAd == null) {

				continue;
			}
			if (jobAd.isEmpty()) {

				continue;
			}
			ClassifyUnit classifyUnit = new JASCClassifyUnit(jobAd, parentID, zeilenNr);
			classifyUnits.add(classifyUnit);
		}
			
		TrainingDataGenerator tdg = new TrainingDataGenerator(new File("classification/data/trainingSets/trainingdata_anonymized.tsv"),6,4,null);
		classifyUnits.addAll(tdg.getTrainingData());
		
		Tool lemmatizer = new Lemmatizer(
				"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model", false);
		Tool tagger = new Tagger(
				"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/tag-ger-3.6.model");

		List<ExtractionUnit> extractionUnits = ExtractionUnitBuilder.initializeIEUnits(classifyUnits, lemmatizer, null, tagger);
		

		return extractionUnits;
	}

}
