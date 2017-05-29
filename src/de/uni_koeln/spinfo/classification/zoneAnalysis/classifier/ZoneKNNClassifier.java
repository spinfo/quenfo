 package de.uni_koeln.spinfo.classification.zoneAnalysis.classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.core.distance.Distance;
import de.uni_koeln.spinfo.classification.core.distance.DistanceCalculator;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.model.ZoneKNNModel;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;

/**
 * @author geduldig
 * 
 * A Classifier based on the KNN-Algorithm
 *
 */
public class ZoneKNNClassifier extends ZoneAbstractClassifier{

	private static final long serialVersionUID = 1L;
	
	//nearest neighbors
	private int knn = 5;

	/**
	 * @param multiClass
	 * @param k
	 * @param distance
	 */
	public ZoneKNNClassifier(boolean multiClass, int k, Distance distance){
		this.knn = k;
		this.multiClass = multiClass;
		this.distance = distance;
	}
	
	public ZoneKNNClassifier(){
	}
	

	
	/**
	 * @param k 
	 */
	public void setK(int k){
		this.knn = k;
	}

	
	/**
	 * @return k
	 */
	public int getK(){
		return knn;
	}
	

	
	/* (non-Javadoc)
	 * @see de.uni_koeln.spinfo.bibb.jasc.classifier.AbstractClassifier#buildModel(java.util.List, de.uni_koeln.spinfo.bibb.jasc.data.FeatureUnitConfiguration, de.uni_koeln.spinfo.bibb.jasc.featureEngineering.quantifiers.AbstractFeatureQuantifier, java.io.File)
	 */
	@Override
	public Model buildModel(List<ClassifyUnit> cus, FeatureUnitConfiguration fuc, AbstractFeatureQuantifier fq, File dataFile){
		Model model = new ZoneKNNModel();
		    Map<double[], boolean[]> trainingdata = new HashMap<double[], boolean[]>();
			for (ClassifyUnit classifyUnit : cus) {
				trainingdata.put(classifyUnit.getFeatureVector(), ((ZoneClassifyUnit) classifyUnit).getClassIDs());
			}	
			((ZoneKNNModel) model).setTrainingData(trainingdata);		
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
	public boolean[] classify(ClassifyUnit cu, Model model){
		int numberOfClasses = ( (ZoneClassifyUnit) cu).getClassIDs().length;
		boolean[] toReturn = new boolean[numberOfClasses];
		
		//sort classIDs by distance to cu:
		Map<Double,List<boolean[]>> classIDsByDistance = new TreeMap<Double,List<boolean[]>>();
		Iterator<double[]> fuIterator = ((ZoneKNNModel) model).getTrainingData().keySet().iterator();
		while(fuIterator.hasNext()){
			
			
			double[] featureVector = fuIterator.next();
			double dist = DistanceCalculator.getDistance(featureVector, cu.getFeatureVector(), distance);
			List<boolean[]> classIDs;
			if(classIDsByDistance.containsKey(dist)){
				classIDs = classIDsByDistance.get(dist);
			}
			else{
				classIDs = new ArrayList<boolean[]>();
			}
			classIDs.add(((ZoneKNNModel) model).getTrainingData().get(featureVector));
			classIDsByDistance.put(dist, classIDs);
		}
		
		
		//find k nearest classIDs
		List<boolean[]> KNNs = new ArrayList<boolean[]>();
		Iterator<List<boolean[]>> iterator = classIDsByDistance.values().iterator();
		while(KNNs.size()< knn){
			KNNs.addAll(iterator.next());
		}
		//count classMembers of knn
		int[] classCounts = new int[numberOfClasses]; 
		for (boolean[] classIDs : KNNs) {
			for(int c = 0; c < numberOfClasses; c++){
				if(classIDs[c]){
					classCounts[c]++;
				}
			}
		}
		
		//find nearestclasses
		int bestCount = 0;
		List<Integer> bestClasses = new ArrayList<Integer>();
		for(int c = 0; c < numberOfClasses; c++){
			int count = classCounts[c];
			if(count> bestCount){
				bestCount = count;
				bestClasses.clear();
				bestClasses.add(c+1);
				continue;
			}
			if(count == bestCount){
				if(multiClass){
					bestClasses.add(c+1);
				}
				else{
					if((c+1) == defaultID){
						bestClasses.clear();
						bestClasses.add(c+1);
					}
				}
			}
		}
		for(int c = 0; c < bestClasses.size(); c++){
			toReturn[bestClasses.get(c)-1] = true;
		}
		return toReturn;
	}
	
	
	
	/* (non-Javadoc)
	 * @see de.uni_koeln.spinfo.bibb.jasc.classifier.AbstractClassifier#getClassifierConfig()
	 */
	public String getClassifierConfig(){
		return "k="+knn;
	}



}
