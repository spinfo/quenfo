package de.uni_koeln.spinfo.information_extraction.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.data.InformationEntity;

/**
 * @author geduldia
 * 
 * Represents an EvaluationResult consisting of the integers tp (true-positive), fp and fn and a map of Inf.-Entities and ExtractionUnit  for each value
 *
 */
public class EvaluationResult {
	
	private int tp;
	private int fp;
	private int fn;
	
	private Map<InformationEntity, List<ExtractionUnit>> tpMap = new HashMap<InformationEntity, List<ExtractionUnit>>();
	private Map<InformationEntity, List<ExtractionUnit>> fpMap = new HashMap<InformationEntity, List<ExtractionUnit>>();
	private Map<String, List<ExtractionUnit>> fnMap = new HashMap<String, List<ExtractionUnit>>();
	
	/**
	 * increase number of tp
	 */
	public void addTP(InformationEntity ie, ExtractionUnit eUnit){
		tp++;
		List<ExtractionUnit> list = tpMap.get(ie);
		if(list == null) list = new ArrayList<ExtractionUnit>();
		list.add(eUnit);
		tpMap.put(ie, list);
	}
	
	/**
	 * increase number of fn
	 */
	public void addFN(String ie, ExtractionUnit eUnit){
		fn++;
		List<ExtractionUnit> list =fnMap.get(ie);
		if(list == null) list = new ArrayList<ExtractionUnit>();
		list.add(eUnit);
		fnMap.put(ie, list);
	}
	

	
	/**
	 * increase number of fp
	 */
	public void addFP(InformationEntity ie, ExtractionUnit eUnit){
		fp++;
		List<ExtractionUnit> list =fpMap.get(ie);
		if(list == null) list = new ArrayList<ExtractionUnit>();
		list.add(eUnit);
		fpMap.put(ie, list);
	}
	

	
	
	/**
	 * @return recall-value
	 */
	public double getRecall(){
		double toReturn = ((double) tp)/(tp+fn);
		return toReturn;
	}
	
	/**
	 * @return precision-value
	 */
	public double getPrecision(){
		double toReturn = ((double)tp)/(tp+fp);
		return toReturn;
	}
	
	/**
	 * @return f1-score
	 */
	public double getF1Score(){
		double toReturn = ((double)2*tp) / (2*tp+fp+fn);
		return toReturn;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("\nTP: " + tp+"\n");
		sb.append("FP: " + fp+"\n");
		sb.append("FN: " + fn+"\n");
		sb.append("\nrecall: "+ getRecall()+"\nprecision: " + getPrecision()+ "\nF1Score "+ getF1Score()+ "\n\n");
		return sb.toString();
	}
	
	public String printTPs(){
		StringBuffer sb = new StringBuffer();
		sb.append("TRUE-POSITIVES: "+"\n\n");
		for (InformationEntity ie : tpMap.keySet()) {
			sb.append(ie+"\n");
			for (ExtractionUnit eUnit : tpMap.get(ie)) {
				sb.append("\t" + eUnit.getSentence()+"\n");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public String printFPs(){
		StringBuffer sb = new StringBuffer();
		sb.append("FALSE-POSITIVES: "+"\n\n");
		for (InformationEntity ie : fpMap.keySet()) {
			sb.append(ie+ "\n");
			for (ExtractionUnit eUnit : fpMap.get(ie)) {
				sb.append("\t" + eUnit.getSentence()+"\n");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public String printFNs(){
		StringBuffer sb = new StringBuffer();
		sb.append("FALSE-NEGATIVES: "+"\n\n");
		for (String ie : fnMap.keySet()) {
			sb.append(ie+"\n");
			for (ExtractionUnit eUnit : fnMap.get(ie)) {
				sb.append("\t" + eUnit.getSentence()+"\n");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

}
