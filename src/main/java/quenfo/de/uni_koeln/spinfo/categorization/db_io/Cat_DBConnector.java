package quenfo.de.uni_koeln.spinfo.categorization.db_io;

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

import quenfo.de.uni_koeln.spinfo.categorization.data.Category;
import quenfo.de.uni_koeln.spinfo.categorization.data.CompetenceCategory;
import quenfo.de.uni_koeln.spinfo.categorization.data.Entity;
import quenfo.de.uni_koeln.spinfo.categorization.data.Pair;
import quenfo.de.uni_koeln.spinfo.categorization.data.Sentence;
import quenfo.de.uni_koeln.spinfo.categorization.data.ToolCategory;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;

/**
 * @author geduldia
 *
 */
public class Cat_DBConnector {

	public static Connection connect(String dbFilePath) throws SQLException, ClassNotFoundException {
		Connection connection;
		// register the driver
		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
		return connection;
	}

	/**
	 * reads all matching-results from the given DB
	 * 
	 * @param connection
	 * @param categories map of categorized entities and their categories
	 * @param type
	 * @param validated
	 * @return map of competences/tools (as key) and a list their containing
	 *         sentences (as values)
	 * @throws SQLException
	 */
	public static Map<Entity, Set<Sentence>> getSentencesByEntity(Connection connection,
			Map<Entity, Set<Category>> categories, IEType type, boolean validated, boolean trimSentences)
			throws SQLException {
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		String sql = null;
		String sentenceColumns = "SentenceID";
		if (trimSentences) {
			sentenceColumns = "SentenceID, lemmata";
		}
		if (type == IEType.TOOL) {
			sql = "SELECT Tool, " + sentenceColumns + " FROM Tools";
		}
		else {
//		if (type == IEType.COMPETENCE_IN_3) {
			sql = "SELECT Comp, " + sentenceColumns + " FROM Competences";
		}
		
		ResultSet result = stmt.executeQuery(sql);
		Entity entity;
		String id;
		String lemmata;
		Map<Entity, Set<Sentence>> toReturn = new HashMap<Entity, Set<Sentence>>();
		while (result.next()) {
			entity = new Entity(result.getString(1));
			entity.setValidated(validated);
			if (categories != null && categories.keySet().contains(entity)) {
				entity.setCategories(categories.get(entity));
			}
			id = result.getString(2);
			Sentence sentence = new Sentence(id);
			if (trimSentences) {
				lemmata = result.getString(3);
				sentence.setLemmata(lemmata);
			}

			Set<Sentence> set = toReturn.get(entity);
			if (set == null)
				set = new HashSet<Sentence>();
			set.add(sentence);
			toReturn.put(entity, set);
		}
		stmt.close();
		result.close();
		connection.commit();
		return toReturn;
	}

	/**
	 * Create Output-Tables for the similarity-/cooccurrence-Pairs
	 * 
	 * @param connection DBConnection
	 * @param type
	 * @throws SQLException
	 */
	public static String createPairsTable(Connection connection, IEType type, boolean trimSentences, int contextSize)
			throws SQLException {
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		String table = "Pairs";
		if (trimSentences) {
			table = "Pairs_contextSize_" + contextSize;
		}
		String sql = "DROP TABLE IF EXISTS " + table;
		stmt.executeUpdate(sql);

		sql = null;
		if (type == IEType.TOOL) {
			sql = "CREATE TABLE " + table
					+ "(ID INTEGER PRIMARY KEY AUTOINCREMENT, Tool_A TEXT NOT NULL,  Tool_B Text NOT NULL,  Count DOUBLE NOT NULL, validated_A INT NOT NULL, validated_B INT NOT NULL, FirstLevelCategory_A TEXT NOT NULL, SecondLevelCategory_A TEXT NOT NULL, FirstLevelCategory_B TEXT NOT NULL, SecondLevelCategory_B TEXT NOT NULL)";
		} else {
//		if (type == IEType.COMPETENCE_IN_3) {
			sql = "CREATE TABLE " + table
					+ " (ID INTEGER PRIMARY KEY AUTOINCREMENT, Competence_A TEXT NOT NULL,  Competence_B Text NOT NULL, Count DOUBLE NOT NULL, validated_A INT NOT NULL, validated_B INT NOT NULL, FirstLevelCategory_A TEXT NOT NULL, SecondLevelCategory_A TEXT NOT NULL, FirstLevelCategory_B TEXT NOT NULL, SecondLevelCategory_B TEXT NOT NULL)";
		}

		stmt.executeUpdate(sql);
		stmt.close();
		createPairIndex(connection, type, table);
		connection.commit();
		return table;
	}

	/**
	 * Create Output-Tables for the similarity-/cooccurrence-Pairs
	 * 
	 * @param connection DBConnection
	 * @param type
	 * @throws SQLException
	 */
	public static String createPairsTable(Connection connection, IEType type) throws SQLException {
		return createPairsTable(connection, type, false, -1);
	}

	public static void writePairs(Connection connection, Set<Pair> pairs, IEType type, String tableName)
			throws SQLException {
		connection.setAutoCommit(false);
		String sql = null;
		if (type == IEType.TOOL) {
			sql = "INSERT INTO " + tableName
					+ " (Tool_A, Tool_B, Count, validated_A, validated_B, FirstLevelCategory_A, SecondLevelCategory_A, FirstLevelCategory_B, SecondLevelCategory_B) VALUES(?,?,?,?,?,?,?,?,?)";
		} else {
//		if (type == IEType.COMPETENCE_IN_3) {
			sql = "INSERT INTO " + tableName
					+ " (Competence_A, Competence_B, Count, validated_A, validated_B, FirstLevelCategory_A, SecondLevelCategory_A, FirstLevelCategory_B, SecondLevelCategory_B) VALUES(?,?,?,?,?,?,?,?,?)";
		}

		PreparedStatement stmt = connection.prepareStatement(sql);
		for (Pair pair : pairs) {
			stmt.setString(1, pair.getE1().getLemma());
			stmt.setString(2, pair.getE2().getLemma());
			Set<Category> cats1 = pair.getE1().getCategories();// categories.get(pair.getE1());
			Set<Category> cats2 = pair.getE2().getCategories();// categories.get(pair.getE2());
			if (cats1 != null) {
				StringBuffer sb1 = new StringBuffer();
				StringBuffer sb2 = new StringBuffer();
				for (Category cat : cats1) {
					sb1.append(" | " + cat.getFirstLevelCategory());
					sb2.append(" | " + cat.getSecondLevelCategory());
				}
				stmt.setString(6, sb1.toString().substring(3));
				stmt.setString(7, sb2.toString().substring(3));
			} else {
				stmt.setString(6, "unknown");
				stmt.setString(7, "unknown");
			}
			if (cats2 != null) {
				StringBuffer sb1 = new StringBuffer();
				StringBuffer sb2 = new StringBuffer();
				for (Category cat : cats2) {
					sb1.append(" | " + cat.getFirstLevelCategory());
					sb2.append(" | " + cat.getSecondLevelCategory());
				}
				stmt.setString(8, sb1.toString().substring(3));
				stmt.setString(9, sb2.toString().substring(3));
			} else {
				stmt.setString(8, "unknown");
				stmt.setString(9, "unknown");
			}
			if (pair.getE1().isValidated()) {
				stmt.setInt(4, 1);
			} else {
				stmt.setInt(4, 0);
			}
			if (pair.getE2().isValidated()) {
				stmt.setInt(5, 1);
			} else {
				stmt.setInt(5, 0);
			}
			stmt.setDouble(3, pair.getScore());
			stmt.addBatch();

		}
		stmt.executeBatch();
		stmt.close();
		connection.commit();
	}

	/**
	 * @param connection
	 * @param type
	 * @param level      grouping-level; will be appended to the table name
	 * @throws SQLException
	 */
	public static String createGroupTables(Connection connection, IEType type, String level, boolean trimSentences,
			int contextSize) throws SQLException {
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		String table = "Groups_level_" + level;
		if (trimSentences) {
			table = "Groups_level_" + level + "_contextSize_" + contextSize;
		}
		String sql = "DROP TABLE IF EXISTS " + table;
		stmt.executeUpdate(sql);
		sql = null;
		if (type == IEType.TOOL) {
			sql = "CREATE TABLE " + table
					+ " (ID INTEGER PRIMARY KEY AUTOINCREMENT, Tool TEXT NOT NULL,  GroupID INT NOT NULL, validated INT NOT NULL, FirstLevelCategory TEXT, SecondLevelCategory TEXT)";
		} else {
//		if (type == IEType.COMPETENCE_IN_3) {
			sql = "CREATE TABLE " + table
					+ " (ID INTEGER PRIMARY KEY AUTOINCREMENT, Competence TEXT NOT NULL,  GroupID INT NOT NULL, validated INT NOT NULL, FirstLevelCategory TEXT, SecondLevelCategory TEXT)";
		}

		stmt = connection.createStatement();
		stmt.executeUpdate(sql);
		stmt.close();
		connection.commit();
		return table;
	}

	/**
	 * @param connection
	 * @param type
	 * @param level      grouping-level; will be appended to the table name
	 * @throws SQLException
	 */
	public static String createGroupTables(Connection connection, IEType type, String level) throws SQLException {
		return createGroupTables(connection, type, level, false, -1);
	}

	/**
	 * @param connection
	 * @param groups     Map of groupIDs (as keys) and tools/competences (as values)
	 * @param type
	 * @param level      grouping-level; to identify the correct table
	 * @throws SQLException
	 */
	public static void writeGroups(Connection connection, Map<Integer, List<Entity>> groups, IEType type, String level,
			String table) throws SQLException {
		connection.setAutoCommit(false);
		String sql = null;
		if (type == IEType.TOOL) {
			sql = "INSERT INTO " + table
					+ " (Tool, GroupID, validated, FirstLevelCategory, SecondLevelCategory) VALUES (?,?,?,?,?)";
		} else {
//		if (type == IEType.COMPETENCE_IN_3) {
			sql = "INSERT INTO " + table
					+ " (Competence, GroupID, validated, FirstLevelCategory, SecondLevelCategory) VALUES (?,?,?,?,?)";
		}

		PreparedStatement stmt = connection.prepareStatement(sql);
		for (int groupId : groups.keySet()) {
			for (Entity entity : groups.get(groupId)) {
				stmt.setString(1, entity.getLemma());
				if (groupId == 0) {
					groupId = -1;
				}
				stmt.setInt(2, groupId);
				if (entity.getCategories() != null && entity.getCategories().size() > 0) {
					StringBuffer sb1 = new StringBuffer();
					StringBuffer sb2 = new StringBuffer();
					Set<Category> cats = entity.getCategories();
					for (Category c : cats) {
						sb1.append(" | " + c.getFirstLevelCategory());
						sb2.append(" | " + c.getSecondLevelCategory());
					}
					stmt.setString(4, sb1.toString().substring(3));
					stmt.setString(5, sb2.toString().substring(3));
				} else {
					stmt.setString(4, "unknown");
					stmt.setString(5, "unknown");
				}
				if (entity.isValidated()) {
					stmt.setInt(3, 1);
				} else {
					stmt.setInt(3, 0);
				}
				stmt.addBatch();
			}
		}
		stmt.executeBatch();
		stmt.close();
		connection.commit();
	}

	/**
	 * Read already categorized competences from the Categories-DB
	 * 
	 * @param connection
	 * @return Map with the categorized competences (as keys) and their categories
	 *         (as values)
	 * @throws SQLException
	 */
	public static Map<Entity, Set<Category>> readCategorizedComps(Connection connection) throws SQLException {
		Map<Entity, Set<Category>> toReturn = new HashMap<Entity, Set<Category>>();
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		ResultSet result = stmt.executeQuery(
				"SELECT Competence, FirstLevelCategory, SecondLevelCategory, ThirdLevelCategory, Synonyms, Orig_String FROM Categories");
		while (result.next()) {
			String competence = result.getString(1);
			String orig_string = result.getString(6);
			String firstLevelCat = result.getString(2);
			String secondLevelCat = result.getString(3);
			String thirdLevelCat = result.getString(4);
			String synonyms = result.getString(5);
			Entity entity = new Entity(competence);
			entity.setValidated(true);
			entity.setString(orig_string);
			if (synonyms != null) {
				String[] split = synonyms.split(" \\| ");
				Set<String> synSet = new HashSet<String>();
				for (String syn : split) {
					synSet.add(syn.trim());
				}
				entity.setSynonyms(synSet);
			}
			Category cat = new CompetenceCategory(firstLevelCat, secondLevelCat);
			((CompetenceCategory) cat).setThirdLevelCategory(thirdLevelCat);
			Set<Category> set = toReturn.get(entity);
			if (set == null)
				set = new HashSet<Category>();
			set.add(cat);
			entity.setCategories(set);
			toReturn.put(entity, set);
		}
		stmt.close();
		result.close();
		connection.commit();
		return toReturn;
	}

	/**
	 * Read already categorized tools from the Categories-DB
	 * 
	 * @param connection
	 * @return Map with the categorized tools (as keys) and their categories (as
	 *         values)
	 * @throws SQLException
	 */
	public static Map<Entity, Set<Category>> readCategorizedTools(Connection connection) throws SQLException {
		Map<Entity, Set<Category>> toReturn = new HashMap<Entity, Set<Category>>();
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		ResultSet result = stmt
				.executeQuery("SELECT Tool, FirstLevelCategory, SecondLevelCategory, ShortKey FROM Categories");
		while (result.next()) {
			String tool = result.getString(1);
			String firstLevelCategory = result.getString(2);
			String secondLevelCategory = result.getString(3);
			String shortKey = result.getString(4);
			Entity entity = new Entity(tool);
			entity.setValidated(true);
			Category cat = new ToolCategory(firstLevelCategory, secondLevelCategory, shortKey);
			Set<Category> set = toReturn.get(entity);
			if (set == null)
				set = new HashSet<Category>();
			set.add(cat);
			entity.setCategories(set);
			toReturn.put(entity, set);
		}
		return toReturn;
	}

	/**
	 * @param connection DBConnection
	 * @return Map von Kompetenz/-Tool-Paaren (keys) und ihrem Ähnlichkeitswert
	 *         (values)
	 * @throws SQLException
	 */
	public static Map<Pair, Double> readPairs(Connection connection, String table) throws SQLException {
		Map<Pair, Double> toReturn = new HashMap<Pair, Double>();
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		String sql = "SELECT * FROM " + table;
		ResultSet result = stmt.executeQuery(sql);
		while (result.next()) {
			Pair pair = new Pair(result.getString(2), result.getString(3));
			pair.setScore(result.getDouble(4));
			toReturn.put(pair, pair.getScore());
		}
		return toReturn;
	}

	/**
	 * reads the not validated competence-/tool- extractions from DB
	 * 
	 * @param connection
	 * @param type
	 * @return set of entities
	 * @throws SQLException
	 */
	public static Set<Entity> readEntities(Connection connection, IEType type) throws SQLException {
		Set<Entity> toReturn = new HashSet<Entity>();
		connection.setAutoCommit(false);
		String sql = null;
		if (type == IEType.TOOL) {
			sql = "SELECT Tool FROM Tools";
		} else {
//		if (type == IEType.COMPETENCE_IN_3) {
			sql = "SELECT Comp FROM Competences";
		}

		Statement stmt = connection.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		while (result.next()) {
			String comp = result.getString(1);
			Entity e = new Entity(comp);
			e.setValidated(false);
			toReturn.add(e);
		}
		stmt.close();
		result.close();
		connection.commit();
		return toReturn;
	}

	public static Map<Integer, List<Entity>> readGroups(Connection connection, String table) throws SQLException {
		Map<Integer, List<Entity>> toReturn = new HashMap<Integer, List<Entity>>();
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		String sql = "SELECT * FROM " + table;
		ResultSet result = null;
		try {
			result = stmt.executeQuery(sql);
		} catch (SQLException e) {
			if (connection.getCatalog() != null) {
				System.out.println(
						"In der DB " + connection.getCatalog() + " befindet sich keine Tabelle mit dem Namen " + table);
				System.out.println("Bitte Konfiguration anpassen");
			} else {
				e.printStackTrace();
			}
			System.exit(0);
		}
		while (result.next()) {
			String lemma = result.getString(2);
			int groupId = result.getInt(3);
			int validated = result.getInt(4);
			String[] firstCat = result.getString(5).split(" \\| ");
			String[] secondCat = result.getString(6).split(" \\| ");
			Set<Category> categories = new HashSet<Category>();
			for (int i = 0; i < firstCat.length; i++) {
				if ((firstCat[i]).equals("unknown"))
					continue;
				Category category = new CompetenceCategory(firstCat[i], secondCat[i]);
				categories.add(category);
			}
			Entity entity = new Entity(lemma);
			entity.setValidated(validated == 1 ? true : false);
			entity.setCategories(categories);
			List<Entity> entitiesForID = toReturn.get(groupId);
			if (entitiesForID == null)
				entitiesForID = new ArrayList<Entity>();
			entitiesForID.add(entity);
			toReturn.put(groupId, entitiesForID);
		}
		return toReturn;
	}

	public static void createPairIndex(Connection connection, IEType type, String table) throws SQLException {
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		String sql = null;
		if (type == IEType.TOOL) {
			sql = "CREATE INDEX pairIndex_" + table + " ON " + table + " (Tool_A, Tool_B)";
		} else {
//		if(type == IEType.COMPETENCE_IN_3){
			sql = "CREATE INDEX pairIndex_" + table + " ON " + table + " (Competence_A, Competence_B)";
		}

		stmt.executeUpdate(sql);
		stmt.close();
		connection.commit();
	}

}
