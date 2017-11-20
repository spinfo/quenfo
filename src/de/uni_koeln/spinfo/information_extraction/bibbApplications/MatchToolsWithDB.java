package de.uni_koeln.spinfo.information_extraction.bibbApplications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import de.uni_koeln.spinfo.information_extraction.data.IEType;
import de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import de.uni_koeln.spinfo.information_extraction.workflow.Extractor;

public class MatchToolsWithDB {

	///////////////////////////
	// APP-CONFIGURATION
	/////////////////////////////
	
	// Jahrgang bzw. Name der Tabelle (z.B. DL_ALL_Spinfo) die ursprünglich klassifiziert wurde (= Endung des DB-Namens)	
	static String jahrgang = "Jahgang_2011";

	// Pfad zur Input-DB (Tabellenname wird automatsich hinzugefügt - z.B. CorrectableParagraphs_DL_ALL_Spinfo.db)
	static String inputDB =   /*"D:/Daten/sqlite/CorrectableParagraphs_"+jahrgang+".db"; */"C:/sqlite/CorrectableParagraphs_"+jahrgang+".db";

	// Pfad zum Ordner in dem die neue Output-DB angelegt werden soll
	static String outputFolder = /*"D:/Daten/sqlite/"; */ "C:/sqlite/"; //

	// Name der Output-DB (Tabellenname wird automatsich angehänt - z.B. Tools_DL_ALL_Spinfo.db)
	static String outputDB = "Tools_"+jahrgang+".db";

	// Pfad zur Textdatei mit allen Tools
	static File tools = new File("information_extraction/data/tools/tools.txt");

	// Pfad zur Textdatei für die Match-Staristik
	static File statisticsFile = new File("information_extraction/data/tools/toolStatistics.txt");


	/////////////////////////////
	// END
	/////////////////////////////

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

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
		Extractor extractor = new Extractor(null, tools, null, null, IEType.TOOL);
		extractor.finalStringMatch(statisticsFile, inputConnection, outputConnection);
		

	}

}
