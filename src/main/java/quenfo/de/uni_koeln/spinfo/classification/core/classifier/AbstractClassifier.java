package quenfo.de.uni_koeln.spinfo.classification.core.classifier;

import java.io.File;
import java.util.List;

import quenfo.de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import quenfo.de.uni_koeln.spinfo.classification.core.distance.Distance;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_weighting.AbstractFeatureQuantifier;



/**
 * @author geduldia
 *
 *An abstract class for all classifiers
 *contains basic functionality of a classifier
 *
 */
public abstract class AbstractClassifier {
	
	protected String classifierConfig;
	protected Distance distance;
	

	/**
	 * 
	 * @param cus List of ClassifyUnits
	 * @param fuc FeatureUnitConfiguration
	 * @param fq FeatureQuantifier
	 * @param trainingDataFile 
	 * @return
	 */
	public abstract Model buildModel(List<ClassifyUnit> cus, FeatureUnitConfiguration fuc,AbstractFeatureQuantifier fq, File trainingDataFile);
	
	/**
	 * @param distance measure
	 */
	public  void setDistance(Distance distance){
		this.distance = distance;
	}
	
	/**
	 * @return distance measure
	 */
	public  Distance getDistance(){
		return distance;
	}
	
	/**
	 * @return classifierConfig
	 */
	public String getClassifierConfig(){
		return classifierConfig;
	}

}
