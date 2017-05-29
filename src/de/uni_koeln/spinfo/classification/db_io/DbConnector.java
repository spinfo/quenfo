package de.uni_koeln.spinfo.classification.db_io;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;

public class DbConnector {

	public static Connection connect(String dbFilePath) throws SQLException, ClassNotFoundException {
		Connection connection;
		// register the driver
		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
		String url = connection.getMetaData().getURL();
		System.out.println("Database " +url.substring(url.lastIndexOf("/")+1, url.lastIndexOf(".db"))+ " successfully opened");
		return connection;
	}

	public static void createBIBBDB(Connection connection) throws SQLException {
		System.out.println("create inputDB");
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		String sql = "DROP TABLE IF EXISTS DL_ALL_Spinfo";
		stmt.executeUpdate(sql);
		sql = "CREATE TABLE DL_ALL_Spinfo (ID  INTEGER PRIMARY KEY AUTOINCREMENT, ZEILENNR INT NOT NULL, Jahrgang INT NOT NULL, STELLENBESCHREIBUNG TEXT)";
		stmt.executeUpdate(sql);
		stmt.close();
		connection.commit();
	}

	public static void createClassificationOutputTables(Connection connection, boolean correctable)
			throws SQLException {

		StringBuffer sql;
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		sql = new StringBuffer("DROP TABLE IF EXISTS ClassifiedParagraphs");
		stmt.executeUpdate(sql.toString());
		sql = new StringBuffer("CREATE TABLE ClassifiedParagraphs" + "(ID INTEGER PRIMARY KEY AUTOINCREMENT , "
				+ " Text TEXT, " + " Jahrgang 	INT		NOT NULL, " + " ZEILENNR	INT		NOT	NULL, "
				+ " ClassONE   	INT     NOT NULL, " + " ClassTWO    INT    	NOT NULL, "
				+ " ClassTHREE  INT    	NOT NULL, " + " ClassFOUR  	INT    	NOT NULL");
		if (correctable) {
			sql.append(", UseForTraining	INT	NOT NULL)");
		} else {

			sql.append(")");
		}
		stmt.executeUpdate(sql.toString());

		stmt.close();
		connection.commit();
		System.out.println("Initialized new output-database.");

	}

	public static void createTrainingDataTable(Connection connection) throws SQLException {
		String sql;
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		sql = "DROP TABLE IF EXISTS TrainingData";
		stmt.executeUpdate(sql);
		sql = "CREATE TABLE trainingData ( " + " ID	INTEGER	PRIMARY	KEY	AUTOINCREMENT ," + " Jahrgang	INT	NOT	NULL,"
				+ " ZEILENNR INT NOT NULL, " + " Text TEXT, " + " ClassONE INT NOT NULL, " + " ClassTWO INT NOT NULL, "
				+ " ClassTHREE INT NOT NULL, " + " ClassFOUR INT NOT NULL,"
				+ "CONSTRAINT paragraph UNIQUE(Jahrgang, ZEILENNR, Text))";
		stmt.executeUpdate(sql);
		stmt.close();
		connection.commit();
		System.out.println("initialized new trainingData-Database");
	}

//	public static void createToolOutputTables(Connection connection) throws SQLException {
//		String sql;
//		connection.setAutoCommit(false);
//		Statement stmt = connection.createStatement();
//		sql = "DROP TABLE IF EXISTS Tools";
//		stmt.executeUpdate(sql);
//		sql = "CREATE TABLE Tools (ID INTEGER PRIMARY KEY AUTOINCREMENT, Jahrgang INT NOT NULL, Zeilennr INT NOT NULL, ParaID TEXT NOT NULL, Tool TEXT NOT NULL)";
//		stmt.executeUpdate(sql);
//		stmt.close();
//		connection.commit();
//		System.out.println("initialized new output-database 'Tools'");
//	}

//	public static void createCompetenceOutputTables(Connection connection) throws SQLException {
//		String sql;
//		connection.setAutoCommit(false);
//		Statement stmt = connection.createStatement();
//		sql = "DROP TABLE IF EXISTS Competences";
//		stmt.executeUpdate(sql);
//		sql = "CREATE TABLE Competences (ID INTEGER PRIMARY KEY AUTOINCREMENT, Jahrgang INT NOT NULL, Zeilennr INT NOT NULL, ParaID TEXT NOT NULL, Sentence TEXT NOT NULL, Comp TEXT NOT NULL, Importance TEXT, Contexts INT)";
//		stmt.executeUpdate(sql);
//		stmt.close();
//		connection.commit();
//		System.out.println("initialized ne output-database 'Competences'");
//	}

//	public static void createTrainingCompetencesOutputTables(Connection connection) throws SQLException {
//		String sql;
//		connection.setAutoCommit(false);
//		Statement stmt = connection.createStatement();
//		sql = "DROP TABLE IF EXISTS Competences";
//		stmt.executeUpdate(sql);
//		sql = "CREATE TABLE Competences (ID INTEGER PRIMARY KEY AUTOINCREMENT, Jahrgang INT NOT NULL, Zeilennr INT NOT NULL, ParaID TEXT NOT NULL, Sentence TEXT NOT NULL, Comp TEXT)";
//		stmt.executeUpdate(sql);
//		stmt.close();
//		connection.commit();
//		System.out.println("initialized ne output-database 'Competences'");
//	}

	public static boolean insertClassifiedParagraphsinDB(Connection outputConnection, List<ClassifyUnit> results,
			int jahrgang, int zeilennummer, boolean correctable) throws SQLException {

		boolean[] classIDs;
		// int txtTblID;

		try {
			outputConnection.setAutoCommit(false);

			Statement stmt = outputConnection.createStatement();
			// PreparedStatement prepTxtTbl = outputConnection.prepareStatement(
			// "INSERT INTO ClassifiedParaTexts
			// (ParaID,jahrgang,ZEILENNR,STELLENBESCHREIBUNG) VALUES(?,?,?,?)");
			// PreparedStatement prepClfyOrig =
			// outputConnection.prepareStatement(
			// "INSERT INTO Classes_Original
			// (TxtID,ClassONE,ClassTWO,ClassTHREE,ClassFOUR)
			// VALUES(?,?,?,?,?)");
			PreparedStatement prepClfyCorbl;
			if (correctable) {
				prepClfyCorbl = outputConnection.prepareStatement(
						"INSERT INTO ClassifiedParagraphs (Text, Jahrgang, ZEILENNR, ClassONE,ClassTWO,ClassTHREE,ClassFOUR, UseForTraining) VALUES(?,?,?,?,?,?,?,?)");
			} else {
				prepClfyCorbl = outputConnection.prepareStatement(
						"INSERT INTO ClassifiedParagraphs (Text, Jahrgang, ZEILENNR, ClassONE,ClassTWO,ClassTHREE,ClassFOUR) VALUES(?,?,?,?,?,?,?)");
			}

			for (ClassifyUnit cu : results) {

				// // Update ClassifiedParaTexts
				// prepTxtTbl.setString(1, cu.getID().toString());
				// prepTxtTbl.setInt(2, jahrgang);
				// prepTxtTbl.setInt(3, zeilennummer);
				// prepTxtTbl.setString(4, cu.getContent());
				// prepTxtTbl.executeUpdate();
				//
				// // get ID of last inserted row for use as a foreign key
				// ResultSet rs = stmt.executeQuery("SELECT
				// last_insert_rowid()");
				// rs.next();
				// txtTblID = rs.getInt(1);
				//
				int booleanRpl; // replaces true/false for saving into sqliteDB
				classIDs = ((ZoneClassifyUnit) cu).getClassIDs();
				//
				// // Update Classes_Original
				// prepClfyOrig.setInt(1, txtTblID);
				// for (int classID = 0; classID <= 3; classID++) {
				// if (classIDs[classID]) {
				// booleanRpl = 1;
				// } else {
				// booleanRpl = 0;
				// }
				// prepClfyOrig.setInt(2 + classID, booleanRpl);
				// }
				// prepClfyOrig.executeUpdate();

				// Update Classes_Correctable
				prepClfyCorbl.setString(1, cu.getContent());
				prepClfyCorbl.setInt(2, jahrgang);
				prepClfyCorbl.setInt(3, zeilennummer);
				for (int classID = 0; classID <= 3; classID++) {
					if (classIDs[classID]) {
						booleanRpl = 1;
					} else {
						booleanRpl = 0;
					}
					prepClfyCorbl.setInt(4 + classID, booleanRpl);
				}
				if (correctable) {
					prepClfyCorbl.setInt(8, 0);
				}
				prepClfyCorbl.executeUpdate();
			}

			// prepTxtTbl.close();
			// prepClfyOrig.close();
			prepClfyCorbl.close();
			stmt.close();
			outputConnection.commit();

			return true;

		} catch (SQLException e) {
			outputConnection.rollback();
			e.printStackTrace();
			return false;
		}

	}

	// writes ClassifyUnits (treated as job-Ads) in BIBB-DB
	public static void writeInBIBBDB(List<ClassifyUnit> input, Connection conn) throws SQLException {
		conn.setAutoCommit(false);
		PreparedStatement prep = conn
				.prepareStatement("INSERT INTO DL_ALL_Spinfo (ZEILENNR, Jahrgang, STELLENBESCHREIBUNG) VALUES(?,?,?)");
		for (ClassifyUnit classifyUnit : input) {
			prep.setInt(1, ((JASCClassifyUnit) classifyUnit).getSecondParentID());
			prep.setInt(2, ((JASCClassifyUnit) classifyUnit).getParentID());
			prep.setString(3, classifyUnit.getContent());
			prep.executeUpdate();
		}
		prep.close();
		conn.commit();
	}

//	public static void writeToolsInDB(ExtractionUnit cu, List<InformationEntity> tools, Connection outputConnection)
//			throws SQLException {
//		int jahrgang = cu.getJobAdID();
//		int zeilennr = cu.getSecondJobAdID();
//		String paraID = cu.getClassifyUnitID().toString();
//		PreparedStatement prepStmt = outputConnection
//				.prepareStatement("INSERT INTO Tools (Jahrgang, Zeilennr, ParaID, Tool) VALUES(" + jahrgang + ", "
//						+ zeilennr + ", '" + paraID + "',?)");
//		for (InformationEntity tool : tools) {
//			prepStmt.setString(1, tool.toString());
//			prepStmt.executeUpdate();
//		}
//		prepStmt.close();
//		outputConnection.commit();
//	}

//	public static void writeCompetencesInDB(ExtractionUnit extractionUnit, Map<InformationEntity,List<Context>> comps, Connection connection)
//			throws SQLException {
//		int jahrgang = extractionUnit.getJobAdID();
//		int zeilennr = extractionUnit.getSecondJobAdID();
//		String paraID = extractionUnit.getClassifyUnitID().toString();
//		connection.setAutoCommit(false);
//		PreparedStatement prepStmt = connection
//				.prepareStatement("INSERT INTO Competences (Jahrgang, Zeilennr, ParaID, Sentence, Comp, Importance, Contexts) VALUES(" + jahrgang + ", "
//						+ zeilennr + ", '" + paraID + "',?,?,?,?)");
//		for (InformationEntity comp : comps.keySet()) {
//			prepStmt.setString(1, extractionUnit.getSentence());
//			prepStmt.setString(2, comp.toString());
//			prepStmt.setString(3, comp.getImportance());
//			prepStmt.setInt(4, comps.get(comp).size());
//			prepStmt.executeUpdate();
//		}
//		prepStmt.close();
//		connection.commit();
//	}

//	public static void writeTrainingCompetencesInDB(ExtractionUnit extractionUnit, List<InformationEntity> comps,
//			Connection connection) throws SQLException {
//		int jahrgang = extractionUnit.getJobAdID();
//		int zeilennr = extractionUnit.getSecondJobAdID();
//		String paraID = extractionUnit.getClassifyUnitID().toString();
//		connection.setAutoCommit(false);
//		PreparedStatement prepStmt = connection
//				.prepareStatement("INSERT INTO Competences (Jahrgang, Zeilennr, ParaID, Sentence, Comp) VALUES("
//						+ jahrgang + ", " + zeilennr + ", '" + paraID + "',? " + ",?)");
//		if(comps.size()>0){
//			for (InformationEntity comp : comps) {
//				prepStmt.setString(1, extractionUnit.getSentence());
//				prepStmt.setString(2, comp.toString());
//				prepStmt.executeUpdate();
//			}
//		}
//		
//		else{
//			prepStmt.setString(1, extractionUnit.getSentence());
//			prepStmt.setString(2, null);
//			prepStmt.executeUpdate();
//		}
//		prepStmt.close();
//		connection.commit();
//	}

	public static Map<ClassifyUnit, int[]> getTrainingDataFromClassesCorrectable(Connection connection)
			throws SQLException {
		Map<ClassifyUnit, int[]> toReturn = new HashMap<ClassifyUnit, int[]>();
		connection.setAutoCommit(false);
		DatabaseMetaData dbmd = connection.getMetaData();
		ResultSet tables = dbmd.getTables(null, null, "ClassifiedParagraphs", null);
		if (tables.next()) {
			String sql = "SELECT Jahrgang, ZEILENNR, Text, ClassONE, ClassTWO, ClassTHREE, ClassFOUR FROM ClassifiedParagraphs WHERE (UseForTraining = '1')";
			Statement stmt = connection.createStatement();
			ResultSet result = stmt.executeQuery(sql);
			ClassifyUnit cu;
			int[] classes;
			while (result.next()) {
				cu = new JASCClassifyUnit(result.getString(3), result.getInt(1), result.getInt(2));
				classes = new int[4];
				for (int i = 0; i < 4; i++) {
					classes[i] = result.getInt(4 + i);
				}
				toReturn.put(cu, classes);
			}
			stmt.close();
			connection.commit();
			return toReturn;
		}
		tables.close();
		connection.commit();
		return null;
	}

	public static void updateTrainingData(Connection connection, Map<ClassifyUnit, int[]> td) throws SQLException {
		connection.setAutoCommit(false);
		String sql = "INSERT OR REPLACE INTO Trainingdata (Jahrgang, ZEILENNR, Text, ClassONE, ClassTWO, ClassTHREE, ClassFOUR) VALUES (?,?,?,?,?,?,?)";
		PreparedStatement prepStmt = connection.prepareStatement(sql);
		for (ClassifyUnit cu : td.keySet()) {
			prepStmt.setInt(1, ((JASCClassifyUnit) cu).getParentID());
			prepStmt.setInt(2, ((JASCClassifyUnit) cu).getSecondParentID());
			prepStmt.setString(3, cu.getContent());
			prepStmt.setInt(4, td.get(cu)[0]);
			prepStmt.setInt(5, td.get(cu)[1]);
			prepStmt.setInt(6, td.get(cu)[2]);
			prepStmt.setInt(7, td.get(cu)[3]);
			prepStmt.executeUpdate();
		}
		prepStmt.close();
		connection.commit();
	}

//	public static Map<ExtractionUnit,List<String>> readCompetenceTrainingData(Connection connection) throws SQLException {
//		Map<ExtractionUnit,List<String>> toReturn = new HashMap<ExtractionUnit,List<String>>();
//		connection.setAutoCommit(false);
//
//		String sql = "SELECT Jahrgang, ZEILENNR, ParaID, Sentence, Comp FROM Competences";
//		Statement stmt = connection.createStatement();
//		ResultSet result = stmt.executeQuery(sql);
//		ExtractionUnit ieUnit;
//		while (result.next()) {
//			String anchor = result.getString(5);
//			ieUnit = new ExtractionUnit(result.getString(4));
//			ieUnit.setClassifyUnitID(UUID.fromString(result.getString(3)));
//			ieUnit.setJobAdID(result.getInt(1));
//			ieUnit.setSecondJobAdID(result.getInt(2));
//			List<String> anchors = toReturn.get(ieUnit);
//			if(anchors == null){
//				anchors = new ArrayList<String>();
//			}
//			if(anchor != null){
//				anchors.add(anchor);
//			}
//			toReturn.put(ieUnit, anchors);
//		}
//		stmt.close();
//		connection.commit();
//		return toReturn;
//	}

	// public static void createCompetenceOutputTables(Connection connection)
	// throws SQLException {
	// String sql;
	// connection.setAutoCommit(false);
	// Statement stmt = connection.createStatement();
	// sql = "DROP TABLE IF EXISTS ClassifiedCompetences";
	// stmt.executeUpdate(sql);
	// sql = "CREATE TABLE ClassifiedCompetences" + "(CompetenceID INTEGER
	// PRIMARY KEY AUTOINCREMENT,"
	// + "CompetenceText TEXT," + "Quality TEXT," + "Importance TEXT," + "Type
	// VARCHAR(2),"
	// + "AdID INT NOT NULL)";
	// stmt.executeUpdate(sql);
	// stmt.close();
	// connection.commit();
	// }

	// public static boolean insertCompetences(Connection connection,
	// List<Competence> toAdd) throws SQLException {
	// connection.setAutoCommit(false);
	// PreparedStatement prepComp = connection.prepareStatement(
	// "INSERT OR REPLACE INTO ClassifiedCompetences (CompetenceText, Quality,
	// Importance, Type, AdID) VALUES(?,?,?,?,?)");
	// for (Competence competence : toAdd) {
	// prepComp.setString(1, competence.getCompetence());
	// prepComp.setString(2, competence.getQuality());
	// prepComp.setString(3, competence.getImportance());
	// if (competence.getType() != null) {
	// prepComp.setString(4, competence.getType().toString());
	// }
	// prepComp.setInt(5, competence.getJobAdID());
	// prepComp.executeUpdate();
	// System.out.println("insert: " + competence);
	// }
	// if (prepComp != null)
	// prepComp.close();
	// connection.commit();
	// return true;
	// }

	// public static void createClassifedCompetencesTable(Connection connection)
	// throws SQLException {
	// String sql;
	// connection.setAutoCommit(false);
	// Statement stmt = connection.createStatement();
	// sql = "DROP TABLE IF EXISTS ClassifiedCompetences";
	// stmt.executeUpdate(sql);
	//
	// sql = "CREATE TABLE ClassifiedCompetences" + "(CompetenceID INTEGER
	// PRIMARY KEY AUTOINCREMENT,"
	// + "CompetenceText TEXT," + "Quality TEXT," + "Importance TEXT," + "Type
	// VARCHAR(2),"
	// + "FOREIGN KEY(AdID) REFERENCES ClassifiedParaTexts(AdID))";
	// }
}
