package de.uni_koeln.spinfo.clustering.bibbApplications;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.clustering.featureEngineering.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.clustering.featureEngineering.CooccurrenceFeatureQuantifier;
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import is2.data.SentenceData09;
import is2.lemmatizer.Lemmatizer;

public class CreateCooccurrencesDB {

	private static String inputDB = "C:/sqlite/Competences.db";

	public static void main(String[] args) throws Exception {

		// read Competences (and Contexts) from DB
		Connection inputConnection = IE_DBConnector.connect(inputDB);
		Map<ExtractionUnit, List<String>> extractionUnits = IE_DBConnector.readTrainingData(inputConnection);

		// sort contexts by competence
		Map<String, StringBuffer> contextsByCompetence = new HashMap<String, StringBuffer>();
		for (ExtractionUnit eu : extractionUnits.keySet()) {
			for (String comp : extractionUnits.get(eu)) {
				StringBuffer sb = contextsByCompetence.get(comp);
				if (sb == null)
					sb = new StringBuffer();
				sb.append(eu.getSentence() + " ");
				contextsByCompetence.put(comp, sb);
			}
		}

		// Tokenize and lemmatize contexts
		Map<String, List<String>> lemmatasByCompetence = new HashMap<String, List<String>>();
		IETokenizer tokenizer = new IETokenizer();
		is2.tools.Tool lemmatizer = new Lemmatizer(
				"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model");
		for (String comp : contextsByCompetence.keySet()) {
			SentenceData09 sd = new SentenceData09();
			sd.init(tokenizer.tokenizeSentence("<root> " + contextsByCompetence.get(comp).toString().trim()));
			lemmatizer.apply(sd);
			ArrayList<String> lemmata = new ArrayList<>(Arrays.asList(sd.plemmas));
			lemmata.remove(0);
			lemmatasByCompetence.put(comp, lemmata);
		}
		
		//get cooccurrences
		AbstractFeatureQuantifier<String> qf = new CooccurrenceFeatureQuantifier<String>();
		Map<String,double[]> vectors = qf.getFeatureVectors(lemmatasByCompetence, null);
	
		//write results in db
		IE_DBConnector.createCooccurrenceTable(inputConnection);
		IE_DBConnector.writeCooccurrences(inputConnection, vectors, qf.getFeatureUnitOrder());
	}

}
