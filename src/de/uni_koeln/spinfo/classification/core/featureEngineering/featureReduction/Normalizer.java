package de.uni_koeln.spinfo.classification.core.featureEngineering.featureReduction;

import java.util.List;

/**
 * @author jhermes
 *
 */
public class Normalizer {
	boolean normalizeUnderscoreSeqences;
	boolean normalizeNumbers;
	boolean toLowerCase;
	
	public Normalizer(){
		this.normalizeUnderscoreSeqences = true;
		this.normalizeNumbers = true;
		this.toLowerCase = true;
	}
	
	public Normalizer(boolean normalizeUnderscoreSeqences, boolean normalizeNumbers, boolean toLowerCase) {
		super();
		this.normalizeUnderscoreSeqences = normalizeUnderscoreSeqences;
		this.normalizeNumbers = normalizeNumbers;
		this.toLowerCase = toLowerCase;
	}



	public void normalize(List<String> featureUnits){
		for (int i=0; i<featureUnits.size(); i++) {
			String fu=featureUnits.get(i);
//			if(fu.length()>1){
//				if(normalizeUnderscoreSeqences && fu.startsWith("__") && fu.endsWith("__")){
//					fu="___";
//				}				
//			}
			if(normalizeNumbers && Character.isDigit(fu.charAt(0)) && Character.isDigit(fu.charAt(fu.length()-1))){
				fu="NUM";
			}
			if(toLowerCase){
				fu = fu.toLowerCase();
			}
			if(fu.length() > 1){
				featureUnits.set(i,fu);
			}
			
		}
	}

}
