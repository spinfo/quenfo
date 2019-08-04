package quenfo.de.uni_koeln.spinfo.information_extraction.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import is2.data.SentenceData09;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author geduldia
 * 
 *         A part of a job-ad smaller than a paragraph. (usually a sentence)
 *         Includes a lot of lexial data (lemmata, posTags,...) which is needed
 *         for the information extraction.
 *
 */

@Data
@EqualsAndHashCode(of = { "jobAdID", "secondJobAdID", "classifyUnitID", "sentence" })
@ToString(of = { "sentence" })
public class ExtractionUnit {

	private UUID sentenceID;

	private String sentence;

	// wird von den Mate-Tools produziert (enthält Lemmata, posTags und Tokens)
	private SentenceData09 sentenceData = new SentenceData09();

	// ID der beinhaltenden ClassifyUnit
	private UUID classifyUnitID;

	// Table-ID der beinhaltenden ClassifyUnit
	private int classifyUnitTableID;

	@Getter(AccessLevel.NONE)
	private String[] tokens;

	@Getter(AccessLevel.NONE)
	private String[] lemmata;

	@Getter(AccessLevel.NONE)
	private String[] posTags;
	//
	private boolean lexicalDataIsStoredInDB;

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
	private List<TextToken> tokenObjects = new ArrayList<TextToken>();

	public ExtractionUnit(String sentence) {
		this.sentenceID = UUID.randomUUID();
		this.sentence = sentence;
	}

	public ExtractionUnit() {
		this.sentenceID = UUID.randomUUID();
	}

	/**
	 * @return tokens produced by the MateTool
	 */
	public String[] getTokens() {
		if (this.tokens != null)
			return tokens;
		if (this.sentenceData != null)
			return sentenceData.forms;
		return null;
	}

	/**
	 * @return lemmata produced by the MateTool
	 */
	public String[] getLemmata() {
		if (this.lemmata != null)
			return this.lemmata;
		if (this.sentenceData != null)
			return sentenceData.plemmas;
		return null;
	}

	/**
	 * @return posTags produced by the MateTool
	 */
	public String[] getPosTags() {
		if (this.posTags != null)
			return this.posTags;
		if (this.sentenceData != null)
			return sentenceData.ppos;
		return null;
	}

	/**
	 * @return morphTags produced by the MateTool
	 */
	public String[] getMorphTags() {
		return sentenceData.pfeats;
	}

	/**
	 * creates the List of Token-objects for this ExtractionUnit Sets a Root-Token
	 * as first Token and an End-Token as last Token
	 * 
	 * @param sentenceData
	 */
	public void setSentenceData(SentenceData09 sentenceData) {
		this.sentenceData = sentenceData;
		TextToken token = null;
		String[] tokens = getTokens();
		String[] lemmas = getLemmata();
		String[] posTags = getPosTags();
		for (int i = 0; i < tokens.length; i++) {
			if (posTags == null) {
				token = new TextToken(tokens[i], lemmas[i], null);
			} else {
				token = new TextToken(tokens[i], lemmas[i], posTags[i]);
			}
			this.tokenObjects.add(token);
		}
		token = new TextToken(null, "<end-LEMMA>", "<end-POS>");
		this.tokenObjects.add(token);
	}

	public void deleteData() {
		this.sentenceData = null;
		this.tokenObjects = null;
	}

//	public boolean isLexicalDataIsStoredInDB() {
//		return lexicalDataIsStoredInDB;
//	}
//
//	public void setLexicalDataIsStoredInDB(boolean lexicalDataIsStoredInDB) {
//		this.lexicalDataIsStoredInDB = lexicalDataIsStoredInDB;
//	}
//
//	public int getClassifyUnitTableID() {
//		return classifyUnitTableID;
//	}
//
//	public void setClassifyUnitTableID(int classifyUnitTableID) {
//		this.classifyUnitTableID = classifyUnitTableID;
//	}
//
//
//	
//
//	public UUID getSentenceID() {
//		return sentenceID;
//	}
//
//	/**
//	 * @return tokens
//	 */
//	public List<TextToken> getTokenObjects() {
//		return tokenObjects;
//	}
//
//	/**
//	 * @return secondJobAdID (Zeilennummer)
//	 */
//	public int getSecondJobAdID() {
//		return secondJobAdID;
//	}
//
//	/**
//	 * @param secondJobAdID
//	 *            (Zeilennummer)
//	 */
//	public void setSecondJobAdID(int secondJobAdID) {
//		this.secondJobAdID = secondJobAdID;
//	}
//
//	/**
//	 * @return classifyUnitID
//	 */
//	public UUID getClassifyUnitID() {
//		return classifyUnitID;
//	}
//
//	/**
//	 * @param classifyUnitID
//	 */
//	public void setClassifyUnitID(UUID classifyUnitID) {
//		this.classifyUnitID = classifyUnitID;
//	}
//
//	/**
//	 * @return jobAdID
//	 */
//	public int getJobAdID() {
//		return jobAdID;
//	}
//
//	/**
//	 * @param jobAdID
//	 */
//	public void setJobAdID(int jobAdID) {
//		this.jobAdID = jobAdID;
//	}

//	public void setLemmata(String[] lemmata){
//		this.lemmata = lemmata;
//	}
//	
//	public void setPosTags(String[] posTags){
//		this.posTags = posTags;
//	}
//	
//	public void setTokens(String[] tokens){
//		this.tokens = tokens;
//	}

//	/**
//	 * @param sentence
//	 */
//	public void setSentence(String sentence) {
//		this.sentence = sentence;
//	}
//
//	/**
//	 * @return sentence
//	 */
//	public String getSentence() {
//		return sentence;
//	}

//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see java.lang.Object#toString()
//	 */
//	@Override
//	public String toString() {
//		StringBuffer sb = new StringBuffer();
//		sb.append(sentence + "\n");
//		return sb.toString();
//	}

//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see java.lang.Object#equals(java.lang.Object)
//	 */
//	@Override
//	public boolean equals(Object o) {
//		ExtractionUnit cu = (ExtractionUnit) o;
//		return (this.getJobAdID() + this.getSecondJobAdID() + this.getClassifyUnitID().toString() + this.getSentence())
//				.equals(cu.getJobAdID() + cu.getSecondJobAdID() + cu.getClassifyUnitID().toString() + cu.getSentence());
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see java.lang.Object#hashCode()
//	 */
//	@Override
//	public int hashCode() {
//		return new HashCodeBuilder(3, 17).append(getJobAdID()).append(getSecondJobAdID()).append(getClassifyUnitID())
//				.append(getSentence()).toHashCode();
//	}

}
