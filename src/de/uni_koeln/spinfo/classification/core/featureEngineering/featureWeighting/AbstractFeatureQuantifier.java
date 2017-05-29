package de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;

/**
 * Class containing the base functionality of Feature Quantifiers
 * @author jhermes
 *
 */
public abstract class  AbstractFeatureQuantifier {
	
	public int maxTF;
	
	//List of all feature-words
	protected List<String> featureUnitOrder;
	
	

	
	/**
	 * calculates feature values and sets feature-vectors for the given cus
	 * @param classifyUnits
	 * @param featureUnitOrder
	 */
	public abstract void setFeatureValues(List<ClassifyUnit> classifyUnits, List<String> featureUnitOrder);
	
	
	/**
	 * 
	 * @return ordered list of all relevant feature-units
	 */
	public List<String>  getFeatureUnitOrder(){
		return featureUnitOrder;
	}
	

	/**
	 * @param classifyUnits
	 * @return ordered list of all relevant feature-units
	 */
	protected List<String> getFeatureUnitOrder(List<ClassifyUnit> classifyUnits) {
		Set<String> uniqueFeatureUnits = new TreeSet<String>();

		for (ClassifyUnit classifyUnit : classifyUnits) {
			for (String featureUnit : classifyUnit.getFeatureUnits()) {
				uniqueFeatureUnits.add(featureUnit);
			}
		}
		List<String> toReturn = new ArrayList<String>(uniqueFeatureUnits);
		return toReturn;
	}
	
	/**
	 * @param featureUnits
	 * @return termFrequencies of feature units
	 */
	public Map<String, Integer> getTermFrequencies(List<String> featureUnits) {
		Map<String, Integer> tfs = new TreeMap<String, Integer>();
		maxTF = 1;

		for (String featureUnit : featureUnits) {
			if (tfs.containsKey(featureUnit)) {
				int tf = tfs.get(featureUnit) + 1;
				tfs.put(featureUnit, tf);
				if (tf > maxTF) {
					maxTF = tf;
				}
			} else {
				tfs.put(featureUnit, 1);
			}
		}
		return tfs;
	}
	
	public void resetFeatureUnitOrder(){
		this.featureUnitOrder = null;
	}
	
}
