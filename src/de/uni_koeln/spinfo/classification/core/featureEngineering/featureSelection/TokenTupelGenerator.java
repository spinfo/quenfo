package de.uni_koeln.spinfo.classification.core.featureEngineering.featureSelection;

import java.util.LinkedList;
import java.util.List;

/**
 * Generates token tupels (Pairs of co-occurenced tokens)
 * @author geduldig
 *
 */
public class TokenTupelGenerator {

	/**
	 * Generates token tupels (Pairs of co-occurenced tokens)
	 * @param tokens Tokens
	 * @param distance maximal distance of Cooccurence
	 * @return List of 
	 */
	public static List<String> getTokenTupelNGrams(List<String> tokens, int distance) {
		List<String> toReturn = new LinkedList<String>();
		for (int i = 0; i < tokens.size() - distance; i++) {
			StringBuffer sb = new StringBuffer();
			sb.append(tokens.get(i));
			for(int j = 1; j <= distance; j++){
				sb.append(" "+tokens.get(i+j));
				toReturn.add(sb.toString());
				sb.delete(sb.length()-2, sb.length());
			}
		}
		return toReturn;
	}
}
