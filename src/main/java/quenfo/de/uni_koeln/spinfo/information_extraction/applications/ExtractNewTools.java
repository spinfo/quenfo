package quenfo.de.uni_koeln.spinfo.information_extraction.applications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.workflow.Extractor;

/**
 * @author geduldia
 * 
 *         Workflow to extract new tools
 * 
 *         Input: in class 3 (= applicants profile) and/or class 2 (=
 *         jobbdescription) classified paragraphs
 * 
 *         Output: extracted tools
 *
 */
public class ExtractNewTools {

	// wird an den Namen der OutputDB angehängt
	static String jahrgang = "2011";

	// Pfad zur Input-DB mit den klassifizierten Paragraphen
	static String inputDB = "C:/sqlite/classification/CorrectableParagraphs_" + jahrgang + ".db";

	// Output-Ordner
	static String outputFolder = "C:/sqlite/information_extraction/tools/";

	// Name der Output-DB
	static String outputDB = "CorrectableTools_" + jahrgang + ".db";

	// txt-File mit allen bereits bekannten (validierten) Tools (die
	// bekannten Tools helfen beim Auffinden neuer Kompetenzen)
	static File tools = new File("information_extraction/data/tools/tools.txt");

	// txt-File mit bekannten (typischen) Extraktionsfehlern (würden ansonsten
	// immer wieder vorgeschlagen werden)
	static File noTools = new File("information_extraction/data/tools/noTools.txt");

	// txt-File mit den Extraktionspatterns
	static File contextFile = new File("information_extraction/data/tools/toolContexts.txt");

	// falls nicht alle Paragraphen aus der Input-DB verwendet werden sollen:
	// hier Anzahl der zu lesenden Paragraphen festlegen
	// -1 = alle
	static int maxCount = -1;

	// falls nur eine bestimmte Anzahl gelesen werden soll, hier die startID
	// angeben
	static int startPos = 0;

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

		// Verbindung zur Input-DB
		Connection inputConnection = null;
		if (!new File(inputDB).exists()) {
			System.out.println("Database don't exists " + inputDB + "\nPlease change configuration and start again.");
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

		// Connect to the output-DB
		if (!new File(outputFolder).exists()) {
			new File(outputFolder).mkdirs();
		}
		Connection outputConnection = null;
		File outputfile = new File(outputFolder + outputDB);
		if (!outputfile.exists()) {
			outputfile.createNewFile();
		}
		outputConnection = IE_DBConnector.connect(outputFolder + outputDB);

		// Start der Extraktion
		long before = System.currentTimeMillis();
		// Index für die Spalten 'ClassTWO' und 'ClassTHREE' anlegen für
		// schnelleren Zugriff
		IE_DBConnector.createIndex(inputConnection, "ClassifiedParagraphs", "ClassTWO, ClassTHREE");
		Extractor extractor = new Extractor(outputConnection, tools, noTools, contextFile, null, IEType.TOOL);
		if (maxCount == -1) {
			maxCount = tableSize;
		}
		extractor.extract(startPos, maxCount, tableSize, inputConnection, outputConnection);
		long after = System.currentTimeMillis();
		double time = (((double) after - before) / 1000) / 60;
		if (time > 60.0) {
			System.out.println("\nfinished Tool-Extraction in " + (time / 60) + " hours");
		} else {
			System.out.println("\nFinished Tool-Extraction in " + time + " minutes");
		}
	}
}
