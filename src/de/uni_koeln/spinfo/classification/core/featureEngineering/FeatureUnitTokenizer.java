package de.uni_koeln.spinfo.classification.core.featureEngineering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tokenizes texts into tokens (sequences of alphanumeric characters)
 * @author jhermes
 *
 */
public class FeatureUnitTokenizer {
	


	private String delimiter = "[^\\pL\\pM\\p{Nd}\\p{Nl}\\p{Pc}[\\p{InEnclosedAlphanumerics}&&\\p{So}]]";
	
	/**
	 * Tokenizes specified text into sequences of alphanumeric characters
	 * @param text text
	 * @return List of tokens
	 */
	public List<String> tokenize(String text) {
		if(text == null) return null;
		List<String> tokens = Arrays
				.asList((text.split(delimiter)));
		List<String> result = new ArrayList<String>();
		
		for (String token : tokens) {
			if (token.trim().length() > 0) {
				result.add(token.trim());
			}
		}
		return result;
	}
	
	

}
