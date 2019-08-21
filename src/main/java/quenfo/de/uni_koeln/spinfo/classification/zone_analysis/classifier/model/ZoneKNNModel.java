package quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.model;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.eclipse.persistence.annotations.PrimaryKey;

import quenfo.de.uni_koeln.spinfo.classification.core.classifier.AbstractClassifier;
import quenfo.de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.ZoneKNNClassifier;

/**
 * @author geduldia
 * 
 * a model-object based on the KNNClassifier
 *
 */
@Entity
public class ZoneKNNModel extends Model {
	
	


	private static final long serialVersionUID = 1L;
	
	private Map<double[], boolean[]> trainingData = new HashMap<double[], boolean[]>();
	
	
	
	/**
	 * @return trainingData
	 */
	public Map<double[], boolean[]> getTrainingData() {
		return trainingData;
	}
	
	


	/**
	 * @param trainingData
	 */
	public void setTrainingData(Map<double[], boolean[]> trainingData){
		this.trainingData = trainingData;
	}


	public AbstractClassifier getClassifier(){
		return new ZoneKNNClassifier();
	}
	
	

}
