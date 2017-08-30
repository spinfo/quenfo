package de.uni_koeln.spinfo.clustering.featureEngineering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CooccurrenceFeatureQuantifier<E> extends AbstractFeatureQuantifier<String>{

	@Override
	public Map<String, double[]> getFeatureVectors(Map<String, List<String>> documentsByKey,
			List<String> keyOrder) {
		this.featureUnitOrder = keyOrder;
		if(keyOrder == null) this.featureUnitOrder = new ArrayList<String>(documentsByKey.keySet());
		Map<String,double[]> toReturn = new HashMap<String,double[]>();
		Map<String,Map<String,Integer>> tfs = getTermFrequencies(documentsByKey);
		for (String comp : documentsByKey.keySet()) {
			double[] vector = new double[featureUnitOrder.size()];
			int i = 0;
			for (String other : featureUnitOrder) {
				vector[i] = 0;
				if(tfs.get(comp).containsKey(other)){
					vector[i] = tfs.get(comp).get(other);
				}
				i++;
			}
			toReturn.put(comp, vector);
		}
		return toReturn;
	}



}




//Map<String,Map<String,double[]>> toReturn = new HashMap<String,Map<String,double[]>>();
//
//Map<String,Map<String,Integer>> tfs = (Map<String, Map<String, Integer>>) getTermFrequencies(documentsByKey);
//Map<String, Map<String,Integer>> cooccurrences = new HashMap<String, Map<String,Integer>>();
//for (String comp : tfs.keySet()) {
//	double[] vector = new double[documentsByKey.keySet().size()];
//	int i = 0;
//	for (String lemma : tfs.get(comp).keySet()) {
//		if(documentsByKey.keySet().contains(lemma)){
//			Map<String,Integer> map = cooccurrences.get(comp);
//			if(map == null) map = new HashMap<String,Integer>();
//			map.put(lemma, tfs.get(comp).get(lemma));
//			cooccurrences.put(comp, map);
//			vector[i] = tfs.get(comp).get(lemma);
//		}
//	}
