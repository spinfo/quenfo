package quenfo.de.uni_koeln.spinfo.information_extraction.db_io;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.Pattern;

/**
 * @author geduldia
 * 
 * 
 *         includes all necessary DB-methods for the extraction- (and matching-)
 *         workflow
 *
 */
public class IE_DBConnector {

	/**
	 * creates/connects to the given database
	 * 
	 * @param dbFilePath
	 * @return Connection
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public static Connection connect(String dbFilePath) throws SQLException, ClassNotFoundException {
		Connection connection;
		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
		return connection;
	}

	/**
	 * Creates the Output-Tables for the extracted competences/tools
	 * 
	 * @param connection
	 * @param type       type of Information (competences or tools)
	 * @throws SQLException
	 */
	public static void createExtractionOutputTable(Connection connection, IEType type, boolean correctable)
			throws SQLException {
		String sql = null;
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		if (type == IEType.TOOL) {
			sql = "DROP TABLE IF EXISTS Tools";
		} else { // bei allen anderen Comptence-Fällen
			sql = "DROP TABLE IF EXISTS Competences";
		}
		
//		if (type == IEType.COMPETENCE_IN_3) {
//			sql = "DROP TABLE IF EXISTS Competences";
//		}
		
		stmt.executeUpdate(sql);
		if (correctable) {
			if (type == IEType.TOOL) {
				sql = "CREATE TABLE Tools (ID INTEGER PRIMARY KEY AUTOINCREMENT, Jahrgang INT NOT NULL, Zeilennr INT NOT NULL, ParaID TEXT NOT NULL, Sentence TEXT NOT NULL, Tool TEXT NOT NULL, Contexts TEXT NOT NULL, ContextDescriptions TEXT NOT NULL, isTool INT NOT NULL, Notes TEXT)";
			}
			else {
//			if (type == IEType.COMPETENCE_IN_3) {
				sql = "CREATE TABLE Competences (ID INTEGER PRIMARY KEY AUTOINCREMENT, Jahrgang INT NOT NULL, Zeilennr INT NOT NULL, ParaID TEXT NOT NULL, Sentence TEXT NOT NULL, Comp TEXT, Contexts INT, ContextDescriptions TEXT NOT NULL, isCompetence INT NOT NULL, Notes TEXT)";
			}
		} else {
			if (type == IEType.TOOL) {
				sql = "CREATE TABLE Tools (ID INTEGER PRIMARY KEY AUTOINCREMENT, Jahrgang INT NOT NULL, Zeilennr INT NOT NULL, ParaID TEXT NOT NULL, SentenceID TEXT NOT NULL, Lemmata TEXT NOT NULL ,Sentence TEXT NOT NULL, Tool TEXT NOT NULL)";

			} else {
//			if (type == IEType.COMPETENCE_IN_3) {
				sql = "CREATE TABLE Competences (ID INTEGER PRIMARY KEY AUTOINCREMENT, Jahrgang INT NOT NULL, Zeilennr INT NOT NULL, ParaID TEXT NOT NULL, SentenceID TEXT NOT NULL, Lemmata TEXT NOT NULL, Sentence TEXT NOT NULL, Label TEXT, Comp TEXT, Importance TEXT)";
			}
			
		}
		stmt.executeUpdate(sql);
		stmt.close();
		connection.commit();
	}

	/**
	 * returns ClassifyUnits of the correct class(es) for Information-Extraction
	 * Competences: class 3 Tools: class 3 or class 2
	 * 
	 * 
	 * @param count
	 * @param startPos
	 * @param connection
	 * @param type       type of information
	 * @return a list of read ClassifyUnits
	 * @throws SQLException
	 */
	public static List<ClassifyUnit> readClassifyUnits(int count, int startPos, Connection connection, IEType type)
			throws SQLException {

		connection.setAutoCommit(false);
		String query = null;
		if (type == IEType.COMPETENCE_IN_3) {
			query = "SELECT * FROM ClassifiedParagraphs WHERE(ClassTHREE = '1') LIMIT ? OFFSET ?;";
		} else if (type == IEType.COMPETENCE_IN_2) {
			query = "SELECT * FROM ClassifiedParagraphs WHERE(ClassTWO = '1') LIMIT ? OFFSET ?;";
		} else if (type == IEType.COMPETENCE_IN_23) {
			query = "SELECT * FROM ClassifiedParagraphs WHERE (ClassTWO = '1' OR ClassTHREE = '1') LIMIT ? OFFSET ?;";
		}
		else if (type == IEType.TOOL) {
			query = "SELECT * FROM ClassifiedParagraphs WHERE(ClassTHREE = '1' OR ClassTWO = '1') LIMIT ? OFFSET ?;";
		}
		
		ResultSet result;
		PreparedStatement prepStmt = connection.prepareStatement(query);
		prepStmt.setInt(1, count);
		prepStmt.setInt(2, startPos);
		prepStmt.setFetchSize(100); //TODO fetchSize
		result = prepStmt.executeQuery();
		List<ClassifyUnit> classifyUnits = new ArrayList<ClassifyUnit>();
		ClassifyUnit classifyUnit;
		while (result.next()) {
			int class2 = result.getInt("ClassTWO");
			int class3 = result.getInt("ClassTHREE");
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
			classifyUnit = new JASCClassifyUnit(result.getString("Text"), result.getInt("Jahrgang"),
					result.getInt("ZEILENNR"));
			((JASCClassifyUnit) classifyUnit).setTableID(result.getInt("ID"));
			((JASCClassifyUnit) classifyUnit).setActualClassID(classID);
			try {
				String sentences = result.getString("ExtractionUnits");
				// System.out.println(sentences);
				if (sentences != null && !(sentences.equals(""))) {
					((JASCClassifyUnit) classifyUnit)
							.setSentences((sentences.replace(" | ", " ").replace("<root> ", "")));
					((JASCClassifyUnit) classifyUnit).setTokens(sentences);
				}
			} catch (SQLException e) {
			}
			try {
				String lemmata = result.getString("Lemmata");
				if (lemmata != null && !lemmata.equals("")) {
					((JASCClassifyUnit) classifyUnit).setLemmata(lemmata);
				}

			} catch (SQLException e) {

			}
			try {
				String posTags = result.getString("POSTags");
				if (posTags != null && !posTags.equals("")) {
					((JASCClassifyUnit) classifyUnit).setPosTags(posTags);
				}

			} catch (SQLException e) {
			}
			classifyUnits.add(classifyUnit);
		}
		result.close();
		prepStmt.close();
		connection.commit();
		return classifyUnits;
	}

	/**
	 * writes the via pattern extracted or via string-matching found competences in
	 * the DB
	 * 
	 * @param extractions
	 * @param connection
	 * @param correctable
	 * @param gold
	 * @throws SQLException
	 */
	public static void writeCompetenceExtractions(
			Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extractions, Connection connection,
			boolean correctable) throws SQLException {
		Set<String> types = new HashSet<String>();
		connection.setAutoCommit(false);
		PreparedStatement prepStmt;
		if (correctable) {
			// für den Output der Extraktions-Workflows

			prepStmt = connection.prepareStatement(
					"INSERT INTO Competences (Jahrgang, Zeilennr, ParaID, Sentence, Comp, Contexts, ContextDescriptions, isCompetence) VALUES(?,?,?,?,?,?,?,-1)");
		} else {
			// Für den Output der Matching-Workflows
			prepStmt = connection.prepareStatement(
					"INSERT INTO Competences (Jahrgang, Zeilennr, ParaID, SentenceID, Lemmata, Sentence, Label, Comp,  Importance) VALUES(?,?,?,?,?,?,?,?,?)");
		}
		for (ExtractionUnit extractionUnit : extractions.keySet()) {
			Map<InformationEntity, List<Pattern>> ies = extractions.get(extractionUnit);
			int jahrgang = extractionUnit.getJobAdID();
			int zeilennr = extractionUnit.getSecondJobAdID();
			String paraID;
			try {
				paraID = extractionUnit.getClassifyUnitID().toString();
			} catch (Exception e) {
				paraID = extractionUnit.getClassifyUnitjpaID() + "";
			}
			String sentence = extractionUnit.getSentence();
			StringBuffer lemmata = null;
			if (!correctable) {
				lemmata = new StringBuffer();
				for (int i = 1; i < extractionUnit.getLemmata().length; i++) {
					String string = extractionUnit.getLemmata()[i];
					lemmata.append(string + " ");
				}
			}

			for (InformationEntity ie : ies.keySet()) {

				if (correctable) {
					// write only unique types
					String expression = ie.toString();
					if (types.contains(expression)) {
						PreparedStatement update = connection.prepareStatement(
								"UPDATE Competences SET Sentence = sentence || '  |  ' || ? WHERE Comp = ?");
						update.setString(1, sentence);
						update.setString(2, expression);
						update.executeUpdate();
						update.close();
						continue;
					}
					types.add(expression);
				}
				prepStmt.setInt(1, jahrgang);
				prepStmt.setInt(2, zeilennr);
				prepStmt.setString(3, paraID);

				if (correctable) {
					prepStmt.setString(4, sentence);
					prepStmt.setString(5, ie.toString());

					prepStmt.setInt(6, ies.get(ie).size()); // 6
					if (!ies.get(ie).isEmpty()) {
						StringBuffer sb = new StringBuffer();
						for (Pattern pattern : ies.get(ie)) {
							sb.append("[" + pattern.getDescription() + "]  ");
						}
						prepStmt.setString(7, sb.toString()); // 7
					} else {
						prepStmt.setString(7, "StringMatch"); // 7
					}
				} else {
					StringBuilder sb = new StringBuilder();
					String labels = "";
					if (ie.getLabels() != null) { // TODO warum getLabels == null ?
						for (String l : ie.getLabels())
							sb.append(l + "|");
						labels = sb.toString();
						labels = labels.substring(0, labels.length() - 1);
					}
					prepStmt.setString(4, extractionUnit.getSentenceID().toString());
					prepStmt.setString(5, lemmata.toString());
					prepStmt.setString(6, sentence);
					prepStmt.setString(7, labels);
					prepStmt.setString(8, ie.toString());
					prepStmt.setString(9, ie.getModifier());
				}
				prepStmt.addBatch();
			}
		}
		prepStmt.executeBatch();
		prepStmt.close();
		connection.commit();
	}

	/**
	 * writes the via pattern extracted or via string-matching found tools in the DB
	 * 
	 * @param extractions
	 * @param connection
	 * @param correctable
	 * @throws SQLException
	 */
	public static void writeToolExtractions(Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extractions,
			Connection connection, boolean correctable) throws SQLException {

		connection.setAutoCommit(false);
		PreparedStatement prepStmt;
		if (correctable) {
			prepStmt = connection.prepareStatement(
					"INSERT INTO Tools (Jahrgang, Zeilennr, ParaID, Sentence, Tool, Contexts, ContextDescriptions, isTool) VALUES(?,?,?,?,?,?,?,-1)");
		} else {
			prepStmt = connection.prepareStatement(
					"INSERT INTO Tools (Jahrgang, Zeilennr, ParaID, SentenceID, Lemmata, Sentence, Tool) VALUES(?,?,?,?,?,?,?)");
		}
		Set<String> types = new HashSet<String>();
		for (ExtractionUnit extractionUnit : extractions.keySet()) {
			Map<InformationEntity, List<Pattern>> ies = extractions.get(extractionUnit);
			int jahrgang = extractionUnit.getJobAdID();
			int zeilennr = extractionUnit.getSecondJobAdID();
			String paraID = extractionUnit.getClassifyUnitID().toString();
			String sentence = extractionUnit.getSentence();
			StringBuffer lemmata = null;
			if (!correctable) {
				lemmata = new StringBuffer();
				for (int i = 1; i < extractionUnit.getLemmata().length; i++) {
					String string = extractionUnit.getLemmata()[i];
					lemmata.append(string + " ");
				}
			}

			for (InformationEntity ie : ies.keySet()) {

				if (correctable) {
					// write only unique types
					String expression = ie.toString();
					if (types.contains(expression)) {
						PreparedStatement update = connection.prepareStatement(
								"UPDATE Tools SET Sentence = sentence || '  |  ' || ? WHERE Tool = ?");
						update.setString(1, sentence);
						update.setString(2, expression);
						update.executeUpdate();
						update.close();
						continue;
					}
					types.add(expression);
				}

				prepStmt.setInt(1, jahrgang);
				prepStmt.setInt(2, zeilennr);
				prepStmt.setString(3, paraID);
				if (correctable) {
					prepStmt.setString(4, sentence);
					prepStmt.setString(5, ie.toString());
					prepStmt.setInt(6, ies.get(ie).size()); // 6
					if (!ies.get(ie).isEmpty()) {
						StringBuffer sb = new StringBuffer();
						for (Pattern pattern : ies.get(ie)) {
							sb.append("[" + pattern.getDescription() + "]  ");
						}
						prepStmt.setString(7, sb.toString()); // 7
					} else {
						prepStmt.setString(3, "StringMatch");
					}
				} else {
					prepStmt.setString(4, extractionUnit.getSentenceID().toString());
					prepStmt.setString(5, lemmata.toString());
					prepStmt.setString(6, sentence);
					prepStmt.setString(7, ie.toString());
				}
				prepStmt.addBatch();
			}
		}
		prepStmt.executeBatch();
		prepStmt.close();
		connection.commit();
	}

	/**
	 * reads manually annotated competences/tools from the DB
	 * 
	 * @param connection
	 * @param annotated
	 * @param type
	 * @return set of competence-/tool- Strings
	 * @throws SQLException
	 */
	public static Set<String> readAnnotatedEntities(Connection connection, int annotated, IEType type)
			throws SQLException {

		Set<String> toReturn = new TreeSet<String>();
		connection.setAutoCommit(false);
		String sql = null;
		if (type == IEType.TOOL) {
			if (annotated == -1) {
				sql = "SELECT Tool FROM Tools";
			} else {
				sql = "SELECT Tool FROM Tools WHERE(isTool = '" + annotated + "')";
			}

		} else {
//		if (type == IEType.COMPETENCE_IN_3) {
			if (annotated == -1) {
				sql = "SELECT Comp FROM Competences";
			} else {
				sql = "SELECT Comp FROM Competences WHERE(isCompetence = '" + annotated + "')";
			}

		}
		
		Statement stmt = connection.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		while (result.next()) {
			String comp = result.getString(1);
			toReturn.add(comp);
		}
		stmt.close();
		connection.commit();
		return toReturn;
	}

	/**
	 * reads extracted competences/tools from the DB
	 * 
	 * @param connection
	 * @param type
	 * @return Set of competence-/tool- Strings
	 * @throws SQLException
	 */
	public static Set<String> readEntities(Connection connection, IEType type) throws SQLException {
		return readAnnotatedEntities(connection, -1, type);
	}

	/**
	 * adds lexical data (sentences, lemmata, posTags) to the ClassifyUnits for
	 * later reuse
	 * 
	 * @param connection
	 * @param extractionUnits
	 * @throws SQLException
	 */
	public static void upateClassifyUnits(Connection connection, List<ExtractionUnit> extractionUnits)
			throws SQLException {
		connection.setAutoCommit(false);
		String sql = "UPDATE ClassifiedParagraphs SET ExtractionUnits = ?, Lemmata = ?, POSTags = ? WHERE ID = ?";
		PreparedStatement stmt = connection.prepareStatement(sql);
		Map<Integer, StringBuffer> sentences = new HashMap<Integer, StringBuffer>();
		Map<Integer, StringBuffer> lemmata = new HashMap<Integer, StringBuffer>();
		Map<Integer, StringBuffer> posTags = new HashMap<Integer, StringBuffer>();
		for (ExtractionUnit e : extractionUnits) {
			if (e.isLexicalDataIsStoredInDB())
				continue;
			int cuID = e.getClassifyUnitTableID();
			StringBuffer sb = sentences.get(cuID);
			if (sb == null)
				sb = new StringBuffer();
			else
				sb.append("  ||  ");
			for (String token : e.getTokens()) {
				sb.append(token + " | ");
			}
			sb.delete(sb.length() - 3, sb.length());
			// sb.append(e.getSentence());
			sentences.put(cuID, sb);

			sb = lemmata.get(cuID);
			if (sb == null)
				sb = new StringBuffer();
			else
				sb.append("  ||  ");
			for (String lemma : e.getLemmata()) {
				sb.append(lemma + " | ");
			}
			sb.delete(sb.length() - 3, sb.length());
			lemmata.put(cuID, sb);

			sb = posTags.get(cuID);
			if (sb == null)
				sb = new StringBuffer();
			else
				sb.append("  ||  ");
			for (String pos : e.getPosTags()) {
				sb.append(pos + " | ");
			}
			sb.delete(sb.length() - 3, sb.length());
			posTags.put(cuID, sb);
		}
		for (Integer id : sentences.keySet()) {
			stmt.setString(1, sentences.get(id).toString());
			stmt.setString(2, lemmata.get(id).toString());
			stmt.setString(3, posTags.get(id).toString());
			stmt.setInt(4, id);
			stmt.addBatch();
		}
		stmt.executeBatch();
		stmt.close();
		connection.commit();
	}

	/**
	 * creates an index on the given columns in thegiven table
	 * 
	 * @param connection
	 * @param table
	 * @param columns
	 * @throws SQLException
	 */
	public static void createIndex(Connection connection, String table, String columns) throws SQLException {
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		String sql = "CREATE INDEX classIndex ON " + table + " (" + columns + ")";
		try {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			// Index exisitiert bereits
		}
		stmt.close();
		connection.commit();
	}

}
