package quenfo.de.uni_koeln.spinfo.classification.db_io;

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
import java.util.SortedMap;

import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.data.ZoneClassifyUnit;

public class Class_DBConnector {

	public static Connection connect(String dbFilePath) throws SQLException, ClassNotFoundException {
		Connection connection;
		// register the driver
		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
		String url = connection.getMetaData().getURL();
		return connection;
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
				+ " ClassTHREE  INT    	NOT NULL, " + " ClassFOUR  	INT    	NOT NULL)");
		// if (correctable) {
		// sql.append(", UseForTraining INT NOT NULL)");
		// } else {
		//
		// sql.append(")");
		// }
		stmt.executeUpdate(sql.toString());
		stmt.close();
		connection.commit();

	}

	public static void addColumn(Connection connection, String column, String table)
			throws SQLException {
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		try{
			stmt.executeUpdate("ALTER TABLE "+table+" ADD " + column + " TEXT");
			stmt.close();
			connection.commit();
		}
		catch(SQLException e){
			//Spalte exisitiert bereits
		}
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


	public static boolean insertClassifiedParagraphsinDB(Connection connection, List<ClassifyUnit> results,
			int jahrgang, int zeilennummer, boolean correctable) throws SQLException {
		boolean[] classIDs;
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			PreparedStatement prepStmt;
			prepStmt = connection.prepareStatement(
					"INSERT INTO ClassifiedParagraphs (Text, Jahrgang, ZEILENNR, ClassONE,ClassTWO,ClassTHREE,ClassFOUR) VALUES(?,?,?,?,?,?,?)");
			for (ClassifyUnit cu : results) {
				int booleanRpl; // replaces true/false for saving into sqliteDB
				classIDs = ((ZoneClassifyUnit) cu).getClassIDs();
				prepStmt.setString(1, cu.getContent());
				prepStmt.setInt(2, jahrgang);
				prepStmt.setInt(3, zeilennummer);
				for (int classID = 0; classID <= 3; classID++) {
					if (classIDs[classID]) {
						booleanRpl = 1;
					} else {
						booleanRpl = 0;
					}
					prepStmt.setInt(4 + classID, booleanRpl);
				}
				prepStmt.addBatch();
			}
			prepStmt.executeBatch();
			prepStmt.close();
			stmt.close();
			connection.commit();
			return true;
		} catch (SQLException e) {
			System.out.println("\nFehler beim Schreiben: \n");
//			boolean printIds = true;;
			for (ClassifyUnit cu : results) {
//				if(printIds){
//					System.out.println("Zeilennr & Jahrgang: " + ((JASCClassifyUnit) cu).getParentID()+" "+ ((JASCClassifyUnit) cu).getSecondParentID()+"\n");
//					printIds = false;
//				}
				
				System.out.println(cu.getContent());
			}
			System.out.println("\n\n");
			//outputConnection.rollback();
			e.printStackTrace();
			return false;
		}

	}


	//Nicht mehr in Gebrauch
	@Deprecated
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

	//Nicht mehr in Gebrauch
	@Deprecated
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


	public static void writeInputDB(SortedMap<Integer, String> jobAds,Connection connection) throws SQLException {
		connection.setAutoCommit(false);
		String sql = "INSERT INTO DL_ALL_Spinfo (ZEILENNR, Jahrgang, STELLENBESCHREIBUNG) VALUES(?,2011,?)";
		PreparedStatement stmt = connection.prepareStatement(sql);
		for (int i : jobAds.keySet()) {
			stmt.setInt(1, i);
			stmt.setString(2, jobAds.get(i));
			stmt.executeUpdate();
		}
		stmt.close();
		connection.commit();
	}
}
