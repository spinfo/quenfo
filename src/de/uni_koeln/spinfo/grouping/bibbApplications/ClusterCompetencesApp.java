package de.uni_koeln.spinfo.grouping.bibbApplications;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.grouping.featureEngineering.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.grouping.featureEngineering.CooccurrenceFeatureQuantifier;
import de.uni_koeln.spinfo.grouping.featureEngineering.LoglikeliHoodQuantifier;
import de.uni_koeln.spinfo.grouping.featureEngineering.TFIDFFeatureQuantifier;
import de.uni_koeln.spinfo.grouping.weka.WekaClusterer;
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import is2.data.SentenceData09;
import is2.lemmatizer.Lemmatizer;
import weka.clusterers.SimpleKMeans;
import weka.core.ManhattanDistance;
import weka.core.SelectedTag;

public class ClusterCompetencesApp {

	private static String inputDB = "C:/sqlite/Competences.db";

//	private static AbstractFeatureQuantifier<String> quantifier = new LoglikeliHoodQuantifier<String>();
//	private static AbstractFeatureQuantifier<String> quantifier = new RelativeFrequencyQuantifier<String>();
//	private static FeatureQuantifier<String> quantifier = new AbsoluteFrequencyQuantifier<String>();
//	private static AbstractFeatureQuantifier<String> quantifier = new TFIDFFeatureQuantifier<String>();
	private static AbstractFeatureQuantifier<String> quantifier = new CooccurrenceFeatureQuantifier<>();

	public static void main(String[] args) throws Exception {


		
		//read Data from DB
		System.out.println("read data from db");
		Connection inputConnection = IE_DBConnector.connect(inputDB);
		Map<String, List<String>> lemmatasByCompetence= IE_DBConnector.readCompsAndContexts(inputConnection);	

		
		// calc featureVectors and create Clusterer
		System.out.println("calc. clusters");
		Map<String, double[]> vectors = quantifier.getFeatureVectors(lemmatasByCompetence, null);
		SimpleKMeans kmeans = new SimpleKMeans(); // new instance of clusterer
		kmeans.setNumClusters(60);
		kmeans.setPreserveInstancesOrder(true);
		kmeans.setInitializationMethod(new SelectedTag(SimpleKMeans.RANDOM, SimpleKMeans.TAGS_SELECTION));
		kmeans.setDistanceFunction(new ManhattanDistance());
		kmeans.setMaxIterations(100);
		WekaClusterer clusterer = new WekaClusterer(kmeans, vectors);
		clusterer.cluster();

		
		//write results in db
		System.out.println("write result in db");
		IE_DBConnector.createCluserTable(inputConnection);
		int i = 0;
		for (String comp : vectors.keySet()) {
			int clusterID = clusterer.getClusterForInstance(i);
			IE_DBConnector.writeClusterResult(clusterID, comp, inputConnection);
			i++;
		}
	}

}
