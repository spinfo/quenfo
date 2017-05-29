package de.uni_koeln.spinfo.normalization;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_koeln.spinfo.information_extraction.data.IEType;
import de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;

public class CalcSimilaritiesApp {
	
	public static String inputDB = "C:/sqlite/Competences.db";
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		
		//connect to input-db
		Connection inputConnection = null;
		if (!new File(inputDB).exists()) {
			System.out
					.println("Database don't exists " + inputDB + "\nPlease change configuration and start again.");
			System.exit(0);
		} else {
			inputConnection = IE_DBConnector.connect(inputDB);
		}
		
		//create new table  'similarCompetences'
		IE_DBConnector.createSimilarityTable(inputConnection);
		
		//read Competences from DB
		Set<String> competenceSet = IE_DBConnector.readEntities(inputConnection, IEType.COMPETENCE);

		//calc. similarities
		SimilarityCalculator dc = new SimilarityCalculator();
		dc.setVocabulary(competenceSet);
		dc.calcSimilarityMatrix();
		for (String string : competenceSet) {
			List<String> sims = dc.getSimilaritiesForString(string);
			IE_DBConnector.writeSimilarities(string, sims, inputConnection);
		}

	}

	

}
