package quenfo.de.uni_koeln.spinfo.information_extraction.applicationsjb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.workflow.Extractor;

/**
 * @author geduldia
 * 
 *         Workflow to extract new competences
 * 
 *         Input: to class 3 (= applicants profile) classified paragraphs
 * 
 *         Output: extracted competences 
 *
 */
public class ExtractNewCompetences {

	// wird an den Namen der OutputDB angehängt
	static String jahrgang = null;

	// Pfad zur Input-DB mit den klassifizierten Paragraphen
	static String inputDB = null;

	// Output-Ordner
	static String outputFolder = null;

	// Name der Output-DB
	static String outputDB = null;

	// txt-File mit allen bereits bekannten (validierten) Kompetenzen (die
	// bekannten Kompetenzn helfen beim Auffinden neuer Kompetenzen)
	static File competences = null;

	// txt-File mit bekannten (typischen) Extraktionsfehlern (würden ansonsten
	// immer wieder vorgeschlagen werden)
	static File noCompetences = null;

	// txt-File mit den Extraktionspatterns
	static File patternsFile = null;
	
	static File modifierFile = null;

	// falls nicht alle Paragraphen aus der Input-DB verwendet werden sollen:
	// hier Anzahl der zu lesenden Paragraphen festlegen
	// -1 = alle
	static int maxCount = -1;

	// falls nur eine bestimmte Anzahl gelesen werden soll, hier die startID
	// angeben
	static int startPos = 0;
	
	// true, falls Koordinationen  in Informationseinheit aufgelöst werden sollen
	static boolean resolveCoordinations = false;
	
	// true, falls Goldstandard-Tabelle erzeugt werden soll
	static boolean gold = false;

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

		loadProperties();
		
		// Verbindung zur Input-DB
		Connection inputConnection = null;
		if (!new File(inputDB).exists()) {
			System.out
					.println("Input-DB '" + inputDB + "' does not exist\nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = IE_DBConnector.connect(inputDB);
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

		// Verbindung zur Output-DB
		if (!new File(outputFolder).exists()) {
			new File(outputFolder).mkdirs();
		}
		Connection outputConnection = null;
		File outputfile = new File(outputFolder + outputDB);
		if (!outputfile.exists()) {
			outputfile.createNewFile();
		}
		outputConnection = IE_DBConnector.connect(outputFolder + outputDB);

		// Start der Extraktion:
		long before = System.currentTimeMillis();
		// Index für die Spalte 'ClassTHREE' anlegen für schnelleren Zugriff
		IE_DBConnector.createIndex(inputConnection, "ClassifiedParagraphs", "ClassTHREE");
		Extractor extractor = new Extractor(outputConnection, competences, noCompetences, patternsFile, modifierFile,
				IEType.COMPETENCE, resolveCoordinations);
		if (maxCount == -1) {
			maxCount = tableSize;
		}
		extractor.extract(startPos, maxCount, tableSize, inputConnection, outputConnection, gold);
		long after = System.currentTimeMillis();
		Double time = (((double) after - before) / 1000) / 60;
		if (time > 60.0) {
			System.out.println("\nfinished Competence-Extraction in " + (time / 60) + " hours");
		} else {
			System.out.println("\nFinished Competence-Extraction in " + time + " minutes");
		}

	}

	private static void loadProperties() throws IOException {
		Properties props = new Properties();		
		InputStream is = MatchCompetences.class.getClassLoader().getResourceAsStream("config.properties");
		props.load(is);
		jahrgang = props.getProperty("jahrgang");
		inputDB = props.getProperty("paraInputDB") + jahrgang + ".db";
		outputFolder = props.getProperty("compIEOutputFolder");
		outputDB = props.getProperty("compIEOutputDB") + jahrgang + ".db";
		competences = new File(props.getProperty("competences"));
		noCompetences = new File(props.getProperty("noCompetences"));
		patternsFile = new File(props.getProperty("compPatterns"));
		modifierFile = new File(props.getProperty("modifier"));
		maxCount = Integer.parseInt(props.getProperty("maxCount"));
		startPos = Integer.parseInt(props.getProperty("startPos"));
		resolveCoordinations = Boolean.parseBoolean(props.getProperty("expandCoordinates"));
		gold = Boolean.parseBoolean(props.getProperty("gold"));
		
		
		
	}
}