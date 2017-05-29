package de.uni_koeln.spinfo.information_extraction.data;

/**
 * @author geduldia
 * 
 * Abstract class for a single Token consisting of string, lemma, an posTag.
 * A Token can be marked as (part of) an InformationEntity (e.g. Competence), 
 * an importanceTerm (e.g. 'wünschenswert') or a knowledgeTerm (e.g. 'Kenntniss', 'Erfahrung', 'Übung')
 * classes extending this class:
 *  - ContextToken (Part of a ContextPattern)
 *  - TextToken (Part of an ExtractionUnit)
 */

public abstract class Token {
	
	protected String posTag;
	protected String lemma;	
	protected String string;
	
	protected boolean informationEntity;
	protected boolean importanceTerm;
	protected boolean knowledgeTerm;
	
	/**
	 * @param string
	 * @param lemma
	 * @param posTag
	 */
	public Token(String string, String lemma, String posTag) {
		this(string, lemma, posTag, false);
	}

	/**
	 * @param string
	 * @param lemma
	 * @param posTag
	 * @param isInformationEntity
	 */
	public Token(String string, String lemma, String posTag, boolean isInformationEntity) {
		this.posTag = posTag;
		this.string = string;
		this.lemma = lemma;
		if (string != null && string.toLowerCase().equals("pc")) {
			this.lemma = "pc";
		}
		this.informationEntity =isInformationEntity;
	}
	
	/**
	 * @return returns true if this token is (start of) a known InformationEntity
	 */
	public boolean isInformationEntity() {
		return informationEntity;
	}

	/**
	 * @param isInformationEntity
	 */
	public void setInformationEntity(boolean isInformationEntity) {
		this.informationEntity = isInformationEntity;
	}

	/**
	 * @return returns true if this token is (start of) an importanceTerm
	 */
	public boolean isImportanceTerm() {
		return importanceTerm;
	}

	/**
	 * @param isImportanceTerm
	 */
	public void setImportanceTerm(boolean isImportanceTerm) {
		this.importanceTerm = isImportanceTerm;
	}

	/**
	 * @return returns true if this token is (start of) a knowledgeTerm
	 */
	public boolean isKnowledgeTerm() {
		return knowledgeTerm;
	}

	/**
	 * @param isKnowledgeTerm
	 */
	public void setKnowledgeTerm(boolean isKnowledgeTerm) {
		this.knowledgeTerm = isKnowledgeTerm;
	}

	/**
	 * @return posTag
	 */
	public String getPosTag() {
		return posTag;
	}

	/**
	 * @return lemma
	 */
	public String getLemma() {
		return lemma;
	}

	/**
	 * @return string
	 */
	public String getString() {
		return string;
	}

	
	

}
