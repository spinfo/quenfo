package quenfo.de.uni_koeln.spinfo.categorization.applications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import quenfo.de.uni_koeln.spinfo.categorization.data.Category;
import quenfo.de.uni_koeln.spinfo.categorization.data.Entity;
import quenfo.de.uni_koeln.spinfo.categorization.db_io.Cat_DBConnector;
import quenfo.de.uni_koeln.spinfo.categorization.workflow.Cat_Jobs;
import quenfo.de.uni_koeln.spinfo.categorization.workflow.SimilarityCalculator;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;

/**
 * @author geduldia
 * 
 * 
 *         workflow to group ALL tools (= already categorized, already
 *         validated and not yet validated tools) according to their
 *         needleman-wunsch string-similarity
 * 
 *         1. read tools from the different input sources (categories-DB,
 *         tools.txt and extractions-DB)
 * 
 *         2. calculate the nw-similarity for each possible pair of tools.
 *         store each pair with a similarity of at least [minPairSimimlarity] *
 *         (s1.length + s2.length) in an output-DB (--> Table 'Pairs')
 *         
 *         3. build groups of similar competences. 5 rounds with different min. values for group-membership.
 *         store the results of each round in the output-DB (--> Tables: 'Groups_1' - 'Groups_5')
 *         The Group with the most group-members gets the highest groupID etc. 
 *          *         
 * 
 *
 */

public class GroupToolsByStringSimilarity {

	// wird an den Namen der Output-DB angehängt
	private static String jahrgang = "DL_ALL_Spinfo";

	// DB mit den bereits kategorisierten Tools
	private static String categoriesDB = "C:/sqlite/categorization/tools/CategorizedTools.db";

	private static String validTools = "information_extraction/data/tools/tools.txt";

	// DB mit den extrahierten Toolvorschlägen
	private static String notValidatedTools = "C:/sqlite/information_extraction/tools/CorrectableTools_" + jahrgang
			+ ".db";

	// Ordner für die Output-DB
	private static String outputFolder = "C:/sqlite/categorization/tools/";

	// Name der Output-DB
	private static String outputDB = "ToolStringSimilarities_" + jahrgang + ".db";

	// mindest Ähnlichkeit für Paare (Wert * (s1.length + s2.length))
	private static double minPairSimilarity = 1.0;

	// mindest Ähnlichkeiten für Gruppenzugehörigkeit (Wert * (s1.legth +
	// s2.length)))
	private static double[] minGroupSimilarities = new double[] { 1.4, 1.3, 1.2, 1.1, 1.0 };

	// Werte für die Needleman-Wunsch-Berechnung
	// (Straf-)punkte für:
	private static int match = 3;
	private static int gap = -1;
	private static int mismatch = -1;

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

		long before = System.currentTimeMillis();

		// Einlesen der bereits kategorisierten Tools
		if(!new File(categoriesDB).exists()){
			System.out.println("Die DB " + categoriesDB.substring(categoriesDB.lastIndexOf("/")+1, categoriesDB.length())+" im Ordner "+categoriesDB.substring(0,categoriesDB.lastIndexOf("/"))+" existiert nicht");
			System.out.println("Bitte den Pfad anpassen, oder die DB in den entsprechenden Ordner verschieben");
			System.exit(0);
		}
		Connection connection = Cat_DBConnector.connect(categoriesDB);
		System.out.println(
				"\nread categorized tools from DB " + categoriesDB.substring(categoriesDB.lastIndexOf("/") + 1));
		Map<Entity, Set<Category>> categories = Cat_DBConnector.readCategorizedTools(connection);
		System.out.println("--> " + categories.keySet().size()+" tools");

		// Einlesen der bereits validierten Tools (und rausfiltern der
		// bereits kategorisierten)
		File file = new File(validTools);
		System.out.println(
				"\nread all validated tools from file " + validTools.substring(validTools.lastIndexOf("/") + 1));
		Set<Entity> validTools = Cat_Jobs.readEntitiesFile(file, categories.keySet());
		System.out.println("--> " + validTools.size() + " tools( + the categorized tools)");

		// Einlesen der noch nicht validierten Tools
		connection = Cat_DBConnector.connect(notValidatedTools);
		System.out.println("\nread all not validated tools from DB "
				+ notValidatedTools.substring(notValidatedTools.lastIndexOf("/") + 1));
		Set<Entity> notValidatedTools = Cat_DBConnector.readEntities(connection, IEType.TOOL);
		System.out.println("--> " + notValidatedTools.size()+" tools");

		// Zusammenfügen aller eingelesenen Tools
		System.out.println("\nmerge all tools");
		Set<Entity> allTools = new HashSet<Entity>();
		allTools.addAll(categories.keySet());
		allTools.addAll(notValidatedTools);
		allTools.addAll(validTools);
		System.out.println("--> " + allTools.size()+" tools");
		notValidatedTools.clear();
		validTools.clear();
		categories.clear();

		// Berechnung der paarweisen NW-Ähnlichkeit und Speicherung der
		// ähnlichen Paare in der Output-DB (Table Pairs)
		System.out.println("\ncalc. pairwise string-similarities");
		if (!new File(outputFolder).exists()) {
			new File(outputFolder).mkdirs();
		}
		connection = Cat_DBConnector.connect(outputFolder + outputDB);
		Cat_DBConnector.createPairsTable(connection, IEType.TOOL);
		SimilarityCalculator sc = new SimilarityCalculator(match, mismatch, gap);
		Map<Double, List<Entity>> similarTools = Cat_Jobs.getSimilarityPairs(new ArrayList<Entity>(allTools),
				sc, minPairSimilarity, connection, IEType.TOOL);
		System.out.println("number of similar tools: " + similarTools.size());

		// Gruppenbildung
		System.out.println("\nbuild similarity groups");
		for (int level = 1; level <= minGroupSimilarities.length; level++) {
			double minSimilarity = minGroupSimilarities[level - 1];
			System.out.println(
					"\nlevel " + level + " (min. similarity = " + minSimilarity + " * (s1.length + s2.length))");
			Map<Integer, List<Entity>> similarityGroups = Cat_Jobs.buildStringSimilarityGroups(similarTools, minSimilarity, sc);
			System.out.println("number of Groups: " + similarityGroups.keySet().size());
			String tableName = Cat_DBConnector.createGroupTables(connection, IEType.TOOL, Integer.toString(level));
			System.out.println("write groups in Output-DB Table 'Groups_" + level + "'");
			Cat_DBConnector.writeGroups(connection, similarityGroups, IEType.TOOL, Integer.toString(level), tableName);
		}

		long after = System.currentTimeMillis();
		double time = (((double) after - before) / 1000) / 60;
		if (time > 60) {
			System.out.println("\nfinished Similarity-Groups in " + (time / 60) + "hours");
		} else {
			System.out.println("\nfinished Similarity-Groups in " + time + " minutes");
		}
	}
}
