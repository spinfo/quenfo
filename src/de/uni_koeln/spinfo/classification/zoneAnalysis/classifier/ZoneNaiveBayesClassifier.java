package de.uni_koeln.spinfo.classification.zoneAnalysis.classifier;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.model.NaiveBayesClassModel;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.model.ZoneNaiveBayesModel;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;


/**
 * @author geduldia
 * 
 * a classifier based on  naive bayes algorithm
 *
 */
public class ZoneNaiveBayesClassifier extends ZoneAbstractClassifier {

	private static final long serialVersionUID = 1L;
	/* decision-criterion for multiclass classification */
	private int criticalValue = -1;

	/**
	 * @param multiClass
	 * @param criticalValue
	 */
	public ZoneNaiveBayesClassifier(boolean multiClass, int criticalValue) {
		this.multiClass = multiClass;
		this.criticalValue = criticalValue;
	}

	public ZoneNaiveBayesClassifier() {
	}

	/**
	 * @param value
	 */
	public void setCriticalValue(int value) {
		this.criticalValue = value;
	}

	/**
	 * @return criticalValue
	 */
	public int getCriticalValue() {
		return criticalValue;
	}


	/* (non-Javadoc)
	 * @see de.uni_koeln.spinfo.bibb.jasc.classifier.AbstractClassifier#buildModel(java.util.List, de.uni_koeln.spinfo.bibb.jasc.data.FeatureUnitConfiguration, de.uni_koeln.spinfo.bibb.jasc.featureEngineering.quantifiers.AbstractFeatureQuantifier, java.io.File)
	 */
	@Override
	public Model buildModel(List<ClassifyUnit> cus, FeatureUnitConfiguration fuc, AbstractFeatureQuantifier fq, File dataFile){
		Model model = new ZoneNaiveBayesModel();
		int numberOfClasses = ( (ZoneClassifyUnit) cus.get(0)).getClassIDs().length;
		/* create a model for each class */
		for (int c = 0; c < numberOfClasses; c++) {
			NaiveBayesClassModel classModel = new NaiveBayesClassModel();
			int membersInClass = 0;
			int membersNotInClass = 0;
			/* DocFrequencies in Class */
			Map<String, Integer> inClassDFs = new HashMap<String, Integer>();
			/* DocFrequencies not in Class */
			Map<String, Integer> notInClassDFs = new HashMap<String, Integer>();

			for (ClassifyUnit cu : cus) {
				Set<String> uniqueFUs = new TreeSet<String>();
				uniqueFUs.addAll(cu.getFeatureUnits());
				boolean[] classes = ( (ZoneClassifyUnit) cu).getClassIDs();
				if (classes[c]) {
					membersInClass++;
					/* update DocFrequencies */
					for (String fu : uniqueFUs) {
						int df = 0;
						if (inClassDFs.containsKey(fu)) {
							df = inClassDFs.get(fu);
						}
						inClassDFs.put(fu, df + 1);
					}
				} else {
					membersNotInClass++;
					/* update DocFrequencies */
					for (String fu : uniqueFUs) {
						int df = 0;
						if (notInClassDFs.containsKey(fu)) {
							df = notInClassDFs.get(fu);
						}
						notInClassDFs.put(fu, df + 1);
					}
				}

			}
			/* set model properties */
			/* prob. for class c */
			double classProbability = (float) membersInClass / cus.size();
			classModel.setClassProbability(classProbability);
			classModel.setMembersInClass(membersInClass);
			classModel.setMembersNotInClass(membersNotInClass);
			classModel.setInClassDFs(inClassDFs);
			classModel.setNotInClassDFs(notInClassDFs);
			((ZoneNaiveBayesModel) model).addClassModel(classModel);
		}
		model.setClassifierName(this.getClass().getSimpleName());
		if(fq!=null){
			model.setFQName(fq.getClass().getSimpleName());
			model.setFUOrder(fq.getFeatureUnitOrder());
		}
		
		model.setDataFile(dataFile);
		model.setFuc(fuc);
		return model;
	}

	

	
	private double classify(ClassifyUnit cu, int classID, Model model) {
		int numberOfClasses = ( (ZoneClassifyUnit) cu).getClassIDs().length;
		NaiveBayesClassModel classModel = ((ZoneNaiveBayesModel) model)
				.getClassModels().get(classID - 1);

		// P(cu|Class)
		double probCUClass = 1d;
		// P(cu|notClass)
		double probCUNotClass = 1d;
		for (String fu : cu.getFeatureUnits()) {
			// P(fu|class) = Anzahl der CUs aus 'class', die fu enthalten/
			// Anzahl aller CUs in 'class'
			Map<String, Integer> dfs = classModel.getInClassDFs();
			Integer df = dfs.get(fu);
			if (df == null)
				df = 0;
			// smoothed
			double probFUClass = ((double) df + 1)
					/ (classModel.getMembersInClass() + numberOfClasses);
			if (probFUClass <= 0.0) {
				System.out.println("probFUClass: " + probFUClass);
			}

			// P(fu|notClass) = Anzahl der CUs aus 'notClass', die fu enthalten/
			// Anzahl aller CUs in 'notClass'
			df = classModel.getNotInClassDFs().get(fu);
			if (df == null)
				df = 0;
			// smoothed
			double probFUNotClass = ((double) df + 1)
					/ (classModel.getMembersNotInClass() + numberOfClasses);
			if (probFUNotClass == 0.0) {
				System.out.println("probFUNotInclass: " + probFUNotClass);
			}
			probCUClass = probCUClass * probFUClass;

			probCUNotClass = probCUNotClass * probFUNotClass;

		}
		// P(class)
		double probClass = ((float) classModel.getMembersInClass())
				/ (classModel.getMembersInClass() + classModel
						.getMembersNotInClass());
		double probNotClass = ((float) classModel.getMembersNotInClass())
				/ (classModel.getMembersInClass() + classModel
						.getMembersNotInClass());
		double toReturn;
		double zaehler = ((double) probCUClass) * probClass;
		double nenner = probCUNotClass * probNotClass;
		if (zaehler == 0.0) {
			zaehler = Double.MIN_VALUE;
		}
		if (nenner == 0.0) {
			nenner = Double.MIN_VALUE;
		}
		toReturn = zaehler / nenner;
		return toReturn;
	}


	
	/* (non-Javadoc)
	 * @see de.uni_koeln.spinfo.bibb.jasc.classifier.AbstractClassifier#classify(de.uni_koeln.spinfo.bibb.jasc.data.ClassifyUnit, de.uni_koeln.spinfo.bibb.jasc.classifier.models.AbstractModel)
	 */
	@Override
	public boolean[] classify(ClassifyUnit cu, Model model) {
		int numberOfClasses = ( (ZoneClassifyUnit) cu).getClassIDs().length;
		boolean[] toReturn = new boolean[numberOfClasses];
		double bestProb = Double.MIN_VALUE;
		int bestClass = -1;
		for (int classID = 1; classID <= numberOfClasses; classID++) {
			double currentProb = classify(cu, classID, model);
			if (multiClass) {
				if (currentProb > criticalValue) {
					toReturn[classID - 1] = true;
				}
			}

			else {

				if (currentProb > bestProb) {
					bestProb = currentProb;
					bestClass = classID;
				}
				if (currentProb == bestProb) {
					if (classID == defaultID) {
						bestClass = defaultID;
					}
				}
			}

		}
		if (!multiClass) {
			if (bestClass != (-1)) {
				toReturn[bestClass - 1] = true;
			}

		}
		if (bestClass < 0) {
			System.out.println("bestProb: " + bestProb);
		}
		return toReturn;
	}

	

}
