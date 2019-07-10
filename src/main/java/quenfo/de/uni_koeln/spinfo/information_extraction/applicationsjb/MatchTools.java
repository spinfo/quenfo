package quenfo.de.uni_koeln.spinfo.information_extraction.applicationsjb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.log4j.Logger;

import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.workflow.Extractor;

/**
 * @author geduldia
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

	// wird an den Namen der Output-DB angehängt
	//static String jahrgang = null;//"2011";

	// Pfad zur Input-DB mit den klassifizierten Paragraphen
	static String paraInputDB = null;//"C:/sqlite/classification/CorrectableParagraphs_" + jahrgang + ".db";

	// Ordner in dem die neue Output-DB angelegt werden soll
	static String toolMOutputFolder = null;///* "D:/Daten/sqlite/"; */ "C:/sqlite/matching/tools/"; //

	// Name der Output-DB
	static String toolMOutputDB = null;//"ToolMatches_" + jahrgang + ".db";

	// txt-File mit allen bereits validierten Tools
	static File tools = null;//new File("information_extraction/data/tools/tools.txt");

	// txt-File zur Speicherung der Match-Statistiken
	static File statisticsFile = null;//new File("information_extraction/data/tools/matchingStats.txt");

	// Anzahl der Paragraphen aus der Input-DB, gegen die gematcht werden soll
	// (-1 = alle)
	static int maxCount;// = -1;

	// Falls nicht alle Paragraphen gematcht werden sollen, hier die
	// Startposition angeben
	static int startPos;// = 0;

	// true, falls Koordinationen in Informationseinheit aufgelöst werden sollen
	static boolean resolveCoordinations;// = true;

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		
		loadProperties();
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
		Extractor extractor = new Extractor(tools, null, IEType.TOOL, resolveCoordinations);
		extractor.stringMatch(statisticsFile, inputConnection, outputConnection, maxCount,
				startPos);
		long after = System.currentTimeMillis();
		double time = (((double) after - before) / 1000) / 60;
		if (time > 60.0) {
			log.info("finished matching in " + (time / 60) + " hours");
		} else {
			log.info("finished matching in " + time + " minutes");
		}

	}

	// TODO load props
	private static void loadProperties() throws IOException {
		Properties props = new Properties();
		InputStream is = MatchCompetences.class.getClassLoader().getResourceAsStream("config.properties");
		props.load(is);
		
		String jahrgang = props.getProperty("jahrgang");
		paraInputDB = props.getProperty("paraInputDB") + jahrgang + ".db";
		toolMOutputFolder = props.getProperty("toolMOutputFolder");
		toolMOutputDB = props.getProperty("toolMOutputDB") + jahrgang + ".db";
		tools = new File(props.getProperty("tools"));
		maxCount = Integer.parseInt(props.getProperty("maxCount"));
		statisticsFile = new File(props.getProperty("statisticsFile"));
		startPos = Integer.parseInt(props.getProperty("startPos"));
		//expandCoordinates = Boolean.parseBoolean(props.getProperty("expandCoordinates"));
	}
}
