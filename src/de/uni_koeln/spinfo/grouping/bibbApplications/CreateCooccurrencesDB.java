package de.uni_koeln.spinfo.grouping.bibbApplications;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.grouping.featureEngineering.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.grouping.featureEngineering.CooccurrenceFeatureQuantifier;
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import is2.data.SentenceData09;
import is2.lemmatizer.Lemmatizer;

public class CreateCooccurrencesDB {

	private static String inputDB = "C:/sqlite/Competences.db"; 
	
	
	public static void main(String[] args) throws Exception {

		//read Data from DB
		System.out.println("read data from db");
		Connection inputConnection = IE_DBConnector.connect(inputDB);
		Map<String, List<String>> lemmatasByCompetence = IE_DBConnector.readCompsAndContexts(inputConnection);	
		
		//get cooccurrences
		System.out.println("find cooccurences");
		AbstractFeatureQuantifier<String> qf = new CooccurrenceFeatureQuantifier<String>();
		Map<String,double[]> vectors = qf.getFeatureVectors(lemmatasByCompetence, null);
	
		//write results in db
		System.out.println("write coouccrences");
		IE_DBConnector.createCooccurrenceTable(inputConnection);
		IE_DBConnector.writeCooccurrences(inputConnection, vectors, qf.getFeatureUnitOrder());
	}

}
