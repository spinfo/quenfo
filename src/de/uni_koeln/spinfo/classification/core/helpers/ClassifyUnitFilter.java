package de.uni_koeln.spinfo.classification.core.helpers;

import java.util.ArrayList;
import java.util.List;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.featureEngineering.FeatureUnitTokenizer;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;

/**
 * @author geduldia
 *
 */
public class ClassifyUnitFilter {
	
	/**
	 * filters all cus with less than specified number of tokens in content
	 * @param cus
	 * @param minNumberOfTokens
	 * @return filtered list of cus
	 */
	public static List<ClassifyUnit> filterByTokens(List<JASCClassifyUnit> cus, int minNumberOfTokens){
		FeatureUnitTokenizer tokenizer = new FeatureUnitTokenizer();
		List<String> tokens;
		List<ClassifyUnit> filtered = new ArrayList<ClassifyUnit>();
		for (int i = 0; i < cus.size(); i++) {
			JASCClassifyUnit cu = cus.get(i);
			tokens = tokenizer.tokenize(cu.getContent());
			if(tokens == null){
				continue;
			}
			if(!(tokens.size() < minNumberOfTokens)){
				filtered.add(cu);
			}
		}
		return filtered;
	}
	
	/**
	 * filters all cus with less than specified number of feature units
	 * @param cus
	 * @param minNumberOfFUs
	 * @return filtered list of cus
	 */
	public static List<ClassifyUnit> filterByFUs(List<ClassifyUnit> cus, int minNumberOfFUs){
		List<ClassifyUnit> filtered = new ArrayList<ClassifyUnit>();
		for (int i = 0; i < cus.size(); i++) {
			ClassifyUnit cu = cus.get(i);
			if(!(cu.getFeatureUnits().size() < minNumberOfFUs)){
				filtered.add(cu);
			}
		}
		return filtered;
	}
	
	/**
	 * filters all cus with less than specifies characters in content
	 * @param cus
	 * @param contentLength
	 * @return filtered cus
	 */
	public static List<JASCClassifyUnit> filterByContentLength(List<JASCClassifyUnit> cus, int contentLength){
		List<JASCClassifyUnit> filtered = new ArrayList<JASCClassifyUnit>();
		for (int i = 0; i < cus.size(); i++) {
			JASCClassifyUnit cu = cus.get(i);
			if(!(cu.getContent().length() < contentLength)){
				filtered.add(cu);
			}
		}
		return filtered;
	}

}
