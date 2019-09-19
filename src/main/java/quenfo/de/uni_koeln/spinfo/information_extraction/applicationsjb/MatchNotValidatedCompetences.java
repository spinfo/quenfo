package quenfo.de.uni_koeln.spinfo.information_extraction.applicationsjb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import quenfo.de.uni_koeln.spinfo.information_extraction.applicationsjpa.MatchCompetences;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.workflow.Extractor;

/**
 * @author geduldia
 * 
 *         workflow to match the not validated competence-extractions (output of
 *         the app.: ExtractNewCompetences) against all class 3 paragraphs
 *         
 *         input: all as class 3 (= applicants profile) classified paragraphs
 *         output: all matching 'competences' togetjer with their containing sentence
 *
 */
public class MatchNotValidatedCompetences {
	
	static Logger log = Logger.getLogger(MatchNotValidatedCompetences.class);

//	// wird an den Namen der Output-DB angehängt
//	static String jahrgang = null;//"2011";

	// Pfad zur Input-DB mit den klassifizierten Paragraphen
	static String paraInputDB = /* "D:/Daten/sqlite/CorrectableParagraphs.db"; */null;//"C:/sqlite/classification/CorrectableParagraphs_"
			//+ jahrgang + ".db"; //

	// Ordner in dem die neue Output-DB angelegt werden soll
	static String compMOutputFolder = /* "D:/Daten/sqlite/"; */null;//"C:/sqlite/matching/competences/"; //

	// Name der Output-DB
	static String compMnotValOutputDB = null;//"NotValidatedCompetenceMatches_" + jahrgang + ".db";

	// DB mit den extrahierten Kompetenz-Vorschlägen
	static String extractedCompsDB = null;//"C:/sqlite/information_extraction/competences/CorrectableCompetences_" + jahrgang
			//+ ".db";

	// txt-File mit den Modifizierern
	static File modifier = null;//new File("information_extraction/data/competences/modifier.txt");

	// txt-File zum Speichern der Match-Statistik
	static File statisticsFile = null;//new File("information_extraction/data/competences/notValidatedMatchingStats.txt");

	// Anzahl der Paragraphen aus der Input-DB, gegen die gematcht werden soll
	// (-1 = alle)
	static int maxCount = -1;

	// Falls nicht alle Paragraphen gematcht werden sollen, hier die
	// Startposition angeben
	static int startPos = 0;
	
	// true, falls Koordinationen  in Informationseinheit aufgelöst werden sollen
	static boolean expandCoordinates = false;

	public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
		
		if (args.length > 0) {
			String configPath = args[0];
			loadProperties(configPath);
		}
		
		// Verbindung mit Input-DB
		Connection inputConnection = null;
		if (!new File(paraInputDB).exists()) {
			System.out.println(
					"Database don't exists " + paraInputDB + "\nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = IE_DBConnector.connect(paraInputDB);
		}
		// Verbindung mit Output-DB
		if (!new File(compMOutputFolder).exists()) {
			new File(compMOutputFolder).mkdirs();
		}
		Connection outputConnection = IE_DBConnector.connect(compMOutputFolder + compMnotValOutputDB);
		IE_DBConnector.createExtractionOutputTable(outputConnection, IEType.COMPETENCE, false);

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

		// Einlesen der extrahierten Kompetenzvorschläge
		log.info("read not validated Competences from DB: " + extractedCompsDB);
		Connection extractionsConnection = IE_DBConnector.connect(extractedCompsDB);
		Set<String> extractions = IE_DBConnector.readEntities(extractionsConnection, IEType.COMPETENCE);
		// Kompetenz-Vorschläge in eine txt-Datei schreiben
		// (Der Umweg über den txt-File wird genommen, um den bereits
		// bestehenden Workflow zum Matchen der validierten Kompetenzen nutzen
		// zu können)
		File notValidatedCompetences = new File("information_extraction/data/competences/notValidatedCompetences.txt");
		PrintWriter out = new PrintWriter(new FileWriter(notValidatedCompetences));
		for (String extracted : extractions) {
			out.write("\n" + extracted);
		}
		out.close();

		// start Matching
		long before = System.currentTimeMillis();
		// erzeugt einen Index auf die Spalte 'ClassTHREE' (falls noch nicht
		// vorhanden)
		IE_DBConnector.createIndex(inputConnection, "ClassifiedParagraphs", "ClassTHREE");
		Extractor extractor = new Extractor(notValidatedCompetences, modifier, IEType.COMPETENCE, expandCoordinates);
		extractor.stringMatch(statisticsFile, inputConnection, outputConnection, maxCount, startPos);
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
		String quenfoData = configFolder.getParent();

		// load general properties (db path etc.)
		Properties generalProps = loadPropertiesFile(configFolder.getAbsolutePath() + "/general.properties");

		paraInputDB = quenfoData + "/sqlite/classification/" + generalProps.getProperty("classifiedParagraphs");// + jahrgang + ".db";
		
		
		// load matching properties (tools list etc.)
		Properties matchProps = loadPropertiesFile(configFolder.getAbsolutePath() + "/matching.properties");
		
		maxCount = Integer.parseInt(matchProps.getProperty("maxCount"));
		startPos = Integer.parseInt(matchProps.getProperty("startPos"));
		expandCoordinates = Boolean.parseBoolean(matchProps.getProperty("expandCoordinates"));
		
//		notCatComps = new File(quenfoData + "/information_extraction/data/competences/" + matchProps.getProperty("competences"));
		modifier = new File(quenfoData + "/information_extraction/data/competences/" + matchProps.getProperty("modifier"));
		
		statisticsFile = new File(quenfoData + "/information_extraction/data/competences/" + matchProps.getProperty("compMatchingStats"));
		
		compMOutputFolder = quenfoData + "/sqlite/matching/competences/";
		compMnotValOutputDB = matchProps.getProperty("compMNotValOutputDB");
		
	}

	private static Properties loadPropertiesFile(String path) throws IOException {

		File propsFile = new File(path);
		if (!propsFile.exists()) {
			System.err.println(
					"Config File " + path + " does not exist." + "\nPlease change configuration and start again.");
			System.exit(0);
		}

		Properties properties = new Properties();
		InputStream is = new FileInputStream(propsFile);
		properties.load(is);
		return properties;
	}

}
