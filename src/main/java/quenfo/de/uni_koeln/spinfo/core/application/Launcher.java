package quenfo.de.uni_koeln.spinfo.core.application;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import quenfo.de.uni_koeln.spinfo.classification.applications.ClassifyDatabase;
import quenfo.de.uni_koeln.spinfo.information_extraction.applications.ExtractNewCompetences;
import quenfo.de.uni_koeln.spinfo.information_extraction.applications.ExtractNewTools;
import quenfo.de.uni_koeln.spinfo.information_extraction.applications.MatchCompetences;
import quenfo.de.uni_koeln.spinfo.information_extraction.applications.MatchTools;

/**
 * Class contains an application to run different workflows from command line
 * workflows: classify, extractCompetences, extractTools, matchCompetences, matchTools
 * @author Johanna Binnewitt
 *
 */
public class Launcher {
	
	static Logger log = Logger.getLogger(Launcher.class);

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		
		switch (args[0]) {
		case "classify":
			log.info("--- Classification starts ---");
			ClassifyDatabase.main(args);
			break;
		case "extractCompetences":
			log.info("--- Extracting competences starts ---");
			ExtractNewCompetences.main(args);
			break;
		case "extractTools":
			log.info("--- Extracting tools starts ---");
			ExtractNewTools.main(args);
			break;
		case "matchCompetences":
			log.info("--- Matching competences starts ---");
			MatchCompetences.main(args);
			break;
		case "matchTools":
			log.info("--- Matching tools starts ---");
			MatchTools.main(args);
			break;
		default:
			System.out.println(args[0] + " is not available. Please choose\n"
					+ "classify\n"
					+ "extractCompetences\n"
					+ "extractTools\n"
					+ "matchCompetences\n"
					+ "matchTools");
			break;
		}
		

	}

}
