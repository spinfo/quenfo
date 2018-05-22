package quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.model;

import quenfo.de.uni_koeln.spinfo.classification.core.classifier.AbstractClassifier;
import quenfo.de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.ZoneRocchioClassifier;


/**
 * @author geduldia
 * 
 * model-object based on RocchioClassifier
 *
 */
public class ZoneRocchioModel extends Model{

	
	private static final long serialVersionUID = 1L;
	
	private double[][] centers;

	/**
	 * @return centers - the prototypes for each class
	 */
	public double[][] getCenters() {
		return centers;
	}

	/**
	 * 
	 * @param centers
	 */
	public void setCenters(double[][] centers) {
		this.centers = centers;
	}

	public AbstractClassifier getClassifier(){
		return new ZoneRocchioClassifier();
	}
	
}
