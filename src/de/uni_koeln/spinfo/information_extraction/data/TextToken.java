package de.uni_koeln.spinfo.information_extraction.data;

/**
 * @author geduldia
 * 
 * Represents a single Token of an ExtractionUnit (~ Sentence), consisting
 * of String, Lemma and PosTag. A TextToken can be marked as (start of) a
 * known InformationEntity, importanceTerm or knowledgeTerm.
 *
 */
public class TextToken extends Token {
	
	/**
	 * specifies the number of tokens to complete the InformationEntity (if
	 * token is start of one)
	 */
	private int tokensToCompleteInformationEntity = 0;
	/**
	 * specifies the number of tokens to complete the importanceTerm (if token
	 * is start of one)
	 */
	private int tokensToCompleteImportance = 0;
	/**
	 * specifies the number of tokens to complete the knowledgeTerm (if token is
	 * start of one)
	 */
	private int tokensToCompleteKnowledge = 0;
	/**
	 * token is a known 'noEntity' (~ typical but known extraction-mistake)
	 */
	private boolean noEntity;
	
	

	/**
	 * @param string
	 * @param lemma
	 * @param posTag
	 */
	public TextToken(String string, String lemma, String posTag) {
		super(string, lemma, posTag);
	}

	/**
	 * @return number of tokens to complete the InformationEntity (if token is start of one)
	 */
	public int getTokensToCompleteInformationEntity() {
		return tokensToCompleteInformationEntity;
	}

	
	/**
	 * @param tokensToCompleteInformationEntity
	 */
	public void setTokensToCompleteInformationEntity(int tokensToCompleteInformationEntity) {
		this.tokensToCompleteInformationEntity = tokensToCompleteInformationEntity;
	}

	/**
	 * @return number of tokens to complete the importanceTerm (if token is start of one)
	 */
	public int getTokensToCompleteImportance() {
		return tokensToCompleteImportance;
	}

	/**
	 * @param tokensToCompleteImportance
	 */
	public void setTokensToCompleteImportance(int tokensToCompleteImportance) {
		this.tokensToCompleteImportance = tokensToCompleteImportance;
	}

	/**
	 * @return number of tokens to complete the knowledgeTerm (if token is start of one)
	 */
	public int getTokensToCompleteKnowledge() {
		return tokensToCompleteKnowledge;
	}

	/**
	 * @param tokensToCompleteKnowledge
	 */
	public void setTokensToCompleteKnowledge(int tokensToCompleteKnowledge) {
		this.tokensToCompleteKnowledge = tokensToCompleteKnowledge;
	}

	/**
	 * @return returns true if token is a known typical mistake
	 */
	public boolean isNoEntity() {
		return noEntity;
	}

	/**
	 * @param isNoEntity
	 */
	public void setNoEntity(boolean isNoEntity) {
		this.noEntity = isNoEntity;
	}

	/**
	 * compares this TextToken with the given ContextToken
	 * @param contextToken
	 * @return returns true, if this TextToken matches the given ContextToken
	 */
	public boolean isEqualsContextToken(ContextToken contextToken) {
		// compare strings
		if (contextToken.getString() != null) {
			String[] strings = contextToken.getString().split("\\|");
			boolean match = false;
			for (String string : strings) {
				match = string.equals(this.string);
				if (match)
					break;
			}
			if (!match)
				return false;
		}
		// compare posTags
		if (contextToken.getPosTag() != null) {
			String[] tags = contextToken.getPosTag().split("\\|");
			if (tags[0].startsWith("-")) {
				for (String tag : tags) {
					tag = tag.substring(1);
					if (tag.equals(this.posTag)) {
						return false;
					}
				}
			} else {
				boolean match = false;
				for (String tag : tags) {
					if (tag.startsWith("-")) {
						match = !(tag.equals(this.posTag));
					} else {
						match = tag.equals(this.posTag);
					}
					if (match)
						break;
				}
				if (!match) {
					return false;
				}
			}

		}
		// compare lemmata
		if (contextToken.getLemma() != null) {
			if (contextToken.getLemma().toUpperCase().equals("IMPORTANCE")) {
				return this.importanceTerm;
			}
			if (contextToken.getLemma().toUpperCase().equals("KNOWLEDGE")) {
				return this.knowledgeTerm;
			} else {
				String[] lemmas = contextToken.getLemma().split("\\|");
				boolean match = false;
				for (String lemma : lemmas) {
					if (lemma.startsWith("-")) {
						match = this.getLemma().endsWith(lemma.substring(1));
						if (match) {
							match = !this.getLemma().startsWith(lemma.substring(1));
						}
					} else {
						match = this.lemma.equals(lemma);
					}

					if (match)
						break;
				}
				if (!match) {
					return false;
				}
			}
		}
		if (contextToken.isInformationEntity()) {
			return this.informationEntity;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(string + "\t" + lemma + "\t" + posTag + "\t");
		if (this.informationEntity) {
			sb.append("isInformsationEntitiy" + "\t");
		}
		if (this.noEntity) {
			sb.append("isNoEntity" + "\t");
		}
		if (this.knowledgeTerm) {
			sb.append("isKnowledgeTerm" + "\t");
		}
		if (this.importanceTerm) {
			sb.append("is (start of) importanceTerm" + "\t");
		}
		return sb.toString();
	}

}
