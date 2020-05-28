package quenfo.de.uni_koeln.spinfo.information_extraction.applications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import quenfo.de.uni_koeln.spinfo.core.helpers.PropertiesHandler;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.workflow.Extractor;

/**
 * @author jbinnewi
 * 
 *         Workflow to extract new competences
 * 
 *         Input: to class 3 (= applicants profile) classified paragraphs
 * 
 *         Output: extracted competences
 *         
 *         Uses configurations defined in config files
 *
 */
public class ExtractNewCompetences {
	
	static IEType ieType;

	// Pfad zur Input-DB mit den klassifizierten Paragraphen
	static String paraInputDB;

	// Output-Ordner
	static String compIEoutputFolder;

	// Name der Output-DB
	static String compIEOutputDB;

	// txt-File mit allen bereits bekannten (validierten) Kompetenzen (die
	// bekannten Kompetenzn helfen beim Auffinden neuer Kompetenzen)
	static File competences;

	// txt-File mit bekannten (typischen) Extraktionsfehlern (würden ansonsten
	// immer wieder vorgeschlagen werden)
	static File noCompetences;

	// txt-File mit den Extraktionspatterns
	static File compPatterns;

	// txt-File mit bekannten modifiern ("vorausgesetzt" etc.)
	static File modifier;

	// falls nicht alle Paragraphen aus der Input-DB verwendet werden sollen:
	// hier Anzahl der zu lesenden Paragraphen festlegen
	// -1 = alle
	static int queryLimit;

	// falls nur eine bestimmte Anzahl gelesen werden soll, hier die startID
	// angeben
	static int startPos;
	
	static int fetchSize;

	// true, falls Koordinationen in Informationseinheit aufgelöst werden sollen
	static boolean expandCoordinates;

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

		if (args.length > 0) {
			String configPath = args[1];
			loadProperties(configPath);
		}

		// Verbindung zur Input-DB
		Connection inputConnection = null;
		if (!new File(paraInputDB).exists()) {
			System.out.println(
					"Input-DB '" + paraInputDB + "' does not exist\nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = IE_DBConnector.connect(paraInputDB);
		}

		// Prüfe ob maxCount und startPos gültige Werte haben
		String query = "SELECT COUNT(*) FROM ClassifiedParagraphs;";
		Statement stmt = inputConnection.createStatement();
		ResultSet countResult = stmt.executeQuery(query);
		int tableSize = countResult.getInt(1);
		stmt.close();
		if (tableSize <= startPos) {
			System.out.println("startPosition (" + startPos + ")is greater than tablesize (" + tableSize + ")");
			System.out.println("please select a new startPosition and try again");
			System.exit(0);
		}
		if (queryLimit > tableSize - startPos) {
			queryLimit = tableSize - startPos;
		}

		// Verbindung zur Output-DB
		if (!new File(compIEoutputFolder).exists()) {
			new File(compIEoutputFolder).mkdirs();
		}
		Connection outputConnection = null;
		File outputfile = new File(compIEoutputFolder + compIEOutputDB);
		if (!outputfile.exists()) {
			outputfile.createNewFile();
		}

		outputConnection = IE_DBConnector.connect(compIEoutputFolder + compIEOutputDB);

		// Start der Extraktion:
		long before = System.currentTimeMillis();
		// Index für die Spalte 'ClassTHREE' anlegen für schnelleren Zugriff
		IE_DBConnector.createIndex(inputConnection, "ClassifiedParagraphs", "ClassTHREE");
		Extractor extractor = new Extractor(outputConnection, competences, noCompetences, compPatterns, modifier,
				ieType, expandCoordinates);
		if (queryLimit == -1) {
			queryLimit = tableSize;
		}
		extractor.extract(startPos, queryLimit, fetchSize, tableSize, inputConnection, outputConnection);
		long after = System.currentTimeMillis();
		Double time = (((double) after - before) / 1000) / 60;
		if (time > 60.0) {
			System.out.println("\nfinished Competence-Extraction in " + (time / 60) + " hours");
		} else {
			System.out.println("\nFinished Competence-Extraction in " + time + " minutes");
		}

	}

	private static void loadProperties(String folderPath) throws IOException {

		File configFolder = new File(folderPath);

		if (!configFolder.exists()) {
			System.err.println("Config Folder " + folderPath + " does not exist."
					+ "\nPlease change configuration and start again.");
			System.exit(0);
		}
		
		//initialize and load all properties files
		String quenfoData = configFolder.getParent();
		PropertiesHandler.initialize(configFolder);
		
		ieType = PropertiesHandler.getSearchType("ie");


		paraInputDB = quenfoData + "/sqlite/classification/" + PropertiesHandler.getStringProperty("general", "classifiedParagraphs");
		
		queryLimit = PropertiesHandler.getIntProperty("ie", "queryLimit");
		startPos = PropertiesHandler.getIntProperty("ie", "startPos");
		fetchSize = PropertiesHandler.getIntProperty("ie", "fetchSize");
		expandCoordinates = PropertiesHandler.getBoolProperty("ie", "expandCoordinates");
		
		
		String competencesFolder = quenfoData + "/resources/information_extraction/competences/";
		competences = new File(competencesFolder + PropertiesHandler.getStringProperty("ie", "competences"));
		noCompetences = new File(competencesFolder + PropertiesHandler.getStringProperty("ie", "noCompetences"));
		modifier = new File(competencesFolder + PropertiesHandler.getStringProperty("ie", "modifier"));
		compPatterns = new File(competencesFolder + PropertiesHandler.getStringProperty("ie", "compPatterns"));
		
		compIEoutputFolder = quenfoData + "/sqlite/information_extraction/competences/";
		compIEOutputDB = PropertiesHandler.getStringProperty("ie", "compIEOutputDB");
		
	}

}
