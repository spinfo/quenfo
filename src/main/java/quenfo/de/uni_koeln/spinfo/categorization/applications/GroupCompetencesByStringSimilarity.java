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
 * 
 *         workflow to group ALL competences (= already categorized, already
 *         validated and not yet validated competences) according to their
 *         needleman-wunsch string-similarity
 * 
 *         1. read comptences from the different input sources (categories-DB,
 *         competences.txt and extractions-DB)
 * 
 *         2. calculate the nw-similarity for each possible pair of competences.
 *         store each pair with a similarity of at least [minPairSimimlarity] *
 *         (s1.length + s2.length) in an output-DB (--> Table 'Pairs')
 *         
 *         3. build groups of similar entities. 5 rounds with different min. values for group-membership.
 *         store the results of each round in the output-DB (--> Tables: 'Groups_1' -  'Groups_5')
 *         The Group with the most group-members gets the highest groupID etc. 
 *       
 * 
 *
 */
public class GroupCompetencesByStringSimilarity {

	// wird an den Namen der Output-DB angehängt
	private static String jahrgang = "2011";

	// DB mit den bereits kategorisierten (AMS-) Kompetenzen
	private static String categoriesDB = "C:/sqlite/categorization/competences/CategorizedCompetences.db";

	private static String validCompetences = "information_extraction/data/competences/competences.txt";
	
	// DB mit den extrahierten Kompetenzvorschlägen
	private static String notValidatedComps = "C:/sqlite/information_extraction/competences/CorrectableCompetences_"
			+ jahrgang + ".db";

	// Ordner für die Output-DB
	private static String outputFolder = "C:/sqlite/categorization/competences/";

	// Name der Output-DB
	private static String outputDB = "CompetenceStringSimilarities_" + jahrgang + ".db";

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

		// Einlesen der bereits kategorisierten Kompetenzen
		if(!new File(categoriesDB).exists()){
			System.out.println("Die DB " + categoriesDB.substring(categoriesDB.lastIndexOf("/")+1, categoriesDB.length())+" im Ordner "+categoriesDB.substring(0,categoriesDB.lastIndexOf("/"))+" existiert nicht");
			System.out.println("Bitte den Pfad anpassen, oder die DB in den entsprechenden Ordner verschieben");
			System.exit(0);
		}
		Connection connection = Cat_DBConnector.connect(categoriesDB);
		System.out.println(
				"\nread categorized comps from DB " + categoriesDB.substring(categoriesDB.lastIndexOf("/") + 1));
		Map<Entity, Set<Category>> categories = Cat_DBConnector.readCategorizedComps(connection);
		System.out.println("--> " + categories.keySet().size()+" competences");

		// Einlesen der bereits validierten (aber noch nicht kategorisierten)
		// Kompetenzen (und rausfiltern der
		// bereits kategorisierten)
		File file = new File(validCompetences);
		System.out.println("\nread all validated comps from file "
				+ validCompetences.substring(validCompetences.lastIndexOf("/") + 1));
		Set<Entity> validCompetences = Cat_Jobs.readEntitiesFile(file, categories.keySet());
		System.out.println("--> " + validCompetences.size() + " competences ( + the categorized competences)");

		// Einlesen der noch nicht validierten Kompetenzen
		connection = Cat_DBConnector.connect(notValidatedComps);
		System.out.println("\nread all not validated competences from DB "
				+ notValidatedComps.substring(notValidatedComps.lastIndexOf("/") + 1));
		Set<Entity> notValidatedCompetences = Cat_DBConnector.readEntities(connection, IEType.COMPETENCE_IN_3);
		System.out.println("--> " + notValidatedCompetences.size()+" competences");

		// Zusammenfügen aller eingelesenen Kompetenzen
		System.out.println("\nmerge all competences");
		Set<Entity> allCompetences = new HashSet<Entity>();
		allCompetences.addAll(categories.keySet());
		categories = null;
		allCompetences.addAll(notValidatedCompetences);
		notValidatedCompetences = null;
		allCompetences.addAll(validCompetences);
		validCompetences = null;
		System.out.println("--> " + allCompetences.size()+ " competences");

		// Berechnung der paarweisen NW-Ähnlichkeit und Speicherung der
		// ähnlichen Paare in der Output-DB (Table Pairs)
		System.out.println("\ncalc. pairwise string-similarities");
		if (!new File(outputFolder).exists()) {
			new File(outputFolder).mkdirs();
		}
		connection = Cat_DBConnector.connect(outputFolder + outputDB);
		Cat_DBConnector.createPairsTable(connection, IEType.COMPETENCE_IN_3);
		SimilarityCalculator sc = new SimilarityCalculator(match, mismatch, gap);
		Map<Double,List<Entity>> similarComps = Cat_Jobs.getSimilarityPairs(new ArrayList<Entity>(allCompetences), 
				sc, minPairSimilarity, connection, IEType.COMPETENCE_IN_3);
		System.out.println("--> number of similar competences: " + similarComps.size());

		// Gruppenbildung
		System.out.println("\nbuild similarity groups");
		for (int level = 1; level <= minGroupSimilarities.length; level++) {
			double minSimilarity = minGroupSimilarities[level - 1];
			System.out.println(
					"\nlevel " + level + " (min. similarity = " + minSimilarity + " * (s1.length + s2.length))");
			Map<Integer, List<Entity>> similarityGroups = Cat_Jobs.buildStringSimilarityGroups(similarComps, minSimilarity,
					sc);
			System.out.println("number of Groups: " + similarityGroups.keySet().size());
			String tableName = Cat_DBConnector.createGroupTables(connection, IEType.COMPETENCE_IN_3, Integer.toString(level));
			System.out.println("write groups in Output-DB Table 'Groups_level_" + level + "'");
			Cat_DBConnector.writeGroups(connection, similarityGroups, IEType.COMPETENCE_IN_3, Integer.toString(level), tableName);
		}

		long after = System.currentTimeMillis();
		double time = (((after - before) / (double) 1000) / 60) / 60;
		System.out.println("\noverall time: " + time + " hours");
	}
}
