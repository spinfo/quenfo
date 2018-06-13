package quenfo.de.uni_koeln.spinfo.classification.applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;

import quenfo.de.uni_koeln.spinfo.classification.db_io.Class_DBConnector;
import quenfo.de.uni_koeln.spinfo.classification.jasc.workflow.ConfigurableDatabaseClassifier;

/**
 * 
 * @author avogt, geduldia
 * 
 *         Workflow zur Segmentierung von Stellenanzeigen in Paragraphen und
 *         Klassifikation der Paragraphen in die vier Kategorien:
 * 
 *         - Unternehmensbeschreibung - Jobbeschreibung - Bewerberprofil -
 *         Sonstiges/Formalia
 * 
 *         Im Konsolen-Dialog kann ein bereits gespeichertes
 *         Klassifikationsmodel ausgewählt werden, oder ein neues erzeugt
 *         werden. Es werden zunächst nur die ersten [fetchSize] Stellenanzeigen
 *         verarbeitet. Danach kann im Konsolen-Dialog ausgewählt werden, ob man
 *         das Programm nochml stoppen (s), die nächsten [fetchSize] Stellenanzeigen verarbeiten, oder ohne Unterbrechung zu Ende klassifizieren lässt..
 *
 */
public class ClassifyDatabase {

	// Pfad zur Input-DB
	static String inputDB = /* "D:/Daten/sqlite/SteA.db3"; */"classification/db/JobAds.db";

	// Jahrgang 
	static String jahrgang = "2011";
	
	//Name der Input-Tabelle
	static String inputTable = "DL_ALL_Spinfo_"+jahrgang;

	// Pfad zum Output-Ordner in dem die neue DB angelegt werden soll
	static String outputFolder = /* "D:/Daten/sqlite/classification/"; */ "C:/sqlite/classification/";

	// Name der korrigierbaren Output-DB (Input für alle späteren
	// IE-Applications )
	static String corrOutputDB = "CorrectableParagraphs_" + jahrgang + ".db";

	// Name der (nicht korrigierbaren) Output-DB (dient nur zur Dokumentation
	// der originalen Klassifikationsergebnisse)
	static String origOutputDB = "OriginalParagraphs_" + jahrgang + ".db";
	
	// Pfad zur Datei mit den Trainingsdaten
	static String trainingdataFile = "classification/data/trainingSets/trainingDataBIBBandSpinfo.csv";

	// Anzahl der Stellenanzeigen, die klassifiziert werden sollen (-1 = gesamte
	// Tabelle)
	static int queryLimit = -1;

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
		Connection trainingConnection = null;
		File corrDBFile = new File(outputFolder + corrOutputDB);
		File origDBFile = new File(outputFolder + origOutputDB);

		// if outputdatabase already exists
		if (corrDBFile.exists()) {
			corrConnection = Class_DBConnector.connect(outputFolder + corrOutputDB);

			// use or override current outputDatabase
			System.out.println("\noutput-database  already exists. "
					+ "\n - press 'o' to overwrite it (deletes all prior entries - annotated Trainingsparagraphs are saved in Trainingdatabase)"
					+ "\n - press 'u' to use it (adds and replaces entries)"
					+ "\n - press 'c' to create a new Output-Database");
			boolean answered = false;
			while (!answered) {
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				String answer = in.readLine();
				if (answer.toLowerCase().trim().equals("o")) {
					corrConnection = Class_DBConnector.connect(outputFolder + corrOutputDB);
					Class_DBConnector.createClassificationOutputTables(corrConnection, true);
					origConnection = Class_DBConnector.connect(outputFolder + origOutputDB);
					Class_DBConnector.createClassificationOutputTables(origConnection, false);
					answered = true;
				} else if (answer.toLowerCase().trim().equals("u")) {
					corrConnection = Class_DBConnector.connect(outputFolder + corrOutputDB);
					origConnection = Class_DBConnector.connect(outputFolder + origOutputDB);
					if (!origDBFile.exists()) {
						Class_DBConnector.createClassificationOutputTables(origConnection, false);
					}
					answered = true;
				} else if (answer.toLowerCase().trim().equals("c")) {
					System.out.println("Please enter the name of the new correctable Database. It will be stored in "
							+ outputFolder);
					BufferedReader ndIn = new BufferedReader(new InputStreamReader(System.in));
					corrOutputDB = ndIn.readLine();
					corrConnection = Class_DBConnector.connect(outputFolder + corrOutputDB);
					Class_DBConnector.createClassificationOutputTables(corrConnection, true);
					System.out.println(
							"Please enter the name of the new original Database. It will be stored in " + outputFolder);
					ndIn = new BufferedReader(new InputStreamReader(System.in));
					origOutputDB = ndIn.readLine();
					origConnection = Class_DBConnector.connect(outputFolder + origOutputDB);
					Class_DBConnector.createClassificationOutputTables(origConnection, false);
					answered = true;
				} else {
					System.out.println("C: invalid answer! please try again...");
					System.out.println();
				}
			}
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
		
		ConfigurableDatabaseClassifier dbClassfy = new ConfigurableDatabaseClassifier(inputConnection, corrConnection,
				origConnection, trainingConnection, queryLimit, fetchSize,

				startId, trainingdataFile);
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("\nstart classification....\n\n");
			sb.append("\ninput DB: " + inputDB.substring(inputDB.lastIndexOf("/") + 1));
			sb.append("\ncorrectable output DB: " + corrDBFile.getName());
			sb.append("\noriginal output DB: " + origDBFile.getName());
			sb.append("\nused Trainingdata: ");
			sb.append(trainingdataFile.substring(trainingdataFile.lastIndexOf("/") + 1));
			sb.append("\n\n");
			dbClassfy.classify(sb, inputTable);
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
