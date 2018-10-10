package quenfo;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Test;

import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.workflow.CoordinationEvaluator;

public class EvaluateCooResolvation {

	////Diese Variablen müssen entsprechen nach Tool oder Competence angepasst werden
	
	//Extraktionsthema (Tool oder Competence)
	static IEType type = IEType.COMPETENCE;
	
	// Pfad zur Input-DB mit den klassifizierten Paragraphen
	static String inputDB = "src/test/resources/coordinations/CorrectableCompetences_2011_Gold.db";

	// Name der Output-DB
	static String outputDB = "Coordinations_Competences_2011.db";		
	
	////ENDE ANPASSUNG
	
	// Output-Ordner
	static String outputFolder = "src/test/resources/coordinations/";

	// Anzahl der Paragraphen aus der Input-DB, gegen die gematcht werden soll
	// (-1 = alle)
	static int maxCount = -1;

	// Falls nicht alle Paragraphen gematcht werden sollen, hier die
	// Startposition angeben
	static int startPos = 0;
	
	
	@Test
	public void test() throws ClassNotFoundException, SQLException, IOException {
		// Verbindung mit Input-DB
		Connection inputConnection = null;
		if (!new File(inputDB).exists()) {
			System.out
					.println("Database don't exists " + inputDB + "\nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = IE_DBConnector.connect(inputDB);
		}
		String query = null;
		// Prüfe ob maxCount und startPos gültige Werte haben
		if (type.equals(IEType.COMPETENCE))
			query = "SELECT COUNT(*) FROM Competences;";
		else
			query = "SELECT COUNT(*) FROM Tools;";
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

		long before = System.currentTimeMillis();
		if(type.equals(IEType.COMPETENCE))
			IE_DBConnector.createIndex(inputConnection, "Competences", "ID"); 
		else
			IE_DBConnector.createIndex(inputConnection, "Tools", "ID"); 
		CoordinationEvaluator ce = new CoordinationEvaluator();
		ce.evaluate(type, inputConnection, outputConnection, startPos, maxCount);
		
		long after = System.currentTimeMillis();
		double time = (((double) after - before) / 1000) / 60;
		if (time > 60.0) {
			System.out.println("\nfinished evaluation in " + (time / 60) + " hours");
		} else {
			System.out.println("\nfinished evaluation in " + time + " minutes");
		}
	}

}
