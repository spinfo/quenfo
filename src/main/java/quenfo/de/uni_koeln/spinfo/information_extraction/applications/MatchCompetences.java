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
 *         workflow to match the already validated competences (from competences.txt) against the as class 3 classified paragraphs
 *         
 *         input: as class 3 (= applicants profile) classified paragraphs
 *         output: all matching competences together with their containing sentence

 */
public class MatchCompetences {
	
	static IEType ieType;

	// Pfad zur Input-DB mit den klassifizierten Paragraphen
	static String paraInputDB;

	// Ordner in dem die neue Output-DB angelegt werden soll
	static String outputFolder;

	// Name der Output-DB
	static String outputDB;

	// txt-File mit den validierten Kompetenzen
	static File notCatComps;
	
	// tei-File mit kategorisierten Kompetenzen
	static File catComps;
	
	// Ebene, auf der die Kompetenz zugeordnet werden soll(div1, div2, div3, entry, form, orth)
	static String category;

	// txt-File mit allen 'Modifier'-Ausdrücken
	static File modifier;

	// txt-File zur Speicherung der Match-Statistiken
	static File statisticsFile;

	// Anzahl der Paragraphen aus der Input-DB, gegen die gematcht werden soll
	// (-1 = alle)
	static int maxCount;

	// Falls nicht alle Paragraphen gematcht werden sollen, hier die
	// Startposition angeben
	static int startPos;
	
	static int fetchSize;
	
	// true, falls Koordinationen  in Informationseinheit aufgelöst werden sollen
	static boolean expandCoordinates;

	public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
		
		if (args.length > 0) {
			String configPath = args[1];
			loadProperties(configPath);
		}
		
		// Verbindung mit Input-DB
		Connection inputConnection = null;
		if (!new File(paraInputDB).exists()) {
			System.out
					.println("Database don't exists " + paraInputDB + "\nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = IE_DBConnector.connect(paraInputDB);
		}

		// Verbindung mit Output-DB
		if (!new File(outputFolder).exists()) {
			new File(outputFolder).mkdirs();
		}
		Connection outputConnection = IE_DBConnector.connect(outputFolder + outputDB);
		IE_DBConnector.createExtractionOutputTable(outputConnection, ieType, false);
		
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
		//erzeugt einen Index auf die Spalte 'ClassTHREE' (falls noch nicht vorhanden)
		IE_DBConnector.createIndex(inputConnection, "ClassifiedParagraphs", "ClassTHREE");
		Extractor extractor = new Extractor(notCatComps, modifier, catComps, category, ieType, expandCoordinates);
		extractor.stringMatch(statisticsFile, inputConnection, outputConnection, maxCount, startPos, fetchSize);
		long after = System.currentTimeMillis();
		double time = (((double) after - before) / 1000) / 60;
		if (time > 60.0) {
			System.out.println("\nfinished matching in " + (time / 60) + " hours");
		} else {
			System.out.println("\nfinished matching in " + time + " minutes");
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


		// get values from properties files
		
		ieType = PropertiesHandler.getSearchType("matching");

		paraInputDB = quenfoData + "/sqlite/classification/" + PropertiesHandler.getStringProperty("general", "classifiedParagraphs");// + jahrgang + ".db";
		
		maxCount = PropertiesHandler.getIntProperty("matching", "queryLimit");
		startPos = PropertiesHandler.getIntProperty("matching", "startPos");
		fetchSize = PropertiesHandler.getIntProperty("matching", "fetchSize");
		expandCoordinates = PropertiesHandler.getBoolProperty("matching", "expandCoordinates");
		
		String competencesFolder = quenfoData + "/resources/information_extraction/competences/";
		notCatComps = new File(competencesFolder + PropertiesHandler.getStringProperty("matching", "competences"));
		modifier = new File(competencesFolder + PropertiesHandler.getStringProperty("matching", "modifier"));
		
		statisticsFile = new File(competencesFolder + PropertiesHandler.getStringProperty("matching", "compMatchingStats"));
		
		outputFolder = quenfoData + "/sqlite/matching/competences/";
		outputDB = PropertiesHandler.getStringProperty("matching", "compMOutputDB");
		
		
	
		
		
		
	}

	
	

}
