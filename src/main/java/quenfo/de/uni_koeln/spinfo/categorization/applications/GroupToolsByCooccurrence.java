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
 *         workflow to group ALL tools (= already categorized, already validated
 *         and not yet validated tools) according to their cooccurrences
 * 
 *         1. read matching tools (incl. their containig sentences) from the
 *         different input sources (ToolMatches-DB and
 *         notValidatedToolMatches-DB)
 * 
 *         2. calculate the chi-square-value for each possible pair of tools.
 *         store each cooccurring pair in an output-DB (--> Table 'Pairs')
 * 
 *         3. build groups of cooccurring tools. 5 rounds with different
 *         min.chi-square-values for group-membership. store the results of each
 *         round in the output-DB (--> Tables: 'Groups_1' - 'Groups_5') The
 *         Group with the most group-members gets the highest groupID etc.
 */

public class GroupToolsByCooccurrence {
	// wird an den Namen der Output-DB angehängt
	private static String jahrgang = "2011";

	// DB mit den bereits kategorisierten (AMS-) Kompetenzen
	private static String categoriesDB = "C:/sqlite/categorization/tools/CategorizedTools.db";

	// Matching-Ergebnis der bereits validierten Kompetenzen
	private static String validMatches = "C:/sqlite/matching/tools/ToolMatches_" + jahrgang + ".db";

	// Matching-Ergebnis der noch nicht validierten Kompetenzen
	private static String notValidMatches = "C:/sqlite/matching/tools/NotValidatedToolMatches_" + jahrgang + ".db";

	// Ordner für die Output-DB
	private static String outputFolder = "C:/sqlite/categorization/tools/";

	// Name der Output-DB
	private static String outputDB = "ToolCooccurrences_" + jahrgang + ".db";

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
		System.out.println("--> " + categories.keySet().size()+ " tools");

		// Einlesen der validierten Tools und ihrer Kontexte
		connection = Cat_DBConnector.connect(validMatches);
		System.out.println("\nread all valid tools inkl. contexts from DB "
				+ validMatches.substring(validMatches.lastIndexOf("/") + 1));
		Map<Entity, Set<Sentence>> validSentencesByTool = Cat_DBConnector.getSentencesByEntity(connection, categories,
				IEType.TOOL, true, trimSentences);
		categories = null;
		System.out.println("--> " + validSentencesByTool.keySet().size()+" different tools");

		// Einlesen der nicht-validierten Tools inkl. ihrer Kontexte
		connection = Cat_DBConnector.connect(notValidMatches);
		System.out.println("\nread all not validated tools inkl. contexts from DB "
				+ notValidMatches.substring(notValidMatches.lastIndexOf("/") + 1));
		Map<Entity, Set<Sentence>> notValidSentencesByTool = Cat_DBConnector.getSentencesByEntity(connection, null,
				IEType.TOOL, false, trimSentences);
		System.out.println("--> " + notValidSentencesByTool.keySet().size()+" different tools");

		// Zusammenfügen aller eingelesenen Tools/Kontexte
		System.out.println("\nmerge all tools and contexts");
		Map<Entity, Set<Sentence>> allSentencesByTool = new HashMap<Entity, Set<Sentence>>();
		allSentencesByTool.putAll(notValidSentencesByTool);
		notValidSentencesByTool = null;
		for (Entity e : validSentencesByTool.keySet()) {
			Set<Sentence> set = allSentencesByTool.get(e);
			if (set == null) {
				set = new HashSet<Sentence>();
			}
			set.addAll(validSentencesByTool.get(e));
			allSentencesByTool.put(e, set);
		}
		validSentencesByTool = null;
		Map<Sentence, List<Entity>> allToolsBySentence = Cat_Jobs.reverseMap(allSentencesByTool);
		System.out.println("--> " + allSentencesByTool.keySet().size()+" different tools");
		System.out.println("--> " + allToolsBySentence.keySet().size()+" different contexts");

		
		if (trimSentences) {
			System.out.println("\ntrim sentences to a left and right context-size of " + contextSize + " words");
			// Sätze trimmen auf x- Wörter vor und hinter der Kompetenz
			allSentencesByTool = Cat_Jobs.trimSentences(allSentencesByTool, contextSize);
			//allToolsBySentence = Cat_Jobs.reverseMap(allSentencesByTool);
		}
		
		// Bestimmen aller Kookkurrenz-Paare
		System.out.println("\ncalculate cooccurrence-score for each cooccurring tool-pair and write in DB");
		if (!new File(outputFolder).exists()) {
			new File(outputFolder).mkdirs();
		}
		connection = Cat_DBConnector.connect(outputFolder + outputDB);
		String tableName = Cat_DBConnector.createPairsTable(connection, IEType.TOOL, trimSentences, contextSize);
		Map<Double, List<Entity>> cooccurringTools = null;
		if(trimSentences){
			cooccurringTools = Cat_Jobs.getTrimmedCooccurrencePairs(allSentencesByTool, allToolsBySentence, connection, IEType.TOOL, tableName);
		}
		else{
			cooccurringTools = Cat_Jobs.getCooccurrencePairs(allSentencesByTool, allToolsBySentence, connection,
					IEType.TOOL, tableName);
		}	
		allToolsBySentence = null;
		allSentencesByTool = null;
		
		Map<Pair, Double> pairs = Cat_DBConnector.readPairs(connection, tableName);
		System.out.println("--> pairs: " + pairs.size());
		System.out.println("cooccurring tools: " + cooccurringTools.size());
		
		// Gruppenbildung
		System.out.println("\nbuild cooccurrence groups");
		for (int level = 1; level <= 5; level++) {
			double minChiSquare = minChiSquares[level - 1];
			System.out.println("level " + level + " --> min. chi-square = " + minChiSquare);
			Map<Integer, List<Entity>> cooccurrenceGroups = Cat_Jobs.buildCooccurrenceGroups(cooccurringTools,
					minChiSquare, pairs);
			tableName = Cat_DBConnector.createGroupTables(connection, IEType.TOOL, Integer.toString(level),
					trimSentences, contextSize);
			Cat_DBConnector.writeGroups(connection, cooccurrenceGroups, IEType.TOOL, Integer.toString(level),
					tableName);
		}
		long after = System.currentTimeMillis();
		double time = (((double) after - before) / 1000) / 60;
		if (time > 60) {
			System.out.println("\nfinished Cooccurrence-Groups in " + (time / 60) + "hours");
		} else {
			System.out.println("\nfinished Cooccuurrence-Groups in " + time + " minutes");
		}
	}
}
