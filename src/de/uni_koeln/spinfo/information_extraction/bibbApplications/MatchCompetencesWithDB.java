package de.uni_koeln.spinfo.information_extraction.bibbApplications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import de.uni_koeln.spinfo.information_extraction.data.IEType;
import de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import de.uni_koeln.spinfo.information_extraction.workflow.Extractor;

public class MatchCompetencesWithDB {

	///////////////////////////
	// APP-CONFIGURATION
	/////////////////////////////
	
	// Jahrgang bzw. Name der Tabelle (z.B. DL_ALL_Spinfo) die ursprünglich klassifiziert wurde (= Endung des DB-Namens)	
	static String jahrgang = "DL_ALL_Spinfo";

	// Pfad zur Input-DB (Tabellenname wird automatsich hinzugefügt - z.B. CorrectableParagraphs_DL_ALL_Spinfo.db)
	static String inputDB = /*"D:/Daten/sqlite/CorrectableParagraphs.db"; */"C:/sqlite/CorrectableParagraphs_"+jahrgang+".db"; // 

	// Pfad zum Ordner in dem die neue Output-DB angelegt werden soll
	static String outputFolder = /*"D:/Daten/sqlite/";*/"C:/sqlite/"; // 

	// Name der Output-DB (Tabellenname wird automatsich angehänt - z.B. Competences_DL_ALL_Spinfo.db)
	static String outputDB = "Competences_"+jahrgang+".db";

	// Pfad zur Textdatei mit allen Kompetenzen 
	static File competences = new File("information_extraction/data/competences/competences.txt");

	// Pfad zur Textdatei mit allen 'Importance'-Ausdrücken
	static File importanceTerms = new File("information_extraction/data/competences/importanceTerms.txt");

	// Pfad zur Textdatei für die Match-Staristik
	static File statisticsFile = new File("information_extraction/data/competences/competenceStatistics.txt");

	/////////////////////////////
	// END
	/////////////////////////////

	public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {

		// Connect to input database
		Connection inputConnection = null;
		if (!new File(inputDB).exists()) {
			System.out.println("Database don't exists " + inputDB + "\nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = IE_DBConnector.connect(inputDB);
		}

		// Connect to output database
		Connection outputConnection = IE_DBConnector.connect(outputFolder + outputDB);

		// start matching
		Extractor extractor = new Extractor(null, competences, null, null, importanceTerms, IEType.COMPETENCE);
		extractor.finalStringMatch(statisticsFile, inputConnection, outputConnection);
	}

}
