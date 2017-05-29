package de.uni_koeln.spinfo.classification.core.featureEngineering.featureReduction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Removes stopwords from string lists.
 * @author jhermes
 *
 */
public class StopwordFilter {
	
	private Set<String> stopwords;
	
	/**
	 * Initializes the StopwordFilter with stopwords specified in file. 
	 * @param stopwordFile File with stopwords (format: one stopword per line)
	 * @throws IOException If file doesn't exist or can't be read
	 */
	public StopwordFilter(File stopwordFile) throws IOException{
		stopwords = new HashSet<String>();
		BufferedReader in = new BufferedReader(new FileReader(stopwordFile));
		String line = in.readLine();
		while(line!=null){
			stopwords.add(line.trim());
			line = in.readLine();
		}
		in.close();
	}
	
	
	/**
	 * Removes the stopwords from the specified list
	 * @param toFilter list of words
	 * @return the same list without stopwords
	 */
	public List<String> filterStopwords(List<String> toFilter){
		List<String> filtered = new ArrayList<String>();
		for (String token : toFilter) {
			if(!stopwords.contains(token.toLowerCase())){
				filtered.add(token);
			}
		}
		return filtered;
	}

}
