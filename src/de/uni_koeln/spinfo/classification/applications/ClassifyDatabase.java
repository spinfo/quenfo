package de.uni_koeln.spinfo.classification.applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.db_io.DbConnector;
import de.uni_koeln.spinfo.classification.jasc.workflow.ConfigurableDatabaseClassifier;

/**
 * 
 * @author avogt, geduldia
 * 
 *         the main application to classify a data-base
 *
 */
public class ClassifyDatabase {

	/////////////////////////////
	// APP-CONFIGURATION
	/////////////////////////////
	
	// Path to input database
	static String inputDB = /*"D:/Daten/sqlite/SteA.db3"; */"classification/db/bibbDB.db"; 

	// path to the output folder for classified paragraphs
	static String outputFolder = /*"D:/Daten/sqlite/";*/ "C:/sqlite/";

	// Path to the trainingdata-file
	static String trainingdataFile = "classification/data/trainingSets/mergedTD.csv";

	// path to the training-database annotated by BIBB
	static String trainigDatabase = outputFolder + "TrainingData.db";

	// use training-database 
	static boolean trainWithDB = false;

	// use training-file
	static boolean trainWithFile = true;

	// correctable output database 
	static String corrOutputDB = "CorrectableParagraphs.db";

	//original output database
	static String origOutputDB = "OriginalParagraphs.db";

	// overall results fetched from db, no limit: -1
	static int queryLimit = -1;

	// start query from entry with id larger than startId (if there is a query limit)
	static int startId = 0;

	// number of results fetched in one step
	static int fetchSize = 100;

	/////////////////////////////
	// END
	/////////////////////////////

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

		// check if at least one trainingdata option is selected
		if (trainWithDB == false && trainWithFile == false) {
			System.out.println(
					"\nAt least one of the trainingdata options must be selected. \nPlease change configuration and start again");
			System.exit(0);
		}

		// Connect to input database
		Connection inputConnection = null;
		if (!new File(inputDB).exists()) {
			System.out.println(
					"Database '" + inputDB + "' don't exists \nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = DbConnector.connect(inputDB);
		}

		// Connect to output database (and training database)
		Connection corrConnection = null;
		Connection origConnection = null;
		Connection trainingConnection = null;
		File corrDBFile = new File(outputFolder + corrOutputDB);
		File origDBFile = new File(outputFolder + origOutputDB);
		File trainingDatabseFile = new File(trainigDatabase);

		// if outputdatabase already exists
		if (corrDBFile.exists()) {
			corrConnection = DbConnector.connect(outputFolder + corrOutputDB);

			// connect to/create trainingDatabase and transfer trainingdata from
			// classes_correctable to trainingDatabase
			if (!trainingDatabseFile.exists()) {
				trainingConnection = DbConnector.connect(trainigDatabase);
				DbConnector.createTrainingDataTable(trainingConnection);
			} else {
				trainingConnection = DbConnector.connect(trainigDatabase);
			}
			Map<ClassifyUnit, int[]> td = DbConnector.getTrainingDataFromClassesCorrectable(corrConnection);
			corrConnection.close();
			if (td != null) {
				System.out.println(
						"copy " + td.size() + " training paragraphs from Classes_Correctable into training database");
				DbConnector.updateTrainingData(trainingConnection, td);
			}

			// use or override current outputDatabase
			System.out.println("\noutput-database  already exists. "
					+ "\n - press 'o' to overwrite it (deletes all prior entries - annotated Trainingsparagraphs are saved in Trainingdatabase)"
					+ "\n - press 'u' to use it (adds and replaces entries)"
					+ "\n - press 'c' to create a new Output-Database");
			boolean answered = false;
			while (!answered) {
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				String answer = in.readLine();
				if (answer.toLowerCase().trim().equals("o")) {
					corrConnection = DbConnector.connect(outputFolder + corrOutputDB);
					DbConnector.createClassificationOutputTables(corrConnection, true);
					origConnection = DbConnector.connect(outputFolder + origOutputDB);
					DbConnector.createClassificationOutputTables(origConnection, false);
					answered = true;
				} else if (answer.toLowerCase().trim().equals("u")) {
					corrConnection = DbConnector.connect(outputFolder + corrOutputDB);
					origConnection = DbConnector.connect(outputFolder + origOutputDB);
					if (!origDBFile.exists()) {
						DbConnector.createClassificationOutputTables(origConnection, false);
					}
					answered = true;
				} else if (answer.toLowerCase().trim().equals("c")) {
					System.out.println("Please enter the name of the new correctable Database. It will be stored in "
							+ outputFolder);
					BufferedReader ndIn = new BufferedReader(new InputStreamReader(System.in));
					corrOutputDB = ndIn.readLine();
					corrConnection = DbConnector.connect(outputFolder + corrOutputDB);
					DbConnector.createClassificationOutputTables(corrConnection, true);
					System.out.println("Please enter the name of the new original Database. It will be stored in "
							+ outputFolder);
					ndIn = new BufferedReader(new InputStreamReader(System.in));
					origOutputDB = ndIn.readLine();
					origConnection = DbConnector.connect(outputFolder + origOutputDB);
					DbConnector.createClassificationOutputTables(origConnection, false);
					answered = true;
				} else {
					System.out.println("C: invalid answer! please try again...");
					System.out.println();
				}
			}
		}

		// if output database does not exist
		else {

			// connect to trainingDatabase
			if (trainingDatabseFile.exists() && trainWithDB) {
				trainingConnection = DbConnector.connect(trainigDatabase);
			}
			if (!trainingDatabseFile.exists() && trainWithDB) {
				System.out.println("\nthe database " + trainigDatabase
						+ " you want to use as trainingdata does not exist. \nPlease set 'trainWithDB' to 'false' or change database path");
				System.exit(0);
			}

			// create output-database
			corrConnection = DbConnector.connect(outputFolder + corrOutputDB);
			DbConnector.createClassificationOutputTables(corrConnection, true);
			origConnection = DbConnector.connect(outputFolder + origOutputDB);
			DbConnector.createClassificationOutputTables(origConnection, false);
		}

		// create output-directory if not exists
		if (!new File("classification/output").exists()) {
			new File("classification/output").mkdirs();
		}

		// start classifying
		ConfigurableDatabaseClassifier dbClassfy = new ConfigurableDatabaseClassifier(inputConnection, corrConnection,
				origConnection, trainingConnection, queryLimit, fetchSize,

				startId, trainWithDB, trainWithFile, trainingdataFile);
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("\nstart classification....\n\n");
			sb.append("\ninput DB: " + inputDB.substring(inputDB.lastIndexOf("/") + 1));
			sb.append("\ncorrectable output DB: " + corrDBFile.getName());
			sb.append("\noriginal output DB: " + origDBFile.getName());
			sb.append("\nused Trainingdata: ");
			if (trainWithDB) {
				sb.append(trainingDatabseFile.getName() + "    ");
			}
			if (trainWithFile) {
				sb.append(trainingdataFile.substring(trainingdataFile.lastIndexOf("/") + 1));
			}
			sb.append("\n\n");
			dbClassfy.classify(sb);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
