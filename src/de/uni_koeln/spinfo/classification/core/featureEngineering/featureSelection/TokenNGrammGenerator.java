package de.uni_koeln.spinfo.classification.core.featureEngineering.featureSelection;

import java.util.LinkedList;
import java.util.List;

/**
 * Generates token ngrams.
 * 
 * @author geduldig
 *
 */
public class TokenNGrammGenerator {
	
	/**
	 * @param tokens
	 * @param n
	 * @return featureunit/ngrams
	 */
	public static List<String> getTokenNramms(List<String> tokens, int n) {
		List<String> featureUnits = new LinkedList<String>();
		for(int i = 0; i < tokens.size()-2; i++){
				StringBuffer sb = new StringBuffer();
				sb.append(tokens.get(i));
				for(int j = 1; j <n; j++){
					sb.append(tokens.get(i+j));
				}
				featureUnits.add(sb.toString());
		}
		
		return featureUnits;
		
	}
}
