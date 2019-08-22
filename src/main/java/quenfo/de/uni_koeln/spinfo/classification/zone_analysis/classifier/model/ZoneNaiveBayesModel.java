package quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import quenfo.de.uni_koeln.spinfo.classification.core.classifier.AbstractClassifier;
import quenfo.de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.ZoneNaiveBayesClassifier;

/**
 * @author geduldia
 * 
 * model-object based on NaiveBayesClassifier
 *
 */
@Entity
@DiscriminatorValue(value="NB")
public class ZoneNaiveBayesModel extends Model implements Serializable  {
	

	private static final long serialVersionUID = 1L;
	
	
	
	private List<NaiveBayesClassModel> classModels = new ArrayList<NaiveBayesClassModel>();
	
	/**
	 * @param classModel
	 */
	public void addClassModel(NaiveBayesClassModel classModel){
		classModels.add(classModel);
	}
	
	/**
	 * @return classModels
	 */
	public List<NaiveBayesClassModel> getClassModels(){
		return classModels;
	}
	public AbstractClassifier getClassifier(){
		return new ZoneNaiveBayesClassifier();
	}
	

}
