package de.uni_koeln.spinfo.clustering.featureEngineering;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mahout.math.stats.LogLikelihood;
import org.apache.mahout.math.stats.LogLikelihood.ScoredItem;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class LoglikeliHoodQuantifier<E> extends AbstractFeatureQuantifier<E> {

	Multiset<String> bagOfWords2 = HashMultiset.create();

	private void initialize(Map<E, List<String>> trainingdata) {
		for (E key : trainingdata.keySet()) {
			bagOfWords2.addAll((trainingdata.get(key)));
		}
	}

	@Override
	public Map<E, double[]> getFeatureVectors(Map<E, List<String>> documentsByKey, List<String> featureUnitOrder) {
		this.featureUnitOrder = featureUnitOrder;
		if (featureUnitOrder == null) {

			this.featureUnitOrder = getFeatureUnitOrder(documentsByKey.values());
			this.initialize(documentsByKey);
		}
		
		Map<E, double[]> vectors = new HashMap<E, double[]>();

		for (E key : documentsByKey.keySet()) {
			Multiset<String> bowTemp =  HashMultiset.create(bagOfWords2);
			Multiset<String> bagOfWords1 = HashMultiset.create();
			bagOfWords1.addAll(documentsByKey.get(key));
			bowTemp.removeAll(bagOfWords1);	
			List<ScoredItem<String>> llh = LogLikelihood.compareFrequencies(
					bagOfWords1, bowTemp, bagOfWords1.size(), 0.0);
			double[] featureVector = new double[this.featureUnitOrder.size()];
			
			for (int i = 0; i < this.featureUnitOrder.size(); i++) {
				double value = 0.0;
				String featureUnit = this.featureUnitOrder.get(i);	
				for (ScoredItem<String> item : llh) {
					if (item.getItem().equals(featureUnit)) {
						value = item.getScore();
					}
				}
				featureVector[i] = value;
			}
			vectors.put(key, featureVector);
		}
		return vectors;
	}

}
