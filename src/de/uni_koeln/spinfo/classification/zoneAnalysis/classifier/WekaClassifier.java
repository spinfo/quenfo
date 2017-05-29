/**
 * Material for the course 'Text-Engineering', University of Cologne.
 * (http://www.spinfo.phil-fak.uni-koeln.de/spinfo-textengineering.html)
 * <p/>
 * Copyright (C) 2008-2009 Fabian Steeg
 * <p/>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.uni_koeln.spinfo.classification.zoneAnalysis.classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.model.WekaModel;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;

/**
 * Text classification delegating the actual classification to a classifier
 * strategy.
 * @author jhermes
 */
public class WekaClassifier extends ZoneAbstractClassifier {
	
	private Classifier wekaClassifier;
	

	public WekaClassifier (Classifier c){
		this.wekaClassifier = c;
	}

	
	public Classifier getClassifier(){
		return wekaClassifier;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param classifier
	 *            The classifier strategy to use for text classification
	 * @param trainingSet
	 *            The training set for this classifier
	 */

	
	@Override
	public Model buildModel(List<ClassifyUnit> cus,
			FeatureUnitConfiguration fuc, AbstractFeatureQuantifier fq,
			File trainingDataFile) {
		
		Instances trainingSet = initTrainingSet(cus);
		for (ClassifyUnit classifyUnit : cus) {
			trainingSet.add(instance(((ZoneClassifyUnit)classifyUnit), trainingSet));
		}
		
//		// wir merken uns, dass das Training noch nicht abgeschlossen ist ...
//		classifierBuilt = false;
		WekaModel model = new WekaModel();
		
		model.setTrainingData(trainingSet);		
		model.setClassifierName(this.getClass().getSimpleName());
		model.setFQName(fq.getClass().getSimpleName());
		model.setDataFile(trainingDataFile);
		model.setFuc(fuc);
		model.setFUOrder(fq.getFeatureUnitOrder());
		
		return model;	
		
		
	}
	

	
	@Override
	public boolean[] classify(ClassifyUnit cu, Model model) {
		try {
			Instances trainingData = ((WekaModel)model).getTrainingData();
			
			// beim ersten Aufruf prüfen wir, ob der classifier bereits erstellt wurde:
			wekaClassifier.buildClassifier(trainingData);
	
			// Weka gibt als Ergebnis den Index des Klassen-Vektors zurück:
			int i = (int) wekaClassifier.classifyInstance(instance(cu, trainingData));
			boolean[] toReturn = new boolean[6];
			toReturn[i]=true;
			return toReturn;
			
		} catch (Exception e) {
			System.out.println(e.getClass());
			e.printStackTrace();
		}
		return null;
	}

	
	/*
	 * Hier beschreiben wir die Struktur unserer Daten, damit Weka sie interpretieren kann, hierfür wird zunächst ein
	 * Struktur-Vektor definiert, der in ein Instances-Objekt gepackt wird, das als Container für die konkreten
	 * Trainingsdaten dient.
	 */
	private Instances initTrainingSet(List<ClassifyUnit> trainingData) {
		
		int vectorSize = trainingData.get(0).getFeatureVector().length;
		Set<Integer> classIDs = new TreeSet<Integer>();
		for (ClassifyUnit classifyUnit : trainingData) {
			ZoneClassifyUnit actual = (ZoneClassifyUnit)classifyUnit;
			classIDs.add(actual.getActualClassID());
		}
		/* Der Vektor enthält die numerischen Merkmale (bei uns: tf-idf-Werte) sowie ein Klassenattribut: */
		ArrayList<Attribute> structureVector = new ArrayList<Attribute>(vectorSize + 1);
		/* Auch die Klasse wird in Weka als Vektor dargestellt: */
		ArrayList<String> classesVector = new ArrayList<String>();
		for (Integer c : classIDs) {
			/*
			 * Da das Klassen-Attribut nicht numerisch ist (sondern, in Weka-Terminologie, ein nominales bzw.
			 * String-Attribut), müssen hier alle möglichen Attributwerte angegeben werden:
			 */
			classesVector.add(c+"");
		}
		/* An Stelle 0 unseres Strukturvektors kommt der Klassen-Vektor: */
		structureVector.add(new Attribute("topic", classesVector));
		for (int i = 0; i < vectorSize; i++) {
			/*
			 * An jeder weiteren Position unseres Merkmalsvektors haben wir ein numerisches Merkmal (repräsentiert als
			 * Attribute), dessen Name hier einfach seine Indexposition ist:
			 */
			structureVector.add(new Attribute(i + "")); // Merkmal i, d.h. was? > TF-IDF
		}
		/*
		 * Schliesslich erstellen wir einen Container, der Instanzen in der hier beschriebenen Struktur enthalten wird
		 * (also unsere Trainingsbeispiele):
		 */
		Instances result = new Instances("InstanceStructure", structureVector, vectorSize + 1);
		/*
		 * Wobei wir hier erneut angeben muessen, an welcher Stelle der Merkmalsvektoren die Klasse zu finden ist:
		 */
		result.setClassIndex(0);
		return result;
	}
	
	private Instance instance(ClassifyUnit cu, Instances trainingSet) {
		double[] values = cu.getFeatureVector();
		String classID = ((ZoneClassifyUnit)cu).getActualClassID()+"";
		Instance instance = new SparseInstance(1, values);
		/*
		 * Weka muss 'erklärt' bekommen, was die Werte bedeuten - dies ist im Trainingsset beschrieben:
		 */
		instance.setDataset(trainingSet);
		/*
		 * Beim Training geben wir den Instanzen ein Klassenlabel, bei der Klassifikation ist die Klasse unbekannt:
		 */
		if (classID == "0") {
			instance.setClassMissing(); // bei Klassifikation
		} else
			instance.setClassValue(classID); // beim Training
		return instance;
	}
	

}
