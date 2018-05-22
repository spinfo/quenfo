package quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier;

import java.io.Serializable;

import quenfo.de.uni_koeln.spinfo.classification.core.classifier.AbstractClassifier;
import quenfo.de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;

/**
 * @author geduldia
 * 
 * 
 * Class containing base functionality of classifiers
 */
public abstract class ZoneAbstractClassifier extends AbstractClassifier implements Serializable{ 
	
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected boolean multiClass;
	protected int defaultID;

	
	


	
	/**
	 * @return returns true if works with multiclasses
	 */
	public  boolean getMultiClass(){
		return multiClass;
	}
	
	/**
	 * @param multiClass
	 */
	public  void setMultiClass(boolean multiClass){
		this.multiClass = multiClass;
	}
	
	/**
	 * @param defaultID - only relevant for singleclass-classification - if two or more classes have the same prob. the classifier prefers the default-class
	 */
	public  void setDefaultClassID(int defaultID){
		this.defaultID = defaultID;
	}
	
	/**
	 * @return defaultClassID
	 */
	public int getDeafultClassID(){
		return defaultID;
	}

	
	/**
	 * @param cu
	 * @param model
	 * @return classIDs 
	 */
	
	public abstract  boolean[] classify(ClassifyUnit cu, Model model);

	
	


	
}
