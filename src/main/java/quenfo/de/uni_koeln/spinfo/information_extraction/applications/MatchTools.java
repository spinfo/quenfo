package quenfo.de.uni_koeln.spinfo.information_extraction.applications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.log4j.Logger;

import quenfo.de.uni_koeln.spinfo.core.helpers.PropertiesHandler;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.workflow.Extractor;

/**
 * @author jbinnewi
 * 
 *         workflow to match the already validated tools (from tools.txt)
 *         against all as class 3 and/or class 2 classified paragraphs
 * 
 *         input: as class 3 (= applicants profile) and/or class 2 (=job
 *         description) classified paragraphs output: all matching tools
 *         together with their containing sentence
 *
 */
public class MatchTools {

	static Logger log = Logger.getLogger(MatchTools.class);

	// Pfad zur Input-DB mit den klassifizierten Paragraphen
	static String paraInputDB = null;

	// Ordner in dem die neue Output-DB angelegt werden soll
	static String toolMOutputFolder = null;

	// Name der Output-DB
	static String toolMOutputDB = null;

	// txt-File mit allen bereits validierten Tools
	static File tools = null;

	// txt-File zur Speicherung der Match-Statistiken
	static File statisticsFile = null;

	// Anzahl der Paragraphen aus der Input-DB, gegen die gematcht werden soll
	// (-1 = alle)
	static int maxCount;

	// Falls nicht alle Paragraphen gematcht werden sollen, hier die
	// Startposition angeben
	static int startPos;
	
	static int fetchSize;

	// true, falls Koordinationen in Informationseinheit aufgelöst werden sollen
	static boolean expandCoordinates;

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		
		if (args.length > 0) {
			String configPath = args[1];
			loadProperties(configPath);
		}
		
		// Verbindung mit Input-DB
		Connection inputConnection = null;
		if (!new File(paraInputDB).exists()) {
			log.error("Database don't exists " + paraInputDB + "\nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = IE_DBConnector.connect(paraInputDB);
		}

		// Verbindung mit Output-DB
		if (!new File(toolMOutputFolder).exists()) {
			new File(toolMOutputFolder).mkdirs();
		}
		Connection outputConnection = IE_DBConnector.connect(toolMOutputFolder + toolMOutputDB);
		IE_DBConnector.createExtractionOutputTable(outputConnection, IEType.TOOL, false);

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

		// starte Matching
		long before = System.currentTimeMillis();
		IE_DBConnector.createIndex(inputConnection, "ClassifiedParagraphs", "ClassTWO, ClassTHREE");
		Extractor extractor = new Extractor(tools, null, IEType.TOOL, expandCoordinates);
		extractor.stringMatch(statisticsFile, inputConnection, outputConnection, maxCount,
				startPos, fetchSize);
		long after = System.currentTimeMillis();
		double time = (((double) after - before) / 1000) / 60;
		if (time > 60.0) {
			log.info("finished matching in " + (time / 60) + " hours");
		} else {
			log.info("finished matching in " + time + " minutes");
		}

	}

	private static void loadProperties(String folderPath) throws IOException {

		File configFolder = new File(folderPath);

		if (!configFolder.exists()) {
			System.err.println("Config Folder " + folderPath + " does not exist."
					+ "\nPlease change configuration and start again.");
			System.exit(0);
		}
		
		// initialize and load all properties files
		String quenfoData = configFolder.getParent();		
		PropertiesHandler.initialize(configFolder);
		
		
		// get values from properties files
		paraInputDB = quenfoData + "/sqlite/classification/" + PropertiesHandler.getStringProperty("general", "classifiedParagraphs");
		
		maxCount = PropertiesHandler.getIntProperty("matching", "queryLimit");
		startPos = PropertiesHandler.getIntProperty("matching", "startPos");
		fetchSize = PropertiesHandler.getIntProperty("matching", "fetchSize");
		expandCoordinates = PropertiesHandler.getBoolProperty("matching", "expandCoordinates");
		
		String toolsFolder = quenfoData + "/resources/information_extraction/tools/";	
		tools = new File(toolsFolder + PropertiesHandler.getStringProperty("matching", "tools"));	
		
		statisticsFile = new File(toolsFolder + PropertiesHandler.getStringProperty("matching", "toolMatchingStats"));
		
		toolMOutputFolder = quenfoData + "/sqlite/matching/tools/";
		toolMOutputDB = PropertiesHandler.getStringProperty("matching", "toolMOutputDB");
		
	}
}
