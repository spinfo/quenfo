package de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting;

import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;

/**
 * Sets the absolute values of the feature units as feature values
 * @author geduldig 
 *
 */
public class AbsoluteFrequencyFeatureQuantifier extends AbstractFeatureQuantifier{



	
	@Override
	public void setFeatureValues(List<ClassifyUnit> classifyUnits, List<String> featureUnitOrder){
		this.featureUnitOrder = featureUnitOrder;
		if(featureUnitOrder == null){
			this.featureUnitOrder = getFeatureUnitOrder(classifyUnits);
		}

		for (ClassifyUnit classifyUnit : classifyUnits) {
			Map<String,Integer> termFrequencies = getTermFrequencies(classifyUnit.getFeatureUnits());
			double[] featureVector = new double[this.featureUnitOrder.size()];
			double tf = 0;
			int i = 0;
			for (String featureUnit : this.featureUnitOrder) {
				if(termFrequencies.keySet().contains(featureUnit)){
					tf = termFrequencies.get(featureUnit);
				}
				featureVector[i++] = tf;
			}
			classifyUnit.setFeatureVector(featureVector);
		}
	}


	

}
