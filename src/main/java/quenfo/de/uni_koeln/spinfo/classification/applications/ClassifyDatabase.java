package quenfo.de.uni_koeln.spinfo.classification.applications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import quenfo.de.uni_koeln.spinfo.classification.core.classifier.AbstractClassifier;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import quenfo.de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import quenfo.de.uni_koeln.spinfo.classification.core.distance.Distance;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_weighting.AbstractFeatureQuantifier;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_weighting.LogLikeliHoodFeatureQuantifier;
import quenfo.de.uni_koeln.spinfo.classification.db_io.Class_DBConnector;
import quenfo.de.uni_koeln.spinfo.classification.jasc.workflow.ConfigurableDatabaseClassifier;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.ZoneKNNClassifier;
import quenfo.de.uni_koeln.spinfo.core.helpers.PropertiesHandler;

/**
 * 
 * Workflow zur Segmentierung von Stellenanzeigen in Paragraphen und
 *  Klassifikation der Paragraphen in die vier Kategorien:<br>
 * 
 * - Unternehmensbeschreibung - Jobbeschreibung - Bewerberprofil - Sonstiges/Formalia<br><br>
 * 
 *  Die Klassifikation kann in den config-Files general.properties und classification.properties konfiguriert werden.<br><br>
 *  
 *  Es werden zunächst [fetchSize] Anzeigen bearbeitet. Danach kann im Konsolen-Dialog ausgewählt werden, ob man
 *  das Programm stoppen (s), die nächsten [fetchSize] Stellenanzeigen verarbeiten (c), oder ohne Unterbrechung zu Ende klassifizieren lässt (d).
 * 
 * 
 * 
 * 
 * @author Johanna Binnewitt
 *
 */
public class ClassifyDatabase {

	// Pfad zur Input-DB
	static String inputDB = null; 

	// Name der Input-Tabelle
	static String inputTable = null;

	// Pfad zum Output-Ordner in dem die neue DB angelegt werden soll
	static String outputFolder = null;

	// Name der Output-DB
	static String outputDB = null;

	// Pfad zur Datei mit den Trainingsdaten
	static String trainingdataFile = null;

	// Anzahl der Stellenanzeigen, die klassifiziert werden sollen (-1 = gesamte
	// Tabelle)
	static int queryLimit;

	// falls nur eine begrenzte Anzahl von SteAs klassifiziert werden soll
	// (s.o.): hier die Startosition angeben
	static int startPos;

	// Die SteAs werden (aus Speichergründen) nicht alle auf einmal ausgelesen,
	// sondern Päckchenweise - hier angeben, wieviele jeweils in einem Schwung
	// zusammen verarbeitet werden
	// nach dem ersten Schwung erscheint in der Konsole ein Dialog, in dem man
	// das Programm nochmal stoppen (s), die nächsten xx SteAs klassifizieren
	// (c), oder ohne Unterbrechung zu Ende klassifizieren lassen kann (d)
	static int fetchSize;

	static boolean normalize = false;

	static boolean stem = false;

	static boolean filterSW = false;

	static int[] nGrams = null;

	static boolean continousNGrams = false;

	static int miScore = 0;

	static boolean suffixTree = false;

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

		if (args.length > 0) {
			String configFolder = args[1];
			loadProperties(configFolder);
		}

		// Connect to input database
		Connection inputConnection = null;
		if (!new File(inputDB).exists()) {
			System.out
					.println("Database '" + inputDB + "' doesn't exist \nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = Class_DBConnector.connect(inputDB);
		}

		Connection outputConnection = null;

		File outputDBFile = new File(outputFolder + outputDB);

		// if outputdatabase already exists
		if (outputDBFile.exists()) {
			outputConnection = Class_DBConnector.connect(outputDBFile.getPath());// (outputFolder + origOutputDB);
		}

		// if output database does not exist
		else {
			// create output-directory if not exists
			if (!new File(outputFolder).exists()) {
				new File(outputFolder).mkdirs();
			}
			outputConnection = Class_DBConnector.connect(outputDBFile.getPath());
			Class_DBConnector.createClassificationOutputTables(outputConnection/* , false */);
		}


		// start classifying
		long before = System.currentTimeMillis();
		ConfigurableDatabaseClassifier dbClassify = new ConfigurableDatabaseClassifier(inputConnection, 
				outputConnection, queryLimit, fetchSize,
				startPos, trainingdataFile);
		try {
			FeatureUnitConfiguration fuc = new FeatureUnitConfiguration(normalize, stem, filterSW, nGrams,
					continousNGrams, miScore, suffixTree);
			AbstractFeatureQuantifier fq = new LogLikeliHoodFeatureQuantifier();
			AbstractClassifier classifier = new ZoneKNNClassifier(false, 5, Distance.COSINUS);
			ExperimentConfiguration config = new ExperimentConfiguration(fuc, fq, classifier,
					new File(trainingdataFile), outputFolder);

			dbClassify.classifyWithConfig(config, inputTable);
		} catch (Exception e) {
			e.printStackTrace();
		}

		long after = System.currentTimeMillis();
		double time = (((double) after - before) / 1000) / 60;
		if (time > 60) {
			System.out.println("\nfinished Classification in " + (time / 60) + "hours");
		} else {
			System.out.println("\nfinished Classification in " + time + " minutes");
		}
		System.out.println("wrote classified paragraphs to: " + outputDBFile.getAbsolutePath());
	}

	private static void loadProperties(String folderPath) throws IOException {

		File configFolder = new File(folderPath);

		if (!configFolder.exists()) {
			System.err.println("Config Folder " + folderPath + " does not exist."
					+ "\nPlease change configuration and start again.");
			System.exit(0);
		}
		
		String quenfoData = configFolder.getParent();
		PropertiesHandler.initialize(configFolder);
		
		inputDB = quenfoData + "/sqlite/jobads/" + PropertiesHandler.getStringProperty("general", "jobAdsDB");
		inputTable = PropertiesHandler.getStringProperty("general", "jobAds_inputTable");
		outputFolder = quenfoData + "/sqlite/classification/";
		outputDB = PropertiesHandler.getStringProperty("general", "classifiedParagraphs");
		
		trainingdataFile = quenfoData + "/resources/classification/trainingSets/" + PropertiesHandler.getStringProperty("classification", "trainingDataFile");
		
		startPos = PropertiesHandler.getIntProperty("classification", "startPos");
		queryLimit = PropertiesHandler.getIntProperty("classification", "queryLimit");
		fetchSize = PropertiesHandler.getIntProperty("classification", "fetchSize");

		normalize = PropertiesHandler.getBoolProperty("classification", "normalize");
		stem = PropertiesHandler.getBoolProperty("classification", "stem");
		filterSW = PropertiesHandler.getBoolProperty("classification", "filterSW");
		nGrams = PropertiesHandler.getIntArrayProperty("classification", "nGrams");
		continousNGrams = PropertiesHandler.getBoolProperty("classification", "continousNGrams");

	}

}
