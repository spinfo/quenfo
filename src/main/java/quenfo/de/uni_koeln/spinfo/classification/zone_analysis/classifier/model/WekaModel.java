package quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.model;

import quenfo.de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import weka.core.Instances;

public class WekaModel extends Model{
	
	private static final long serialVersionUID = -3254990813492590623L;
	
	private Instances trainingData;
	
	/**
	 * @return trainingData
	 */
	public Instances getTrainingData() {
		return trainingData;
	}

	/**
	 * @param trainingData
	 */
	public void setTrainingData(Instances trainingData){
		this.trainingData = trainingData;
	}
	

}
