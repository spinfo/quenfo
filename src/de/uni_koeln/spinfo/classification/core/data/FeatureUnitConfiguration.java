package de.uni_koeln.spinfo.classification.core.data;

import java.io.Serializable;

/**
 * stores all information about how the feature units where generated (feature-selection, feature-reduction)
 * 
 * @author geduldia
 * 
 * 
 *
 */

public class FeatureUnitConfiguration implements Serializable {
	
	
	private static final long serialVersionUID = 1L;
	

	private boolean normalize;
	private boolean stem;
	private boolean filterStopwords;
	private int[] ngrams;
	private boolean continuusNGrams;
	private int miScore;
	private boolean treatEncoding = false;
	private boolean suffixTree;
	
	/**
	 * @param configString
	 */
	public FeatureUnitConfiguration(String configString){
		String[] splits = configString.split("_");
		for (String string : splits) {
			if(string.equals("norm")){
				normalize=true;
				continue;
			}
			if(string.equals("suffixTree")){
				suffixTree = true;
			}
			if(string.equals(stem)){
				stem=true;
				continue;
			}
			if(string.equals("_filterStopwords")){
				filterStopwords = true;
				continue;
			}
			if(string.startsWith("ngrams:")){
				
				String[] innersplits = string.split("-");
				if(innersplits.length>1){
					ngrams = new int[innersplits.length-1];
					for (int i = 0; i < ngrams.length; i++) {
						ngrams[i] = Integer.parseInt(innersplits[i+1]);
					}
				}
				continue;
			}		
			if(string.startsWith("mi:")){
				miScore = Integer.parseInt(string.substring(string.indexOf(":")+1));
			}
		}
	}

	/**
	 * @param normalize
	 * @param stem
	 * @param filterStopwords
	 * @param ngrams
	 * @param continuusNGrams
	 * @param miScore
	 */
	public FeatureUnitConfiguration(boolean normalize, boolean stem,
			boolean filterStopwords, int[] ngrams, boolean continuusNGrams,
			int miScore, boolean suffixTree) {
		super();
		this.normalize = normalize;
		this.stem = stem;
		this.filterStopwords = filterStopwords;
		this.ngrams = ngrams;
		this.continuusNGrams = continuusNGrams;
		this.miScore = miScore;
		this.suffixTree = suffixTree;
	}


	/**
	 * @return normalize
	 */
	public boolean isNormalize() {
		return normalize;
	}
	
	public boolean isSuffixTree(){
		return suffixTree;
	}

	/**
	 * @return stem
	 */
	public boolean isStem() {
		return stem;
	}

	/**
	 * @return filterStopwords
	 */
	public boolean isFilterStopwords() {
		return filterStopwords;
	}

	/**
	 * @return  nGramms
	 */
	public int[] getNgrams() {
		return ngrams;
	}

	/**
	 * @return continuus Ngrams
	 */
	public boolean isContinuusNGrams() {
		return continuusNGrams;
	}

	/**
	 * @return mi-score
	 */
	public int getMiScore() {
		return miScore;
	}


	

	
	/**
	 * @return treat encoding
	 */
	public boolean isTreatEncoding() {
		return treatEncoding;
	}

	/**
	 * 
	 * @param treatEncoding
	 */
	public void setTreatEncoding(boolean treatEncoding) {
		this.treatEncoding = treatEncoding;
	}
	
	
	public String toString(){
		StringBuffer buff = new StringBuffer();
		if(suffixTree){
			buff.append("_suffixTrees");
		}
		if(normalize){
			buff.append("_norm");
		}
		if(stem){
			buff.append("_stem");
		}
		if(filterStopwords){
			buff.append("_swFilter");
		}
		if(ngrams!=null){
			buff.append("_");
			for (int n : ngrams) {
				buff.append(n+"-");
			}
			buff.append("gramms");
		}
//		buff.append("_ngrams=");
//		if (ngrams != null) {
//
//			for (int n : ngrams) {
//				buff.append("-"+n);
//			}			
//		} 
//		else {
//			buff.append("no");
//		}
		if(continuusNGrams){
			buff.append("_contNGrams");
		}
		buff.append("_mi=" + miScore +"_");
		if(treatEncoding){
			buff.append("treatedEncoding_");
		}
		
		return buff.toString();
	}

}
