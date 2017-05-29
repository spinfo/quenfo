package de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting;

import java.util.List;

import org.apache.mahout.math.stats.LogLikelihood;
import org.apache.mahout.math.stats.LogLikelihood.ScoredItem;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;

/**
 * Calculates log likelihood values for each feature unit and sets them as feature values
 * bag of words 1: All feature units of ClassifyUnit with feature unit
 * bag of words 2: All feature units of all other ClassifyUnits
 * @author geduldig
 *
 */


public class LogLikeliHoodFeatureQuantifier extends AbstractFeatureQuantifier {
	
	
	
	Multiset<String> bagOfWords2 = HashMultiset.create();
	
	private void initialize(List<ClassifyUnit> trainingdata){
		
		for (ClassifyUnit classifyUnit : trainingdata) {
			bagOfWords2.addAll((classifyUnit.getFeatureUnits()));
		}
		
	}


	
	@Override
	public void setFeatureValues(List<ClassifyUnit> classifyUnits, List<String> featureUnitOrder) {
		
		this.featureUnitOrder = featureUnitOrder;
		if(featureUnitOrder==null){
			
			this.featureUnitOrder = getFeatureUnitOrder(classifyUnits);
			this.initialize(classifyUnits);
		}

		for (ClassifyUnit classifyUnit : classifyUnits) {
			Multiset<String> bowTemp =  HashMultiset.create(bagOfWords2);
			Multiset<String> bagOfWords1 = HashMultiset.create();
			bagOfWords1.addAll(classifyUnit.getFeatureUnits());
		//	System.out.println("bow1 size: " + bagOfWords1.size());
			bowTemp.removeAll(bagOfWords1);	
		//	System.out.println("bowTemp: " + bowTemp.size());
			
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
			classifyUnit.setFeatureVector(featureVector);
		}
		
	}






}
