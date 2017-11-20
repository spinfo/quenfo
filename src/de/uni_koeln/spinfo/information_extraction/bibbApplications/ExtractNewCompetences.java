package de.uni_koeln.spinfo.information_extraction.bibbApplications;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_koeln.spinfo.grouping.SimilarityCalculator;
import de.uni_koeln.spinfo.information_extraction.data.Context;
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.data.IEType;
import de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import de.uni_koeln.spinfo.information_extraction.workflow.Extractor;

public class ExtractNewCompetences {

	/////////////////////////////
	// APP-CONFIGURATION
	/////////////////////////////
	
	//Gewichtung der Stringähnlichkeit für die Gruppierung ähnlicher Kompetenzen
	//Sollte i.d.R nicht kleiner als 1.0 sein und nicht höher als 1.4 (--> bei 1.5 werden nur noch exakt identische Strings als ähnlich bewertet - unter 1.0 werden die Gruppen zu heterogen)
	static double similarityScore = 1.0;
	
	//Jahrgang bzw. Name der Tabelle (z.B.  DL_ALL_Spinfo )aus der ursprünglich klassifiziert wurde (= Endung des DB-Namens)
	static String jahrgang = "Jahgang_2011";
	
	// Pfad zur Input-DB (TabellenName wird automatisch hinzugefügt  z.b. .../CorrectableParagraphs_DL_ALL_Spinfo.db)
	static String inputDB = /* "D:/Daten/sqlite/CorrectableParagraphs.db"; */"C:/sqlite/CorrectableParagraphs_"+jahrgang+".db";

	// Pfad zur Output-DB (Tabellenname bzw. Jahrgang wird automatsich angehängt - zb. CorrectableConpetences_DL_ALL_Spinfo.db)
	static String outputDB = /* "D:/Daten/sqlite/CorrectableCompetences.db"; */ "C:/sqlite/CorrectableCompetences_"+jahrgang+".db";
	
	// Pfad zur Textdatei mit allen bereits extrahierten und manuell abgesegneten Kompetenzen
	static File competences = new File("information_extraction/data/competences/competences.txt");

	// Pfad zur Textdatei mit allen als keine Kompetenz ausgezeichneten Begriffen
	static File noCompetences = new File("information_extraction/data/competences/noCompetences.txt");

	// Pfad zur Kontext-Datei
	static File contextFile = new File("information_extraction/data/competences/competenceContexts.txt");

	//falls nicht alle Paragraphen aus der Input-DB verwendet werden sollen: hier Anzahl der zu lesenden Paragraphen festlegen
	// -1 = alle
	static int maxCount = -1;

	// falls nur eine bestimmte Anzahl gelesen werden soll, hier die startID angeben
	static int startPos = 0;


	
	

	/////////////////////////////
	// END
	/////////////////////////////

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

		// connect to input database
		Connection inputConnection = null;
		if (!new File(inputDB).exists()) {
			System.out.println("Database don't exists " + inputDB + "\nPlease change configuration and start again.");
			System.exit(0);
		} else {

			inputConnection = IE_DBConnector.connect(inputDB);
		}

		// check if count and startPos are valid
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

		Connection outputConnection = null;
		File outputfile = new File(outputDB);
		if (!outputfile.exists()) {
			outputfile.createNewFile();
			outputConnection = IE_DBConnector.connect(outputDB);
			IE_DBConnector.createOutputTable(outputConnection, IEType.COMPETENCE, true);
		} else {
			outputConnection = IE_DBConnector.connect(outputDB);
		}

		Extractor extractor = new Extractor(outputConnection, competences, noCompetences, contextFile,
				IEType.COMPETENCE);

		Map<ExtractionUnit, Map<InformationEntity, List<Context>>> extractions = extractor.extract(startPos, maxCount,
				tableSize, inputConnection, outputConnection, similarityScore);
		
	}
}
