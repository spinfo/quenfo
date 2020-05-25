package quenfo.de.uni_koeln.spinfo.categorization.applications;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import quenfo.de.uni_koeln.spinfo.categorization.data.Category;
import quenfo.de.uni_koeln.spinfo.categorization.data.Entity;
import quenfo.de.uni_koeln.spinfo.categorization.data.Pair;
import quenfo.de.uni_koeln.spinfo.categorization.data.Sentence;
import quenfo.de.uni_koeln.spinfo.categorization.db_io.Cat_DBConnector;
import quenfo.de.uni_koeln.spinfo.categorization.workflow.Cat_Jobs;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;

/**
 * @author geduldia
 * 
 * 
 *         workflow to group ALL competences (= already categorized, already
 *         validated and not yet validated competences) according to their
 *         cooccurrences
 * 
 *         1. read matching competences (incl. their containig sentences) from
 *         the different input sources (CompetenceMatches-DB and
 *         notValidatedCompetenceMatches-DB)
 * 
 *         2. calculate the chi-square-value for each possible pair of
 *         competences. store each cooccurring pair in an output-DB (--> Table
 *         'Pairs')
 * 
 *         3. build groups of cooccurring competences. 5 rounds with different
 *         min.chi-square-values for group-membership. store the results of each
 *         round in the output-DB (--> Tables: 'Groups_1' - 'Groups_5') The
 *         Group with the most group-members gets the highest groupID etc.
 */

public class GroupCompetencesByCooccurrence {

	// wird an den Namen der Output-DB angehängt
	private static String jahrgang = "2011";

	// DB mit den bereits kategorisierten (AMS-) Kompetenzen
	private static String categoriesDB = "C:/sqlite/categorization/competences/CategorizedCompetences.db";

	// Matching-Ergebnis der bereits validierten Kompetenzen
	private static String validMatches = "C:/sqlite/matching/competences/CompetenceMatches_" + jahrgang + ".db";

	// Matching-Ergebnis der noch nicht validierten Kompetenzen
	private static String notValidMatches = "C:/sqlite/matching/competences/NotValidatedCompetenceMatches_" + jahrgang
			+ ".db";

	// Ordner für die Output-DB
	private static String outputFolder = "C:/sqlite/categorization/competences/";

	// Name der Output-DB
	private static String outputDB = "CompetenceCooccurrences_" + jahrgang + ".db";

	// mindest Chi-Quadrat-Werte für Gruppenzugehörigkeit
	private static double[] minChiSquares = new double[] { 0.5, 0.1, 0.05, 0.005, 0.0005 };

	// legt fest, ob die Sätze vor der Kookkurrenz-Analyse gekürzt werden oder
	// nicht
	private static boolean trimSentences = false;

	// falls die Sätze gekürzt werden sollen: Anzahl der Wörter vor und hinter
	// dem Kompetenzbegriff
	private static int contextSize = 5;

	public static void main(String[] args) throws ClassNotFoundException, SQLException {

		long before = System.currentTimeMillis();

		// Einlesen der bereits kategorisierten Kopetenzen
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

		// Einlesen der validierten Kompetenzen und ihrer Kontexte
		connection = Cat_DBConnector.connect(validMatches);
		System.out.println("\nread all valid competences inkl. contexts from DB "
				+ validMatches.substring(validMatches.lastIndexOf("/") + 1));
		Map<Entity, Set<Sentence>> validSentencesByComp = Cat_DBConnector.getSentencesByEntity(connection, categories,
				IEType.COMPETENCE_IN_3, true, trimSentences);
		categories = null;
		System.out.println("--> " + validSentencesByComp.keySet().size()+" different competences");

		// Einlesen der nicht-validierten Kompetenezn inkl. ihrer Kontexte
		connection = Cat_DBConnector.connect(notValidMatches);
		System.out.println("\nread all not validated competences inkl. contexts from DB "
				+ notValidMatches.substring(notValidMatches.lastIndexOf("/") + 1));
		Map<Entity, Set<Sentence>> notValidSentencesByComp = Cat_DBConnector.getSentencesByEntity(connection, null,
				IEType.COMPETENCE_IN_3, false, trimSentences);
		System.out.println("--> " + notValidSentencesByComp.keySet().size()+" different competences");

		// Zusammenfügen aller eingelesenen Kompetenzen/Kontexte
		System.out.println("\nmerge all competences and contexts");
		Map<Entity, Set<Sentence>> allSentencesByComp = new HashMap<Entity, Set<Sentence>>();
		allSentencesByComp.putAll(notValidSentencesByComp);
		notValidSentencesByComp = null;
		for (Entity e : validSentencesByComp.keySet()) {
			Set<Sentence> set = allSentencesByComp.get(e);
			if (set == null) {
				set = new HashSet<Sentence>();
			}
			set.addAll(validSentencesByComp.get(e));
			allSentencesByComp.put(e, set);
		}
		validSentencesByComp = null;
		Map<Sentence, List<Entity>> allCompsBySentence = Cat_Jobs.reverseMap(allSentencesByComp);
		System.out.println("--> " + allSentencesByComp.keySet().size()+" different competenes");
		System.out.println("--> " + allCompsBySentence.keySet().size()+" different contexts");
		
		if(trimSentences){
			System.out.println("\ntrim sentences to a left and right context-size of "+ contextSize +" words");
			//Sätze trimmen auf x- Wörter vor und hinter der Kompeten
			allSentencesByComp = Cat_Jobs.trimSentences(allSentencesByComp, contextSize);
		//	allCompsBySentence = Cat_Jobs.reverseMap(allSentencesByComp);
		}

		// Bestimmen aller Kookkurrenz-Paare
		System.out.println("\ncalculate cooccurrence-score for each cooccurring competence-pair and write in DB");
		if (!new File(outputFolder).exists()) {
			new File(outputFolder).mkdirs();
		}
		connection = Cat_DBConnector.connect(outputFolder + outputDB);
		String tableName = Cat_DBConnector.createPairsTable(connection, IEType.COMPETENCE_IN_3, trimSentences, contextSize);
		Map<Double,List<Entity>> cooccurringComps;
		if(trimSentences){
			cooccurringComps = Cat_Jobs.getTrimmedCooccurrencePairs(allSentencesByComp, allCompsBySentence, connection, IEType.COMPETENCE_IN_3, tableName);
		}
		else{
			cooccurringComps = Cat_Jobs.getCooccurrencePairs(allSentencesByComp, allCompsBySentence, connection,
					IEType.COMPETENCE_IN_3, tableName);
		
		}
		allCompsBySentence = null;
		allSentencesByComp = null;

		Map<Pair, Double> pairs = Cat_DBConnector.readPairs(connection, tableName);
		System.out.println("--> pairs: " + pairs.size());
		System.out.println("-->  cooccurring comps " + cooccurringComps.size());

		// Gruppenbildung
		System.out.println("\nbuild cooccurrence groups");
		for (int level = 1; level <= 5; level++) {
			double minChiSquare = minChiSquares[level - 1];
			System.out.println("level " + level + " --> min. chi-square = " + minChiSquare);
			Map<Integer, List<Entity>> cooccurrenceGroups = Cat_Jobs.buildCooccurrenceGroups(cooccurringComps,
					minChiSquare, pairs);
			System.out.println("write results");
			tableName = Cat_DBConnector.createGroupTables(connection, IEType.COMPETENCE_IN_3, Integer.toString(level), trimSentences, contextSize);
			Cat_DBConnector.writeGroups(connection, cooccurrenceGroups, IEType.COMPETENCE_IN_3, Integer.toString(level), tableName);
		}
		long after = System.currentTimeMillis();
		double time = (((double) after - before) / 1000) / 60;
		System.out.println("\nfinished Cooccuurrence-Groups in " + time + " minutes");
	}
}
