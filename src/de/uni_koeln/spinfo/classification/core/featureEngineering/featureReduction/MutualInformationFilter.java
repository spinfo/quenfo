package de.uni_koeln.spinfo.classification.core.featureEngineering.featureReduction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;

public class MutualInformationFilter {

	/**
	 * Filters FeatureUnits according to their Mutual Information values. TODO
	 * what about multiclass units?
	 * 
	 * @author geduldig
	 * 
	 */

	private Map<String, Integer> allTFs;
	private Map<String, Integer> relevantTFs;

	private int totalNumberOfFeatureUnits = 0;
	private int totalNumberOfClassifyUnits = 0;
	private int classifyUnitsInCategory = 0;

	Set<String> relevantFeatureUnitsOverall;
	
	
	

	
	public void initialize(FeatureUnitConfiguration fuc, List<ClassifyUnit> trainingdata){
	
		int numberOfClasses = ( (ZoneClassifyUnit) trainingdata.get(0)).getClassIDs().length;

		relevantFeatureUnitsOverall = new TreeSet<String>();

		for (int classID = 0; classID < numberOfClasses - 1; classID++) {
			
			totalNumberOfClassifyUnits = trainingdata.size();
			
			// alle für die Klasse relevanten Wörter
			Set<String> relevantFeatureUnits4Class = new TreeSet<String>();

			// alle Wörter die es gibt
			List<String> allFeatureUnits = new LinkedList<String>();

			// alle FUs in der Klasse
			List<String> allFeatureUnitsOfClass = new LinkedList<String>();

			for (ClassifyUnit unitToClassify : trainingdata) {
				totalNumberOfFeatureUnits += unitToClassify.getFeatureUnits()
						.size();
				allFeatureUnits.addAll(unitToClassify.getFeatureUnits());
				if (((ZoneClassifyUnit) unitToClassify).getClassIDs()[classID]) {
					classifyUnitsInCategory++;
					allFeatureUnitsOfClass.addAll(unitToClassify
							.getFeatureUnits());
				}
			}
			// alle TermFrequenzen des gesamten Corpus
			allTFs = calcTermFrequencies(allFeatureUnits);
			relevantTFs = calcTermFrequencies(allFeatureUnitsOfClass);
			TreeMap<Double, Set<String>> MIs = new TreeMap<Double, Set<String>>();
			for (String term : relevantTFs.keySet()) {

				// CalcMIs
				double t = (allTFs.get(term) / (double) totalNumberOfFeatureUnits);
				double c = (classifyUnitsInCategory / (double) totalNumberOfClassifyUnits);
				double tANDc = relevantTFs.get(term)
						/ (double) totalNumberOfFeatureUnits;
				double MI = (tANDc * (Math.log(tANDc / (t * c))));
				Set<String> terms = MIs.get(MI);
				if (terms == null) {
					terms = new TreeSet<String>();
				}
				terms.add(term);
				MIs.put(MI, terms);
			}
			Set<Double> keySet = MIs.keySet();
			List<Double> reverseList = new ArrayList<Double>(keySet);
			int miScore = fuc.getMiScore();
			for (int i = reverseList.size() - 1; i >= 0; i--) {
				relevantFeatureUnits4Class.addAll(MIs.get(reverseList.get(i)));
				 if (relevantFeatureUnits4Class.size() >=
				 miScore)
				 break;
			}
			relevantFeatureUnitsOverall.addAll(relevantFeatureUnits4Class);

		}
	}

	/**
	 * Reduces the number of feature units from all ClassifyUnits to the most
	 * relevant ones (based on MI values)
	 * 
	 * @param cus
	 *            A list of all Classify Units
	 * @param miScore
	 *            Number of relevant feature units that should be leaved over
	 */

	public void filter(List<ClassifyUnit> cus, int miScore) {
		
		List<ClassifyUnit> emptyCUs = new ArrayList<ClassifyUnit>();

		for (ClassifyUnit unitToClassify : cus) {
			List<String> featureUnits = unitToClassify.getFeatureUnits();
			List<String> newFUs = new ArrayList<String>(featureUnits);
			for (String featureUnit : featureUnits) {
				if (!relevantFeatureUnitsOverall.contains(featureUnit)) {
					newFUs.remove(featureUnit);
				}
			}
			if (newFUs.size() == 0) {
				emptyCUs.add(unitToClassify);
			}
			unitToClassify.setFeatureUnits(newFUs);
		}
	}

	private Map<String, Integer> calcTermFrequencies(List<String> featureUnits) {
		Map<String, Integer> tfs = new TreeMap<String, Integer>();
		for (String featureUnit : featureUnits) {
			if (tfs.containsKey(featureUnit)) {
				int tf = tfs.get(featureUnit) + 1;
				tfs.put(featureUnit, tf);
			} else {
				tfs.put(featureUnit, 1);
			}
		}
		return tfs;
	}

}
