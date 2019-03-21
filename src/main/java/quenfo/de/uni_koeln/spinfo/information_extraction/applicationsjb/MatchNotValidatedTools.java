package quenfo.de.uni_koeln.spinfo.information_extraction.applicationsjb;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import org.apache.log4j.Logger;

import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.workflow.Extractor;

/**
 * @author geduldia
 * 
 *         workflow to match the not validated tool-extractions (output of
 *         the app.: ExtractNewTools) against all class 3 and/or class 2 paragraphs
 *         
 *         input: all as class 3 (= applicants profile) and/or class 2 (= job description) classified paragraphs
 *         output: all matching 'tools' together with their containing sentence
 *
 */
public class MatchNotValidatedTools {
	
	static Logger log = Logger.getLogger(MatchNotValidatedTools.class);

	// wird an den Namen der Output-DB angehängt
	static String jahrgang = "2011";

	// Pfad zur Input-DB mit den klassifizierten Paragraphen
	static String paragraphsDB = /*"D:/Daten/sqlite/CorrectableParagraphs.db"; */"C:/sqlite/classification/CorrectableParagraphs_"+jahrgang+".db"; // 

	// Ordner in dem die neue Output-DB angelegt werden soll
	static String outputFolder = /*"D:/Daten/sqlite/";*/"C:/sqlite/matching/tools/"; // 

	// Name der Output-DB 
	static String outputDB = "NotValidatedToolMatches_"+jahrgang+".db";
	
	//DB mit den extrahierten Tool-Vorschlägen 
	static String extratedCompsDB = "C:/sqlite/information_extraction/tools/CorrectableTools_"+jahrgang+".db";

	// txt-File zum Speichern der Match-Statistik
	static File statisticsFile = new File("information_extraction/data/tools/notValidatedMatchingStats.txt");
	
	//Anzahl der Paragraphen aus der Input-DB, gegen die gematcht werden soll (-1 = alle)
	static int maxCount = -1;
	
	//Falls nicht alle Paragraphen gematcht werden sollen, hier die Startposition angeben
	static int startPos = 0;
	
	// true, falls Koordinationen  in Informationseinheit aufgelöst werden sollen
	static boolean resolveCoordinations = false;

	public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {

		// Verbindung mit Input-DB
		Connection inputConnection = null;
		if (!new File(paragraphsDB).exists()) {
			System.out.println("Database don't exists " + paragraphsDB + "\nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = IE_DBConnector.connect(paragraphsDB);
		}

		// Verbindung mit Output-DB
		if(!new File(outputFolder).exists()){
			new File(outputFolder).mkdirs();
		}
		Connection outputConnection = IE_DBConnector.connect(outputFolder + outputDB);
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
			
		//Einlesen der extrahierten Tool-Vorschläge 
		Connection extractionsConnection = IE_DBConnector.connect(extratedCompsDB);
		Set<String> extractions = IE_DBConnector.readEntities(extractionsConnection, IEType.TOOL);
		//Tool-Vorschläge in eine txt-Datei schreiben
		//(Der Umweg über den Text-File wird genoommen, um den bereits bestehenden Workflow zum Matchen der validierten Tools nutzen zu können)
		File notValidatedTools = new File("information_extraction/data/tools/notValidatedTools.txt");
		PrintWriter out = new PrintWriter(new FileWriter(notValidatedTools));
		for (String extracted : extractions) {
			out.write("\n"+extracted);
		}
		out.close();
	
		// starte Matching
		long before = System.currentTimeMillis();
		//erzeugt einen Index auf die Spalten 'ClassTWO' und 'ClassTHREE' (falls noch nicht vorhanden)
		IE_DBConnector.createIndex(inputConnection, "ClassifiedParagraphs", "ClassTWO, ClassTHREE");
		Extractor extractor = new Extractor(notValidatedTools, null, IEType.TOOL, resolveCoordinations);
		extractor.stringMatch(statisticsFile, inputConnection, outputConnection, (outputFolder+outputDB), maxCount, startPos);
		long after = System.currentTimeMillis();
		double time = (((double) after - before)/1000)/60;
		if(time > 60.0){
			log.info("finished matching in " + (time/60) +" hours");
		}
		else{
			log.info("finished matching in " + time +" minutes");
		}
	}

}
