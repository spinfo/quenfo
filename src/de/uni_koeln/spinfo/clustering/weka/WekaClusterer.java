package de.uni_koeln.spinfo.clustering.weka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import weka.clusterers.Clusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;

public class WekaClusterer {

	private Clusterer wekaClusterer;
	private Instances data;

	public WekaClusterer(Clusterer wekaClusterer, Map<String, double[]> vectors) throws IOException {
		this.wekaClusterer = wekaClusterer;
		// create Instances
		ArffFileCreator.createArffFile(vectors, new File("clustering/vectors.arff"));
		BufferedReader reader = new BufferedReader(new FileReader("clustering/vectors.arff"));
		this.data = new Instances(reader);
		reader.close();
	}

	public void cluster() throws Exception {
		wekaClusterer.buildClusterer(data); // build the clusterer}		
	}

	public int getClusterForInstance(int i) throws Exception {
		return wekaClusterer.clusterInstance(data.get(i));
	}
	
	
}
