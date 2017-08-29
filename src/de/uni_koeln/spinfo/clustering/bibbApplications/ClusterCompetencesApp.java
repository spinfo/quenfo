package de.uni_koeln.spinfo.clustering.bibbApplications;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbsoluteFrequencyFeatureQuantifier;
import de.uni_koeln.spinfo.clustering.featureEngineering.AbsoluteFrequencyQuantifier;
import de.uni_koeln.spinfo.clustering.featureEngineering.FeatureQuantifier;
import de.uni_koeln.spinfo.clustering.featureEngineering.LoglikeliHoodQuantifier;
import de.uni_koeln.spinfo.clustering.featureEngineering.RelativeFrequencyQuantifier;
import de.uni_koeln.spinfo.clustering.featureEngineering.TFIDFFeatureQuantifier;
import de.uni_koeln.spinfo.clustering.weka.ArffFileCreator;
import de.uni_koeln.spinfo.clustering.weka.WekaClusterer;
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import is2.data.SentenceData09;
import is2.lemmatizer.Lemmatizer;
import weka.clusterers.SimpleKMeans;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instances;
import weka.core.ManhattanDistance;
import weka.core.SelectedTag;

public class ClusterCompetencesApp {

	private static String inputDB = "C:/sqlite/Competences.db";
	
	//private static String outputDB = "C:/sqlite/ClusteredCompetences.db";

	//private static AbstractFeatureQuantifier<String> quantifier = new LoglikeliHoodQuantifier<String>();
	//private static AbstractFeatureQuantifier<String> quantifier = new RelativeFrequencyQuantifier<String>();
	//private static FeatureQuantifier<String> quantifier = new AbsoluteFrequencyQuantifier<String>();
	//private static AbstractFeatureQuantifier<String> quantifier = new TFIDFFeatureQuantifier<String>();

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
		System.out.println("");
		
		//get cooccurrences
		FeatureQuantifier<String> qf = new FeatureQuantifier<>();
		Map<String,Map<String, Integer>> tfs = qf.getTermFrequencies(lemmatasByCompetence);
		Map<String, Map<String,Integer>> cooccurrences = new HashMap<String, Map<String,Integer>>();
		for (String comp : tfs.keySet()) {
			for (String lemma : tfs.get(comp).keySet()) {
				if(lemma.equals(comp))continue;
				if(lemmatasByCompetence.keySet().contains(lemma)){
					Map<String,Integer> map = cooccurrences.get(comp);
					if(map == null) map = new HashMap<String,Integer>();
					map.put(lemma, tfs.get(comp).get(lemma));
					cooccurrences.put(comp, map);
				}
			}
		}
		//write results in db
		IE_DBConnector.createCooccurrenceTable(inputConnection);
		IE_DBConnector.writeCooccurrences(inputConnection, cooccurrences);
		
		for (String comp : cooccurrences.keySet()) {
			System.out.println(comp);
			for (String co : cooccurrences.get(comp).keySet()) {
				System.out.println(co + "("+cooccurrences.get(comp).get(co)+")");
			}
			System.out.println();
		}

//		// calc featureVectors
//		Map<String, double[]> vectors = quantifier.getFeatureVectors(lemmatasByCompetence, null);
//		
//
//		//create Clusterer
//		SimpleKMeans kmeans = new SimpleKMeans(); // new instance of clusterer
//		kmeans.setNumClusters(4);
//		kmeans.setPreserveInstancesOrder(true);
//		kmeans.setInitializationMethod(new SelectedTag(SimpleKMeans.RANDOM, SimpleKMeans.TAGS_SELECTION));
//		kmeans.setDistanceFunction(new ManhattanDistance());
//		kmeans.setMaxIterations(100);
//		WekaClusterer clusterer = new WekaClusterer(kmeans, vectors);
//		clusterer.cluster();
//		
//		//write results in db
//		Connection outputConnection = IE_DBConnector.connect(outputDB);
//		IE_DBConnector.createCluserTable(outputConnection);
//		int i = 0;
//		for (String comp : vectors.keySet()) {
//			int clusterID = clusterer.getClusterForInstance(i);
//			IE_DBConnector.writeClusterResult(clusterID, comp, outputConnection);
//			i++;
//		}
	}

}
