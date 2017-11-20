package de.uni_koeln.spinfo.grouping.featureEngineering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public  abstract class AbstractFeatureQuantifier<E> {
	
	int maxTF = 0;
	List<String> featureUnitOrder;
	
	public abstract Map<E,double[]> getFeatureVectors(Map<E,List<String>> documentsByKey, List<String> featureUnitOrder);
	
	protected List<String> getFeatureUnitOrder(Collection<List<String>> documents ) {
		Set<String> uniqueFeatures = new HashSet<String>();
		for (List<String> doc : documents) {
			for (String lemma : doc) {
				uniqueFeatures.add(lemma);
			}
		}
		List<String> toReturn = new ArrayList<>(uniqueFeatures);
		return toReturn;
	}
	
	public List<String> getFeatureUnitOrder(){
		return featureUnitOrder;
	}
	
	public Map<String,Integer> getDocumentFrequencies(Map<E,List<String>> documentsByKey){
		Map<String, Integer> docFrequencies = new HashMap<String,Integer>();
		for (String feature : featureUnitOrder) {
			int count = 0;
			for (List<String> doc : documentsByKey.values()) {
				if(doc.contains(feature)){
					count++;
				}
			}
			docFrequencies.put(feature, count);
		}
		return docFrequencies;
	}
	
	public Map<E,Map<String,Integer>> getTermFrequencies(Map<E, List<String>> documentsByKey){
		Map<E, Map<String,Integer>> termFrequencies = new HashMap<E, Map<String,Integer>>();
		for (E key : documentsByKey.keySet()) {
			Map<String,Integer> counts = new HashMap<String,Integer>();
			for (String lemma : documentsByKey.get(key)) {
				Integer count = counts.get(lemma);
				if(count == null) count = 0;
				count++;
				if(count > maxTF) maxTF = count;
				counts.put(lemma, count);
			}
			termFrequencies.put(key, counts);
		}
		return termFrequencies;
	}
	


}
