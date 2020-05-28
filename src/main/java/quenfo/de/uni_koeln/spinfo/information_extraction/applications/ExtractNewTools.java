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
 *         Workflow to extract new tools
 * 
 *         Input: in class 3 (= applicants profile) and/or class 2 (=
 *         job description) classified paragraphs
 * 
 *         Output: extracted tools
 *
 */
public class ExtractNewTools {

	// Pfad zur Input-DB mit den klassifizierten Paragraphen
	static String paraInputDB = null;

	// Output-Ordner
	static String toolsIEOutputFolder = null;

	// Name der Output-DB
	static String toolsIEOutputDB = null;

	// txt-File mit allen bereits bekannten (validierten) Tools (die
	// bekannten Tools helfen beim Auffinden neuer Kompetenzen)
	static File tools = null;

	// txt-File mit bekannten (typischen) Extraktionsfehlern (würden ansonsten
	// immer wieder vorgeschlagen werden)
	static File noTools = null;

	// txt-File mit den Extraktionspatterns
	static File toolsPatterns = null;

	// falls nicht alle Paragraphen aus der Input-DB verwendet werden sollen:
	// hier Anzahl der zu lesenden Paragraphen festlegen
	// -1 = alle
	static int maxCount = -1;

	// falls nur eine bestimmte Anzahl gelesen werden soll, hier die startID
	// angeben
	static int startPos = 0;
	
	static int fetchSize;

	// true, falls Koordinationen in Informationseinheit aufgelöst werden sollen
	static boolean expandCoordinates = true;

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

		if (args.length > 0) {
			String configPath = args[1];
			loadProperties(configPath);
		}

		// Verbindung zur Input-DB
		Connection inputConnection = null;
		if (!new File(paraInputDB).exists()) {
			System.out
					.println("Database don't exists " + paraInputDB + "\nPlease change configuration and start again.");
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
		if (maxCount > tableSize - startPos) {
			maxCount = tableSize - startPos;
		}

		// Connect to the output-DB
		if (!new File(toolsIEOutputFolder).exists()) {
			new File(toolsIEOutputFolder).mkdirs();
		}
		Connection outputConnection = null;
		File outputfile = new File(toolsIEOutputFolder + toolsIEOutputDB);
		if (!outputfile.exists()) {
			outputfile.createNewFile();
		}
		outputConnection = IE_DBConnector.connect(toolsIEOutputFolder + toolsIEOutputDB);

		// Start der Extraktion
		long before = System.currentTimeMillis();
		// Index für die Spalten 'ClassTWO' und 'ClassTHREE' anlegen für
		// schnelleren Zugriff
		IE_DBConnector.createIndex(inputConnection, "ClassifiedParagraphs", "ClassTWO, ClassTHREE");
		Extractor extractor = new Extractor(outputConnection, tools, noTools, toolsPatterns, null, IEType.TOOL, false);
		if (maxCount == -1) {
			maxCount = tableSize;
		}
		extractor.extract(startPos, maxCount, fetchSize, tableSize, inputConnection, outputConnection);
		long after = System.currentTimeMillis();
		double time = (((double) after - before) / 1000) / 60;
		if (time > 60.0) {
			System.out.println("\nfinished Tool-Extraction in " + (time / 60) + " hours");
		} else {
			System.out.println("\nFinished Tool-Extraction in " + time + " minutes");
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


		paraInputDB = quenfoData + "/sqlite/classification/" + PropertiesHandler.getStringProperty("general", "classifiedParagraphs");// + jahrgang + ".db";
		
		maxCount = PropertiesHandler.getIntProperty("ie", "queryLimit");
		startPos = PropertiesHandler.getIntProperty("ie", "startPos");
		fetchSize = PropertiesHandler.getIntProperty("ie", "fetchSize");
		expandCoordinates = PropertiesHandler.getBoolProperty("ie", "expandCoordinates");
		
		String toolsFolder = quenfoData + "/resources/information_extraction/tools/";
		
		tools = new File(toolsFolder + PropertiesHandler.getStringProperty("ie", "tools"));
		noTools = new File(toolsFolder + PropertiesHandler.getStringProperty("ie", "noTools"));
		toolsPatterns = new File(toolsFolder + PropertiesHandler.getStringProperty("ie", "toolsPatterns"));

		toolsIEOutputFolder = quenfoData + "/sqlite/information_extraction/tools/";
		toolsIEOutputDB = PropertiesHandler.getStringProperty("ie", "toolsIEOutputDB");
	}
}
