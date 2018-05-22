package quenfo.de.uni_koeln.spinfo.information_extraction.data;

/**
 * @author geduldia
 * 
 * Abstract class for a single Token consisting of string, lemma, and posTag.
 * A Token can be marked as (the first token of) a
 * known Information-Entity (= competence or tool) or (the first token of) an modifier-expression (e.g. 'zwingend erforderlich').
 * classes extending this class:
 *  - ContextToken (Part of a ContextPattern)
 *  - TextToken (Part of an ExtractionUnit)
 */

public abstract class Token {
	
	protected String posTag;
	protected String lemma;	
	protected String string;
	
	protected boolean ieToken;
	protected boolean modifierToken;
	
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
	public Token(String string, String lemma, String posTag, boolean isIEToken) {
		this.posTag = posTag;
		this.string = string;
		this.lemma = lemma;
		if (string != null && string.toLowerCase().equals("pc")) {
			this.lemma = "pc";
		}
		this.ieToken = isIEToken;
	}
	
	/**
	 * @return returns true if this token is (first Token of) a known InformationEntity
	 */
	public boolean isInformationEntity() {
		return ieToken;
	}

	/**
	 * @param isInformationEntity
	 */
	public void setIEToken(boolean isIEToken) {
		this.ieToken = isIEToken;
	}

	/**
	 * @return returns true if this token is (first Token of) a modifier
	 */
	public boolean isModifier() {
		return modifierToken;
	}

	/**
	 * @param isImportanceTerm
	 */
	public void setModifier(boolean isModifier) {
		this.modifierToken = isModifier;
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
