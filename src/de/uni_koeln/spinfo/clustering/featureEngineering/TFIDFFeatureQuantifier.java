package de.uni_koeln.spinfo.clustering.featureEngineering;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TFIDFFeatureQuantifier<E> extends FeatureQuantifier<E> {

	public Map<E, double[]> getFeatureVectors(Map<E, List<String>> documentsByKey, List<String> featureUnitOrder) {
		//set FeatureUnitOrder
		this.featureUnitOrder = featureUnitOrder;
		if(featureUnitOrder == null){
			this.featureUnitOrder = getFeatureUnitOrder(documentsByKey.values());
		}	
		//calc termFrequencies
		Map<E,Map<String,Integer>> termFrequencies = getTermFrequencies(documentsByKey);
		//calc docFrequencies
		Map<String,Integer> docFrequencies = getDocumentFrequencies(documentsByKey);
		
		Map<E,double[]> vectors = new HashMap<E, double[]>();
		//tf-idf calculation
		for (E key : documentsByKey.keySet()) {
			double[] vector = new double[this.featureUnitOrder.size()];
			for (int i = 0; i < this.featureUnitOrder.size(); i++) {
				String feature = this.featureUnitOrder.get(i);
				if(termFrequencies.get(key).containsKey(feature)){
					int tf = termFrequencies.get(key).get(feature);
					int df = docFrequencies.get(feature);
					double idf = (double)Math.log((double)documentsByKey.size()/df);
					double ntf = ((double)(tf) / maxTF);
					double tfidf = ntf * idf;
					vector[i] = tfidf;
				}	
			}
			vectors.put(key, vector);
		}
		return vectors;
	}

}
