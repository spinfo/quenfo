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

	// path to input database
	static String inputDB = /*"D:/Daten/sqlite/ClassifiedParagraphs.db"; */"C:/sqlite/CorrectableParagraphs.db"; // 

	// folder for output database
	static String outputFolder = /*"D:/Daten/sqlite/";*/"C:/sqlite/"; // 

	// name of output database
	static String outputDB = "Competences.db";

	// path to the comps-file
	static File competences = new File("information_extraction/data/competences/competences.txt");

	// path to the importance terms file
	static File importanceTerms = new File("information_extraction/data/competences/importanceTerms.txt");

	// path to the statistics file
	static File statisticsFile = new File("information_extraction/data/competences/competenceStatistics.txt");

	// path to the context file
	static File contextFile = new File("information_extraction/data/competences/competenceContexts.txt");

	// path to the negative examples file
	static File noCompetences = new File("information_extraction/data/competences/noCompetences.txt");
	


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
		
//		File correctableComps = new File(correctableCompetencesDB);
//		Connection correctableConnection = null;
//		if(correctableComps.exists()){
//			
//			correctableConnection = IE_DBConnector.connect(correctableCompetencesDB);
//		}

		// start matching
		Extractor extractor = new Extractor(null, competences, noCompetences, contextFile, importanceTerms, IEType.COMPETENCE);
		extractor.finalStringMatch(statisticsFile, inputConnection, outputConnection);
	}

}
