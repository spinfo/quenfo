package de.uni_koeln.spinfo.clustering.featureEngineering;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbsoluteFrequencyQuantifier<E> extends AbstractFeatureQuantifier<E> {

	@Override
	public Map<E, double[]> getFeatureVectors(Map<E, List<String>> documentsByKey, List<String> featureUnitOrder) {
		// set FeatureUnitOrder
		this.featureUnitOrder = featureUnitOrder;
		if (featureUnitOrder == null) {
			this.featureUnitOrder = getFeatureUnitOrder(documentsByKey.values());
		}
		// calc termFrequencies
		Map<E, Map<String, Integer>> termFrequencies = getTermFrequencies(documentsByKey);

		Map<E, double[]> vectors = new HashMap<E, double[]>();

		for (E key : documentsByKey.keySet()) {
			double[] vector = new double[this.featureUnitOrder.size()];
			for (int i = 0; i < this.featureUnitOrder.size(); i++) {
				String feature = this.featureUnitOrder.get(i);
				if (termFrequencies.get(key).containsKey(feature)) {
					vector[i] = termFrequencies.get(key).get(feature);
				}
			}
			vectors.put(key, vector);
		}
		return vectors;
	}

}
