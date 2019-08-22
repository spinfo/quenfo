package quenfo.de.uni_koeln.spinfo.classification.applicationsjb;

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

public class ClassifyTextkernel {
	
	// Pfad zur Input-DB
		static String inputDB = /* "D:/Daten/sqlite/SteA.db3"; */"classification/db/text_kernel_replaced_dev.db";

		// Jahrgang 
		static String jahrgang = "2011";
		
		//Name der Input-Tabelle
		static String inputTable = "jobs_textkernel";

		// Pfad zum Output-Ordner in dem die neue DB angelegt werden soll
		static String outputFolder = /* "D:/Daten/sqlite/classification/"; */ "C:/sqlite/classification/";

		// Name der korrigierbaren Output-DB (Input für alle späteren
		// IE-Applications )
		static String corrOutputDB = "CorrectableParagraphs_textkernel.db";

		// Name der (nicht korrigierbaren) Output-DB (dient nur zur Dokumentation
		// der originalen Klassifikationsergebnisse)
		static String origOutputDB = "OriginalParagraphs_textkernel.db";
		
		// Pfad zur Datei mit den Trainingsdaten
		static String trainingdataFile = "classification/data/trainingSets/trainingdata_anonymized.tsv";

		// Anzahl der Stellenanzeigen, die klassifiziert werden sollen (-1 = gesamte
		// Tabelle)
		static int queryLimit = 20;

		// falls nur eine begrenzte Anzahl von SteAs klassifiziert werden soll
		// (s.o.): hier die Startosition angeben
		static int startId = 0;

		// Die SteAs werden (aus Speichergründen) nicht alle auf einmal ausgelesen,
		// sondern Päckchenweise - hier angeben, wieviele jeweils in einem Schwung
		// zusammen verarbeitet werden
		// nach dem ersten Schwung erscheint in der Konsole ein Dialog, in dem man
		// das Programm nochmal stoppen (s), die nächsten xx SteAs klassifizieren
		// (c), oder ohne Unterbrechung zu Ende klassifizieren lassen kann (d)
		static int fetchSize = 100;
		
		
		static boolean normalize = true;
		
		static boolean stem = true;
		
		static boolean filterSW = true;
		
		static int[] ngrams = {3,4};
		
		static boolean continousNGrams = true;
		
		static int miScore = 0;
		
		static boolean suffixTree = false;
		
		



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

			// Connect to output database (and training database)
			Connection corrConnection = null;
			Connection origConnection = null;

			File corrDBFile = new File(outputFolder + corrOutputDB);
			File origDBFile = new File(outputFolder + origOutputDB);

			// if outputdatabase already exists
			if (corrDBFile.exists()) {
				corrConnection = Class_DBConnector.connect(corrDBFile.getPath());//(outputFolder + corrOutputDB);
				origConnection = Class_DBConnector.connect(origDBFile.getPath());//(outputFolder + origOutputDB);
			}

			// if output database does not exist
			else {
				// create output-directory if not exists
				if (!new File(outputFolder).exists()) {
					new File(outputFolder).mkdirs();
				}
				// create output-database
				corrConnection = Class_DBConnector.connect(outputFolder + corrOutputDB);
				Class_DBConnector.createClassificationOutputTables(corrConnection, true);
				origConnection = Class_DBConnector.connect(outputFolder + origOutputDB);
				Class_DBConnector.createClassificationOutputTables(origConnection, false);
			}

			// create output-directory if not exists
			if (!new File("classification/output").exists()) {
				new File("classification/output").mkdirs();
			}

			// start classifying				
			long before = System.currentTimeMillis();
			ConfigurableDatabaseClassifier dbClassify = new ConfigurableDatabaseClassifier(inputConnection, corrConnection,
					origConnection, queryLimit, fetchSize,

					startId, trainingdataFile);
			try {
				FeatureUnitConfiguration  fuc = new FeatureUnitConfiguration(normalize, stem, filterSW, ngrams, continousNGrams, miScore, suffixTree);
				AbstractFeatureQuantifier fq = new LogLikeliHoodFeatureQuantifier();
				AbstractClassifier classifier = new ZoneKNNClassifier(false, 5, Distance.COSINUS);
				ExperimentConfiguration config = new ExperimentConfiguration(fuc, fq, classifier, new File(trainingdataFile), outputFolder);
				
				
				dbClassify.classifyWithConfig(config, inputTable);
			} catch (Exception e) {
				e.printStackTrace();
			}

			long after = System.currentTimeMillis();
			double time = (((double)after - before)/1000)/60;
			if (time > 60) {
				System.out.println("\nfinished Classification in " + (time / 60) + "hours");
			} else {
				System.out.println("\nfinished Classification in " + time + " minutes");
			}
		}

}
