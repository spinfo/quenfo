package quenfo.de.uni_koeln.spinfo.core.application;

import java.io.IOException;
import java.sql.SQLException;

import quenfo.de.uni_koeln.spinfo.classification.applications.ClassifyDatabase;
import quenfo.de.uni_koeln.spinfo.information_extraction.applicationsjb.ExtractNewCompetences;
import quenfo.de.uni_koeln.spinfo.information_extraction.applicationsjb.ExtractNewTools;
import quenfo.de.uni_koeln.spinfo.information_extraction.applicationsjb.MatchCompetences;
import quenfo.de.uni_koeln.spinfo.information_extraction.applicationsjb.MatchTools;

public class Launcher {

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		
		switch (args[0]) {
		case "classify":
			ClassifyDatabase.main(args);
			break;
		case "extractCompetences":
			ExtractNewCompetences.main(args);
			break;
		case "extractTools":
			ExtractNewTools.main(args);
			break;
		case "matchCompetences":
			MatchCompetences.main(args);
			break;
		case "matchTools":
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
