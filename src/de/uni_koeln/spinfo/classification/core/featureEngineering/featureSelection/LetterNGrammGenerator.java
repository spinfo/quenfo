package de.uni_koeln.spinfo.classification.core.featureEngineering.featureSelection;

import java.util.LinkedList;
import java.util.List;

/**
 * Generates letter ngrams from tokens  
 * @author geduldia
 *
 */
public class LetterNGrammGenerator {
	

	
	
	/**
	 * Generates ngrams from specified tokens.
	 * @param tokens List of tokens 
	 * @param n length of ngrams
	 * @param continuous If true, ngrams will be generated across token borders
	 * @return List of ngrams
	 */
	public static List<String> getNGramms(List<String> tokens, int n, boolean continuous){
		List<String> toReturn = new LinkedList<String>();
		if(!continuous){
			StringBuffer sb = new StringBuffer();
			for (String string : tokens) {
				sb.append(string+" ");
			}
			String text = sb.toString();
			for(int i = 0; i < text.length()-n; i++){
				String ngramm = text.substring(i, i+(n));
				if(ngramm.length()>1){
					toReturn.add(ngramm);
				}
				
			}
		}		
		else{
			for (String token : tokens) {
				if(token.length() < n){
					if(token.length() >1){
						toReturn.add(token);
					}
					
				}
				for(int i = 0; i < token.length()-n; i++){
					String nGramm = token.substring(i, i+n);
					if(nGramm.length() > 1){
						toReturn.add(nGramm);
					}
					
				}
			}
		}
		return toReturn;
	}
	
	/**
	 * @param paragraph the text
	 * @param n the n of nGrams
	 * @return a list of nGrams
	 */
	public static List<String> getNGramms(String paragraph, int n){
		List<String> toReturn = new LinkedList<String>();
		
		for(int i = 0; i < paragraph.length()-n; i++){
			String ngramm = paragraph.substring(i, i+(n));
			toReturn.add(ngramm);
		}
		return toReturn;
	}


}
