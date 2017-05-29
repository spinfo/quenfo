package de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.model;

import java.io.Serializable;
import java.util.Map;

/**
 * @author geduldia
 * 
 * an intern naive bayes model-object for a specific category
 *
 */
public class NaiveBayesClassModel implements Serializable{
	private static final long serialVersionUID = 1L;
	private Map<String,Integer> inClassDFs;
	private Map<String,Integer> notInClassDFs;
	
	private double classProbability;
	private int membersInClass;
	private int membersNotInClass;
	
	
	
	/**
	 * @return number of members in this class
	 */
	public int getMembersInClass() {
		return membersInClass;
	}
	/**
	 * @param membersInClass
	 */
	public void setMembersInClass(int membersInClass) {
		this.membersInClass = membersInClass;
	}
	/**
	 * @return number of cus not in class
	 */
	public int getMembersNotInClass() {
		return membersNotInClass;
	}
	/**
	 * @param membersNotInClass
	 */
	public void setMembersNotInClass(int membersNotInClass) {
		this.membersNotInClass = membersNotInClass;
	}
	/**
	 * @return prob. of class
	 */
	public double getClassProbability() {
		return classProbability;
	}
	/**
	 * @param probability
	 */
	public void setClassProbability(double probability) {
		this.classProbability = probability;
	}
	/**
	 * @return Doc-Frequencies of class
	 */
	public Map<String, Integer> getInClassDFs() {
		return inClassDFs;
	}
	/**
	 * @param inClassDFs
	 */
	public void setInClassDFs(Map<String, Integer> inClassDFs) {
		this.inClassDFs = inClassDFs;
	}
	/**
	 * @return Doc-Frequencies of cus not in class
	 */
	public Map<String, Integer> getNotInClassDFs() {
		return notInClassDFs;
	}
	/**
	 * @param notInClassDFs
	 */
	public void setNotInClassDFs(Map<String, Integer> notInClassDFs) {
		this.notInClassDFs = notInClassDFs;
	}
}
