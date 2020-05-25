package quenfo.de.uni_koeln.spinfo.categorization.applications;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import quenfo.de.uni_koeln.spinfo.categorization.data.Entity;
import quenfo.de.uni_koeln.spinfo.categorization.db_io.Cat_DBConnector;
import quenfo.de.uni_koeln.spinfo.categorization.workflow.Cat_Jobs;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;




/**
 * @author geduldia
 * 
 *   workflow to group ALL competences (= already categorized, already
 *         validated and not yet validated competences) according to their
 *         cooccurrences AND needleman-wunsch-similarity.
 * 
 *		   reads the similarity-groups and the cooccurrence-groups from DB 
 *		   and merges them to new groups. (5 rounds  -  one round for each group-level)
 *		   Stores the results in an output-DB (--> tables 'groups_1' - 'groups_5')
 *         The Group with the most group-members gets the highest groupID etc. 
 *
 */
public class GroupCompetences {
	
	//wird dem Namen der Output-DB angehängt
	private static String jahrgang = "2011";
	
	// Ordner für die Output-DB
	private static String outputFolder = "C:/sqlite/categorization/competences/";
	
	//Name der Output-DB
	private static String outputDB = "CompetenceGroups_"+jahrgang+".db";
	
	//min. Chi-Quadrat-Werte für Gruppenzugehörigkeit
	private static int[] levels = new int[]{1,2,3,4,5};
	
	//Pfad zu den DBs mit den Stringähnlichkeits-/Kookkurrenz-Gruppen
	private static String simGroupsDB = "C:/sqlite/categorization/competences/CompetenceStringSimilarities_"+jahrgang+".db";
	private static String cooccGroupsDB = "C:/sqlite/categorization/competences/CompetenceCooccurrences_"+jahrgang+".db";
	
	private static boolean trimSentences = false;
	
	private static int contextSize = 5;
	
	
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		
		long before = System.currentTimeMillis();

		if(!new File(outputFolder).exists()){
			new File(outputFolder).mkdirs();
		}
		Connection outputConnection = Cat_DBConnector.connect(outputFolder + outputDB);	
		Connection simConnection = Cat_DBConnector.connect(simGroupsDB);
		Connection cooccConnection = Cat_DBConnector.connect(cooccGroupsDB);
		
		for (int level = 1; level <= levels.length; level++) {
			System.out.println("\nbuild groups for level " + level);
			String cooccTable;
			if(trimSentences){
				cooccTable = "Groups_level_"+level+"_contextSize_"+contextSize;
			}
			else{
				cooccTable = "Groups_level_"+level;
			}
			Map<Integer,List<Entity>> cooccGroups = Cat_DBConnector.readGroups(cooccConnection, cooccTable);
			Map<Integer,List<Entity>> simGroups = Cat_DBConnector.readGroups(simConnection, "Groups_level_"+level);
			Map<Integer, List<Entity>> groups = Cat_Jobs.buildMergedGroups(cooccGroups, simGroups);
			String tableName = Cat_DBConnector.createGroupTables(outputConnection, IEType.COMPETENCE_IN_3, Integer.toString(level), trimSentences, contextSize);
			Cat_DBConnector.writeGroups(outputConnection, groups, IEType.COMPETENCE_IN_3,
					Integer.toString(level), tableName);
		}
		long after = System.currentTimeMillis();
		double time = (((double) after - before)/1000)/60;
		if(time > 60){
			System.out.println("\nfinished Groups in "+(time/60)+"hours");
		}
		else{
			System.out.println("\nfinished Groups in " + time + " minutes");
		}
	}
	

}
