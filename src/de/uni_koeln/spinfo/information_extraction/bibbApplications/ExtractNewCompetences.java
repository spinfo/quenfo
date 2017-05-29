package de.uni_koeln.spinfo.information_extraction.bibbApplications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import de.uni_koeln.spinfo.information_extraction.data.IEType;
import de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import de.uni_koeln.spinfo.information_extraction.workflow.Extractor_or;

public class ExtractNewCompetences {

/////////////////////////////
// APP-CONFIGURATION
/////////////////////////////

// path to the input database
static String inputDB =  /*"D:/Daten/sqlite/ClassifiedParagraphs.db"; */"C:/sqlite/CorrectableParagraphs.db"; 

// path to the competences-file (file of already known competences)
static File competences = new File("information_extraction/data/competences/competences.txt");

// path to the noCompetences-file (file of known typical mistakes)
static File noCompetences = new File("information_extraction/data/competences/noCompetences.txt");

// path to the importanceTerms-file
static File importanceTerms = new File("information_extraction/data/competences/importanceTerms.txt");

// path to the Context-file
static File contextFile = new File("information_extraction/data/competences/competenceContexts.txt");

//path to the (new) output-database
static String outputDB = /*"D:/Daten/sqlite/CorrectableCompetences.db";*/ "C:/sqlite/CorrectableCompetences.db"; 

// first paragraph read from input database
static int startPos = 0;

// max number of read paragraphs
static int maxCount = -1;


/////////////////////////////
// END
/////////////////////////////

public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {


	// connect to input database
	Connection inputConnection = null;
	if (!new File(inputDB).exists()) {
		System.out
				.println("Database don't exists " + inputDB + "\nPlease change configuration and start again.");
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
	if(!outputfile.exists()){
		outputfile.createNewFile();
		outputConnection = IE_DBConnector.connect(outputDB);
		IE_DBConnector.createOutputTable(outputConnection, IEType.COMPETENCE, true);
	}
	else{
		outputConnection = IE_DBConnector.connect(outputDB);
	}

	
	Extractor_or extractor = new Extractor_or(outputConnection, competences, noCompetences, contextFile, importanceTerms, IEType.COMPETENCE);	
	//extractor.extract(startPos, maxCount, tableSize, inputConnection, potentialCompetences, potentialCompetencesWithContext);
	extractor.extract(startPos, maxCount, tableSize, inputConnection, outputConnection);
}

}
