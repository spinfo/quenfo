package de.uni_koeln.spinfo.information_extraction.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sound.midi.MidiDevice.Info;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import is2.data.SentenceData09;


/**
 * @author geduldia
 * 
 * A part of a job-ad smaller than a paragraph. (usually a sentence or an element of a keyword-list) 
 *
 */
public class ExtractionUnit {

	private String sentence;
	/**
	 * includes a lot of linguistic data (produced by the Mate-Tool)
	 */
	private SentenceData09 sentenceData = new SentenceData09();
 
	/**
	 * ID of the ClassifyUnit (paragrah) this ExtractionUnit is part of
	 */
	private UUID classifyUnitID;
	
	/**
	 * first ID of the containing JobAd (Jahrgang)
	 */
	private int jobAdID;
	/**
	 * second ID of the containing JobAd (Zeilennummer)
	 */
	private int secondJobAdID;
	/**
	 * Tokens in this sentence
	 */
	private List<TextToken> tokens = new ArrayList<TextToken>();
	
	
	
	/**
	 * @param sentence
	 */
	public ExtractionUnit(String sentence) {
		this.sentence = sentence;
	}

	public ExtractionUnit() {

	}
	
	/**
	 * @return tokens
	 */
	public List<TextToken> getTokenObjects(){
		return tokens;
	}

	/**
	 * @return secondJobAdID (Zeilennummer)
	 */
	public int getSecondJobAdID() {
		return secondJobAdID;
	}

	/**
	 * @param secondJobAdID (Zeilennummer)
	 */
	public void setSecondJobAdID(int secondJobAdID) {
		this.secondJobAdID = secondJobAdID;
	}

	/**
	 * @return classifyUnitID
	 */
	public UUID getClassifyUnitID() {
		return classifyUnitID;
	}

	/**
	 * @param classifyUnitID
	 */
	public void setClassifyUnitID(UUID classifyUnitID) {
		this.classifyUnitID = classifyUnitID;
	}

	/**
	 * @return jobAdID
	 */
	public int getJobAdID() {
		return jobAdID;
	}

	/**
	 * @param jobAdID
	 */
	public void setJobAdID(int jobAdID) {
		this.jobAdID = jobAdID;
	}

	/**
	 * @return tokens produced by the MateTool
	 */
	public String[] getTokens() {
		return sentenceData.forms;
	}

	/**
	 * @return lemmata produced by the MateTool
	 */
	public String[] getLemmata() {
		return sentenceData.plemmas;
	}

	/**
	 * @return morphTags produced by the MateTool
	 */
	public String[] getMorphTags() {
		return sentenceData.pfeats;
	}

	/**
	 * @return posTags produced by the MateTool
	 */
	public String[] getPosTags() {
		return sentenceData.ppos;
	}

	/**
	 * creates the List of Token-objects for this ExtractionUnit
	 * Sets a Root-Token as first Token and an End-Token as last Token
	 * @param sentenceData
	 */
	public void setSentenceData(SentenceData09 sentenceData) {
		this.sentenceData = sentenceData;
		TextToken token = null;
		for(int i = 0; i < getTokens().length; i++){
			String[] tokens = getTokens();
			String[] lemmas = getLemmata();
			String[] posTags = getPosTags();
			if(posTags == null){
				token = new TextToken(tokens[i], lemmas[i], null);
			}
			else{
				token = new TextToken(tokens[i], lemmas[i], posTags[i]);
			}	
			this.tokens.add(token);
		}
		token = new TextToken(null, "<end-LEMMA>", "<end-POS>");
		this.tokens.add(token);
	}


	/**
	 * @param sentence
	 */
	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	/**
	 * @return sentence
	 */
	public String getSentence() {
		return sentence;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(sentence + "\n");
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		ExtractionUnit cu = (ExtractionUnit) o;
		return (this.getJobAdID() + this.getSecondJobAdID()+this.getClassifyUnitID().toString()+this.getSentence()).equals(cu.getJobAdID()+cu.getSecondJobAdID()+cu.getClassifyUnitID().toString()+cu.getSentence());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode(){
		return new HashCodeBuilder(3, 17).append(getJobAdID()).append(getSecondJobAdID()).append(getClassifyUnitID()).append(getSentence()).toHashCode();
	}

}
