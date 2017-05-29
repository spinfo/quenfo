package de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting;

import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;

/**
 * Sets the relative values of the feature units (compared to all fus of the fu containing classify unit) as feature values
 * @author geduldig
 * 
 */
public class RelativeFrequencyFeatureQuantifier extends AbstractFeatureQuantifier {

	
	
	
	@Override
	public void setFeatureValues(List<ClassifyUnit> classifyUnits, List<String> featureUnitOrder) {
		this.featureUnitOrder = featureUnitOrder;
		if(featureUnitOrder==null){
			this.featureUnitOrder = getFeatureUnitOrder(classifyUnits);
		}
		
		for (ClassifyUnit classifyUnit : classifyUnits) {
			Map<String,Integer> termFrequencies = getTermFrequencies(classifyUnit.getFeatureUnits());
			double[] featureVector = new double[this.featureUnitOrder.size()];
			int i = 0;
			double ntf = 0;
			for (String featureUnit : this.featureUnitOrder) {
				if(termFrequencies.keySet().contains(featureUnit)){
					ntf = termFrequencies.get(featureUnit)/(double)this.featureUnitOrder.size();
				}
				featureVector[i++] = ntf;
			}
			classifyUnit.setFeatureVector(featureVector);
		}
	}



}
