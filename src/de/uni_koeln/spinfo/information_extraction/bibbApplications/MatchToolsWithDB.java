package de.uni_koeln.spinfo.information_extraction.bibbApplications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import de.uni_koeln.spinfo.information_extraction.data.IEType;
import de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import de.uni_koeln.spinfo.information_extraction.workflow.Extractor_or;

public class MatchToolsWithDB {

	///////////////////////////
	// APP-CONFIGURATION
	/////////////////////////////

	// path to input database
	static String inputDB =    "D:/Daten/sqlite/CorrectableParagraphs.db"; //"C:/sqlite/CorrectableParagraphs.db"; //

	// folder for output database
	static String outputFolder = "D:/Daten/sqlite/"; // "C:/sqlite/"; //

	// name of output database
	static String outputDB = "Tools.db";

	// path to the tools-file
	static File tools = new File("information_extraction/data/tools/tools.txt");

	// path to the statistics file
	static File statisticsFile = new File("information_extraction/data/tools/toolStatistics.txt");

	// path to the context file
	static File contextFile = new File("information_extraction/data/tools/toolContexts.txt");

	// path to the negative examples file
	static File noTools = new File("information_extraction/data/tools/noTools.txt");
	
	//path to the correctable output-database  (to read the latest annotated tools)
	//static String correctableToolsDB = /*"D:/Daten/sqlite/CorrectableTools.db"; */ "C:/sqlite/CorrectableTools.db"; //


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
		
//		File correctableTools = new File(correctableToolsDB);
//		Connection correctableConnection = null;
//		if(correctableTools.exists()){
//			
//			correctableConnection = IE_DBConnector.connect(correctableToolsDB);
//		}

		// start matching
		Extractor_or extractor = new Extractor_or(null, tools, noTools, contextFile, IEType.TOOL);
		extractor.finalStringMatch(statisticsFile, inputConnection, outputConnection);
		

	}

}
