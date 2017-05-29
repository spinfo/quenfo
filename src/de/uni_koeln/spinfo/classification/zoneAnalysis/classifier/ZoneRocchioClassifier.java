package de.uni_koeln.spinfo.classification.zoneAnalysis.classifier;

import java.io.File;
import java.util.List;

import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.core.distance.Distance;
import de.uni_koeln.spinfo.classification.core.distance.DistanceCalculator;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.model.ZoneRocchioModel;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;

/**
 * @author geduldig
 * 
 * a classifier based on the rocchio-algorithm
 * 
 */
public class ZoneRocchioClassifier extends ZoneAbstractClassifier {

	private static final long serialVersionUID = 1L;

	/**
	 * @param multiClass
	 * @param distance
	 *            -measure
	 */
	public ZoneRocchioClassifier(boolean multiClass, Distance distance) {
		this.distance = distance;
		this.multiClass = multiClass;
	}

	public ZoneRocchioClassifier() {
	}
	

	
	/* (non-Javadoc)
	 * @see de.uni_koeln.spinfo.bibb.jasc.classifier.AbstractClassifier#buildModel(java.util.List, de.uni_koeln.spinfo.bibb.jasc.data.FeatureUnitConfiguration, de.uni_koeln.spinfo.bibb.jasc.featureEngineering.quantifiers.AbstractFeatureQuantifier, java.io.File)
	 */
	@Override
	public Model buildModel(List<ClassifyUnit> cus, FeatureUnitConfiguration fuc, AbstractFeatureQuantifier fq, File dataFile){
		Model model = new ZoneRocchioModel();
		int numberOfClasses = ( (ZoneClassifyUnit) cus.get(0)).getClassIDs().length;
		int numberOfFeatures = cus.get(0).getFeatureVector().length;
		double[][] centers = new double[numberOfClasses][numberOfFeatures];

		for (int classID = 1; classID <= numberOfClasses; classID++) {
			double[] classCenter = new double[numberOfFeatures];
			int classMembers = 0;
			for (ClassifyUnit cu : cus) {
				/* check, if cu is member of current class */
				if (( (ZoneClassifyUnit) cu).getClassIDs()[classID - 1]) {
					classMembers++;
					double[] featureVector = cu.getFeatureVector();
					boolean isEmpty = true;
					for (double d : featureVector) {
						if (d != 0)
							isEmpty = false;
					}
					if (isEmpty) {
						System.out.println("isEmpty");
					}

					/* modify center of current class */
					for (int d = 0; d < numberOfFeatures; d++) {
						classCenter[d] += featureVector[d];
					}

				}
			}

			for (int d = 0; d < numberOfFeatures; d++) {
				if (classMembers == 0) {
					classCenter[d] = Double.MAX_VALUE;

				} else {
					classCenter[d] = classCenter[d] / ((double) classMembers);
				}

			}

			centers[classID - 1] = classCenter;
		}
		((ZoneRocchioModel) model).setCenters(centers);
		model.setClassifierName(this.getClass().getSimpleName());
		model.setFQName(fq.getClass().getSimpleName());
		model.setDataFile(dataFile);
		model.setFuc(fuc);
		model.setFUOrder(fq.getFeatureUnitOrder());
		return model;
	}

	
	/* (non-Javadoc)
	 * @see de.uni_koeln.spinfo.bibb.jasc.classifier.AbstractClassifier#classify(de.uni_koeln.spinfo.bibb.jasc.data.ClassifyUnit, de.uni_koeln.spinfo.bibb.jasc.classifier.models.AbstractModel)
	 */
	@Override
	public boolean[] classify(ClassifyUnit cu, Model model) {
		int numberOfClasses = ( (ZoneClassifyUnit) cu).getClassIDs().length;
		boolean[] toReturn = new boolean[numberOfClasses];
		if (multiClass) {
			toReturn = classifyMultiClass(cu, model);
		} else {
			toReturn = classifySingleClass(cu, model);
		}
		return toReturn;
	}

	
	private boolean[] classifySingleClass(ClassifyUnit cu, Model model) {
		
		boolean[] toReturn = new boolean[( (ZoneClassifyUnit) cu).getClassIDs().length];
		double smallestDist = Double.MAX_VALUE;
		int bestClass = 0;
		for (int classID = 1; classID <= toReturn.length; classID++) {
			
			/* calc distance to center of current class */
			double dist = DistanceCalculator.getDistance(cu.getFeatureVector(),
					((ZoneRocchioModel) model).getCenters()[classID - 1], distance);
				
			
			if (dist < smallestDist) {
				smallestDist = dist;
				bestClass = classID;
			}
			if (dist == smallestDist) {
				if (classID == defaultID) {
					smallestDist = dist;
					bestClass = classID;
				}
			}
		}

		toReturn[bestClass - 1] = true;
		return toReturn;

	}
	
	private boolean[] classifyMultiClass(ClassifyUnit cu, Model model) {
		int numberOfClasses = ((JASCClassifyUnit) cu).getClassIDs().length;
		boolean[] toReturn = new boolean[numberOfClasses];
		int vectorLength = ((ZoneRocchioModel) model).getCenters()[0].length;
		for (int classID = 1; classID <= numberOfClasses; classID++) {
			/* calc. Center of other classes (= sum of other centers) */
			double[] centerOfOther = new double[vectorLength];
			for (int c = 1; c <= numberOfClasses; c++) {
				if (c != classID) {
					for (int d = 0; d < ((ZoneRocchioModel) model).getCenters()[0].length; d++) {
						centerOfOther[d] += ((ZoneRocchioModel) model).getCenters()[c - 1][d];
					}
				}
			}
			// check if cu belongs to current classID...
			double distanceToClass = DistanceCalculator.getDistance(
					((ZoneRocchioModel) model).getCenters()[classID - 1],
					cu.getFeatureVector(), distance);
			double distanceToOthers = DistanceCalculator.getDistance(
					centerOfOther, cu.getFeatureVector(), distance);
			if (distance == Distance.COSINUS) {
				if (distanceToClass > distanceToOthers) {
					toReturn[classID - 1] = true;
				}
			} else {
				if (distanceToClass < distanceToOthers) {
					toReturn[classID - 1] = true;
				}
			}

		}
		return toReturn;
	}


}
