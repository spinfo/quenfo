package de.uni_koeln.spinfo.classification.core.classifier.model;


import java.io.File;
import java.io.Serializable;
import java.util.List;

import de.uni_koeln.spinfo.classification.core.classifier.AbstractClassifier;
import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbsoluteFrequencyFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.LogLikeliHoodFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.RelativeFrequencyFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.TFIDFFeatureQuantifier;

/**
 * @author geduldia
 * 
 * an abstract class for all models
 *  contains basic functionality of an model object
**/
 
public  class Model implements Serializable{
	
	
	private static final long serialVersionUID = 1L;

	/**
	 * the trainingSet this model is based on
	 */
	private File dataFile;
	
	/**
	 * the used FeatureUnitConfiguration
	 */
	private FeatureUnitConfiguration fuc;
	
	/**
	 * order of the FeatureUnits (translation of the Vector-Dimensions)
	 */
	private List<String> fUOrder;
	
	/**
	 * name of the corresponding classifier
	 */
	protected String classifierName;
	
	/**
	 * name of the used FeatureQuantifier
	 */
	private String fQName;
	
	
	
	
	
	/**
	 * @return  an instance of the used FeatureQuantifier (specified in FQName)
	 */
	public AbstractFeatureQuantifier getFQ(){
	
		if(fQName == null) {
			;return null;
		}
		if(fQName.equals("LogLikeliHoodFeatureQuantifier")){
			return new LogLikeliHoodFeatureQuantifier();
		}
		if(fQName.equals("TFIDFFeatureQuantifier")){
			return new TFIDFFeatureQuantifier();
		}
		if(fQName.equals("AbsoluteFrequencyFeatureQuantifier")){
			return new AbsoluteFrequencyFeatureQuantifier();
		}
		if(fQName.equals("RelativeFrequencyFeatureQuantifier")){
			return new RelativeFrequencyFeatureQuantifier();
		}
		
		return null;
	}
	
	
	

	/**
	 * @return the trainingdata-file this model is based on
	 */
	public File getDataFile() {
		return dataFile;
	}

	/**
	 * set training-data file for model
	 * @param dataFile
	 */
	public void setDataFile(File dataFile) {
		this.dataFile = dataFile;
	}

	/**
	 * @return the featureUnitConfiguration this model is based on
	 */
	public FeatureUnitConfiguration getFuc() {
		return fuc;
	}

	/**
	 * set a FeatureUnitConfiguration
	 * @param fuc
	 */
	public void setFuc(FeatureUnitConfiguration fuc) {
		this.fuc = fuc;
	}

	/**
	 * @return featureUnitOrder of model
	 */
	public List<String> getFUOrder() {
		return fUOrder;
	}

	/**
	 * set the featureUnitOrder 
	 * @param fUOrder
	 */
	public void setFUOrder(List<String> fUOrder) {
		this.fUOrder = fUOrder;
	}

	/**
	 * @return name of the classifier this model is based on
	 */
	public String getClassifierName() {
		return classifierName;
	}

	/**
	 * set a classifier(-name) 
	 * @param classifierName
	 */
	public void setClassifierName(String classifierName) {
		this.classifierName = classifierName;
	}

	/**
	 * @return name of the FeatureQuantifier this model is based on
	 */
	public String getFQName() {
		return fQName;
	}

	/**
	 * set a FeatureQuantifier(-name)
	 * @param fQName
	 */
	public void setFQName(String fQName) {
		this.fQName = fQName;
	}
	
	public  AbstractClassifier getClassifier(){
		return null;
	}
}
