package de.uni_koeln.spinfo.information_extraction.db_io;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.information_extraction.data.Context;
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.data.IEType;
import de.uni_koeln.spinfo.information_extraction.data.InformationEntity;

/**
 * @author geduldia
 * 
 * Connection to sql-databases (Creation, Input, Output)
 *
 */
public class IE_DBConnector {
	
	/**
	 * creates/connects to the given database/-path
	 * @param dbFilePath
	 * @return Connection
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public static Connection connect(String dbFilePath) throws SQLException, ClassNotFoundException {
		Connection connection;
		// register the driver
		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
		String url = connection.getMetaData().getURL();
		System.out.println("Database " + url.substring(url.lastIndexOf("/")+1, url.lastIndexOf(".db")) + " successfully opened");
		return connection;
	}

	/**
	 * Creates IE-outputTables in the given db and for the given IEType
	 * @param connection
	 * @param type 
	 * 			type of Information (e.g. competences or tools)
	 * @throws SQLException
	 */
	public static void createOutputTable(Connection connection, IEType type, boolean correctable) throws SQLException {
		String sql = null;
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		if(type == IEType.COMPETENCE){
			sql = "DROP TABLE IF EXISTS Competences";
		}
		if(type == IEType.TOOL){
			sql = "DROP TABLE IF EXISTS Tools";
		}
		stmt.executeUpdate(sql);
		if(correctable){
			if(type == IEType.COMPETENCE){
				sql = "CREATE TABLE Competences (ID INTEGER PRIMARY KEY AUTOINCREMENT, Jahrgang INT NOT NULL, Zeilennr INT NOT NULL, ParaID TEXT NOT NULL, Sentence TEXT NOT NULL, Comp TEXT, Importance TEXT, Contexts INT, ContextDescriptions TEXT NOT NULL, isCompetence INT NOT NULL, Notes TEXT)";
			}
			if(type == IEType.TOOL){
				sql = "CREATE TABLE Tools (ID INTEGER PRIMARY KEY AUTOINCREMENT, Jahrgang INT NOT NULL, Zeilennr INT NOT NULL, ParaID TEXT NOT NULL, Sentence TEXT NOT NULL, Tool TEXT NOT NULL, Contexts TEXT NOT NULL, ContextDescriptions TEXT NOT NULL, isTool INT NOT NULL, Notes TEXT)";
			}
		}
		else{
			if(type == IEType.COMPETENCE){
				sql = "CREATE TABLE Competences (ID INTEGER PRIMARY KEY AUTOINCREMENT, Jahrgang INT NOT NULL, Zeilennr INT NOT NULL, ParaID TEXT NOT NULL, Sentence TEXT NOT NULL, Comp TEXT, Importance TEXT, Contexts INT, ContextDescriptions TEXT NOT NULL)";
			}
			if(type == IEType.TOOL){
				sql = "CREATE TABLE Tools (ID INTEGER PRIMARY KEY AUTOINCREMENT, Jahrgang INT NOT NULL, Zeilennr INT NOT NULL, ParaID TEXT NOT NULL, Sentence TEXT NOT NULL, Tool TEXT NOT NULL, Contexts TEXT NOT NULL, ContextDescriptions TEXT NOT NULL)";
			}
		}
		stmt.executeUpdate(sql);
		stmt.close();
		connection.commit();
		String url = connection.getMetaData().getURL();
		System.out.println("created output-tables for IE in database '"+url.substring(url.lastIndexOf("/")+1, url.lastIndexOf(".db"))+"'");
	}

	/**
	 * selects ClassifyUnits of the correct claa for InformationExtraction
	 * Competences: class 3
	 * Tools: class 3 or class 2
	 * 
	 * 
	 * @param count 
	 * 			max number of read CUs
	 * @param startPos
	 * @param inputConnection
	 * @param type
	 * 			type of information
	 * @return a list of read ClassifyUnits
	 * @throws SQLException
	 */
	public static List<ClassifyUnit> getClassifyUnitsUnitsFromDB(int count, int startPos, Connection inputConnection, IEType type) throws SQLException {
		String query = null;
		if(type == IEType.COMPETENCE){
			query = "SELECT Jahrgang, ZEILENNR, Text, ClassTWO, ClassTHREE FROM ClassifiedParagraphs WHERE(ClassTHREE = '1') LIMIT ? OFFSET ?;";
		}
		if(type == IEType.TOOL){
			query = "SELECT Jahrgang, ZEILENNR, Text, ClassTWO, ClassTHREE FROM ClassifiedParagraphs WHERE(ClassTHREE = '1' OR ClassTWO = '1') LIMIT ? OFFSET ?;";
		}
		ResultSet result;
		PreparedStatement prepStmt = inputConnection.prepareStatement(query);
		prepStmt.setInt(1, count);
		prepStmt.setInt(2, startPos);
		result = prepStmt.executeQuery();
		List<ClassifyUnit> classifyUnits = new ArrayList<ClassifyUnit>();
		ClassifyUnit classifyUnit;
		while (result.next()) {
			int class2 = result.getInt(4);
			int class3 = result.getInt(5);
			int classID;
			if (class2 == 1) {
				if (class3 == 1) {
					classID = 6;
				} else {
					classID = 2;
				}
			} else {
				classID = 3;
			}
			classifyUnit = new JASCClassifyUnit(result.getString(3), result.getInt(1), result.getInt(2));
			((ZoneClassifyUnit) classifyUnit).setActualClassID(classID);
			classifyUnits.add(classifyUnit);
		}
		return classifyUnits;
	}
	
	/**
	 * writes extracted Competences in the output-database
	 * @param extractions
	 * 			extracted competences
	 * @param connection
	 * @throws SQLException
	 */
	public static void writeCompetences(Map<ExtractionUnit,Map<InformationEntity,List<Context>>> extractions, Connection connection, boolean correctable) throws SQLException{
		for (ExtractionUnit extractionUnit : extractions.keySet()) {
			Map<InformationEntity,List<Context>> ies = extractions.get(extractionUnit);
			int jahrgang = extractionUnit.getJobAdID();
			int zeilennr = extractionUnit.getSecondJobAdID();
			String paraID = extractionUnit.getClassifyUnitID().toString();
			connection.setAutoCommit(false);
			PreparedStatement prepStmt;
			if(correctable){
				prepStmt = connection
						.prepareStatement("INSERT INTO Competences (Jahrgang, Zeilennr, ParaID, Sentence, Comp, Importance, Contexts, ContextDescriptions, isCompetence) VALUES(" + jahrgang + ", "
								+ zeilennr + ", '" + paraID + "',?,?,?,?,?,"+-1+")");
			}
			else{
				prepStmt = connection
						.prepareStatement("INSERT INTO Competences (Jahrgang, Zeilennr, ParaID, Sentence, Comp, Importance, Contexts, ContextDescriptions) VALUES(" + jahrgang + ", "
								+ zeilennr + ", '" + paraID + "',?,?,?,?,?)");
			}
			for (InformationEntity ie : ies.keySet()) {
				prepStmt.setString(1, extractionUnit.getSentence());
				prepStmt.setString(2, ie.toString());
				prepStmt.setString(3, ie.getImportance());
				prepStmt.setInt(4, ies.get(ie).size());
				if(!ies.get(ie).isEmpty()){
					StringBuffer sb = new StringBuffer();
					for (Context context : ies.get(ie)) {
						sb.append("["+context.getDescription()+"]  ");
					}
					prepStmt.setString(5,sb.toString());
				}
				else{
					prepStmt.setString(5, "StringMatch");
				}
				prepStmt.executeUpdate();
			}
			prepStmt.close();
			connection.commit();
		}
	}

	/**
	 * writes extracted tools in the output-database
	 * @param extractions
	 * 			extracted tools
	 * @param connection
	 * @throws SQLException
	 */
	public static void writeTools(Map<ExtractionUnit, Map<InformationEntity, List<Context>>> extractions,Connection connection, boolean correctable) throws SQLException {
		for (ExtractionUnit extractionUnit : extractions.keySet()) {
			Map<InformationEntity,List<Context>> ies = extractions.get(extractionUnit);
			int jahrgang = extractionUnit.getJobAdID();
			int zeilennr = extractionUnit.getSecondJobAdID();
			String paraID = extractionUnit.getClassifyUnitID().toString();
			connection.setAutoCommit(false);
			PreparedStatement  prepStmt;
			if(correctable){
				prepStmt = connection
						.prepareStatement("INSERT INTO Tools (Jahrgang, Zeilennr, ParaID, Sentence, Tool, Contexts, ContextDescriptions, isTool) VALUES(" + jahrgang + ", "
								+ zeilennr + ", '" + paraID + "',?,?,?,?,"+-1+")");
			}
			else{
				prepStmt = connection
						.prepareStatement("INSERT INTO Tools (Jahrgang, Zeilennr, ParaID, Sentence, Tool, Contexts, ContextDescriptions) VALUES(" + jahrgang + ", "
								+ zeilennr + ", '" + paraID + "',?,?,?,?"+")");
			}
			for (InformationEntity ie : ies.keySet()) {
				prepStmt.setString(1, extractionUnit.getSentence());
				prepStmt.setString(2, ie.toString());
				prepStmt.setInt(3, ies.get(ie).size());
				if(!ies.get(ie).isEmpty()){
					StringBuffer sb = new StringBuffer();
					for (Context context : ies.get(ie)) {
						sb.append("["+context.getDescription()+"]  ");
					}
					prepStmt.setString(4,sb.toString());
				}
				else{
					prepStmt.setString(4, "StringMatch");
				}
				prepStmt.executeUpdate();
			}
			prepStmt.close();
			connection.commit();
		}
	}

	/**
	 * writes annotated Trainingdata in the Trainingdatabase
	 * @param extractionUnit
	 * @param competences
	 * @param connection
	 * @throws SQLException
	 */
	public static void writeCompetenceTrainingData(ExtractionUnit extractionUnit, List<InformationEntity> competences,
			Connection connection) throws SQLException {
		int jahrgang = extractionUnit.getJobAdID();
		int zeilennr = extractionUnit.getSecondJobAdID();
		String paraID = extractionUnit.getClassifyUnitID().toString();
		connection.setAutoCommit(false);
		PreparedStatement prepStmt = connection
				.prepareStatement("INSERT INTO Competences (Jahrgang, Zeilennr, ParaID, Sentence, Comp) VALUES("
						+ jahrgang + ", " + zeilennr + ", '" + paraID + "',? " + ",?)");
		if(competences.size()>0){
			for (InformationEntity comp : competences) {
				prepStmt.setString(1, extractionUnit.getSentence());
				prepStmt.setString(2, comp.toString());
				prepStmt.executeUpdate();
			}
		}
		
		else{
			prepStmt.setString(1, extractionUnit.getSentence());
			prepStmt.setString(2, null);
			prepStmt.executeUpdate();
		}
		prepStmt.close();
		connection.commit();
	}

	/**
	 * reads annotated trainingdata from db
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	public static Map<ExtractionUnit, List<String>> readTrainingData(Connection connection) throws SQLException {
		Map<ExtractionUnit,List<String>> toReturn = new HashMap<ExtractionUnit,List<String>>();
		connection.setAutoCommit(false);

		String sql = "SELECT Jahrgang, ZEILENNR, ParaID, Sentence, Comp FROM Competences";
		Statement stmt = connection.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		ExtractionUnit ieUnit;
		while (result.next()) {
			String anchor = result.getString(5);
			ieUnit = new ExtractionUnit(result.getString(4));
			ieUnit.setClassifyUnitID(UUID.fromString(result.getString(3)));
			ieUnit.setJobAdID(result.getInt(1));
			ieUnit.setSecondJobAdID(result.getInt(2));
			List<String> anchors = toReturn.get(ieUnit);
			if(anchors == null){
				anchors = new ArrayList<String>();
			}
			if(anchor != null){
				anchors.add(anchor);
			}
			toReturn.put(ieUnit, anchors);
		}
		stmt.close();
		connection.commit();
		return toReturn;
	}
	
	public static Set<String> readAnnotatedEntities(Connection connection, int annotated, IEType type) throws SQLException{
	
		Set<String> toReturn = new TreeSet<String>();
		connection.setAutoCommit(false);
		String sql = null;
		if(type == IEType.COMPETENCE){
			if(annotated ==-1){
				sql = "SELECT Comp FROM Competences";
			}
			else{
				sql = "SELECT Comp FROM Competences WHERE(isCompetence = '"+annotated+"')";
			}
			
		}
		if(type == IEType.TOOL){
			if(annotated == -1){
				sql = "SELECT Tool FROM Tools";
			}
			else{
				sql = "SELECT Tool FROM Tools WHERE(isTool = '"+annotated+"')";
			}
			
		}
		
		Statement stmt = connection.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		while(result.next()){
			String comp = result.getString(1);
			toReturn.add(comp);
		}
		return toReturn;
	}
	
	public static Set<String> readEntities(Connection connection, IEType type) throws SQLException{
		return readAnnotatedEntities(connection, -1, type);
	}
	
	public static void createCooccurrenceTable(Connection connection) throws SQLException{
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		String sql = "DROP TABLE IF EXISTS Cooccurrences";
		stmt.executeUpdate(sql);
		sql = "CREATE TABLE Cooccurrences (ID INTEGER PRIMARY KEY AUTOINCREMENT, Competence TEXT NOT NULL,  Cooccurrence Text NOT NULL, Count INT NOT NULL)";
		stmt.executeUpdate(sql);
		stmt.close();
		connection.commit();
	}
	
	public static void writeCooccurrences(Connection connection, Map<String,double[]> vectors, List<String> compOrder) throws SQLException{
		connection.setAutoCommit(false);
		PreparedStatement stmt = connection.prepareStatement("INSERT INTO Cooccurrences (Competence, Cooccurrence, Count) VALUES(?,?,?)");
		for (String comp : vectors.keySet()) {
			int i = 0;
			for (String string : compOrder) {
				if(string.equals(comp)) {
					i++;
					continue;
				}
				if(vectors.get(comp)[i]!= 0.0){
					stmt.setString(1, comp);
					stmt.setString(2, string);
					stmt.setInt(3, (int) vectors.get(comp)[i]);
					stmt.executeUpdate();
				}
				i++;
			}
		}
		stmt.close();
		connection.commit();
	}
	
	public static void createCluserTable(Connection connection) throws SQLException{
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		String sql = "DROP TABLE IF EXISTS Clusters";
		stmt.executeUpdate(sql);
		sql = "CREATE TABLE Clusters (ID INTEGER PRIMARY KEY AUTOINCREMENT, Competence TEXT NOT NULL,  ClusterID INT NOT NULL)";
		stmt.executeUpdate(sql);
		stmt.close();
		connection.commit();
	}

	public static void writeClusterResult(int clusterID, String comp, Connection connection) throws SQLException {
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		String sql = "INSERT INTO Clusters (Competence, ClusterID) VALUES('"+comp+"', '"+clusterID+"')";
		stmt.executeUpdate(sql);
		stmt.close();
		connection.commit();
	}

}
