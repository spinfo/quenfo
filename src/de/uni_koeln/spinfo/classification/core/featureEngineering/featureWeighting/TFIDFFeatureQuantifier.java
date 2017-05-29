package de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;

/**
 * Calculates TF/IDF values of each feature unit and sets them as feature values.
 * @author geduldig
 *
 */
public class TFIDFFeatureQuantifier extends AbstractFeatureQuantifier{
	
	


	
	@Override
	public void setFeatureValues(List<ClassifyUnit> classifyUnits, List<String> featureUnitOrder){
		this.featureUnitOrder = featureUnitOrder;
		if(featureUnitOrder==null){
			this.featureUnitOrder = getFeatureUnitOrder(classifyUnits);
		}
		
		Map<String, Integer> docFrequencies = calcDocFrequencies(this.featureUnitOrder);
		for (ClassifyUnit unitToClassify : classifyUnits) {
			Map<String, Integer> termFrequencies = getTermFrequencies(unitToClassify.getFeatureUnits());
			double[] vector = new double[this.featureUnitOrder.size()];
			int i = 0;
			for (String featureUnit : this.featureUnitOrder) {
				double tfidf = 0;
				if (termFrequencies.containsKey(featureUnit)) {
					double ntf = ((double)(termFrequencies.get(featureUnit)) / maxTF);
					double idf =  (double)Math.log((double)(classifyUnits.size())/(docFrequencies.get(featureUnit)));
					tfidf = ntf * idf;
				}
				vector[i++] = tfidf;
			}
			
			unitToClassify.setFeatureVector(vector);
		}
	
	}
	
	

	private Map<String, Integer> calcDocFrequencies(List<String> featureUnitOrder) {
		Map<String, Integer> toReturn = new TreeMap<String, Integer>();
		
			for (String featureUnit : featureUnitOrder) {
				if (toReturn.containsKey(featureUnit)) {
					int df = toReturn.get(featureUnit);
					toReturn.put(featureUnit, df + 1);
				} else {
					toReturn.put(featureUnit, 1);
				}
			}

		return toReturn;
	}




}
