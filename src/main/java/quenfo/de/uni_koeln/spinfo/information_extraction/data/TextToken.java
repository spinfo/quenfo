package quenfo.de.uni_koeln.spinfo.information_extraction.data;

import de.uni_koeln.spinfo.data.Token;

/**
 * @author geduldia
 * 
 * Represents a single Token of an ExtractionUnit (~ Sentence), consisting
 * of String, Lemma and PosTag. A TextToken can be marked as (the first token of) a
 * known Information-Entity (= competence or tool) or (the first token of) an importanceTerm.
 *
 */
public class TextToken extends Token {
	
	//Falls das Token das erste Token einer Information-Entity ist: Anzahl der noch fehlenden Tokens im Satz
	private int tokensToCompleteInformationEntity = 0;
	//Falls das Token das erste Token eines Modifizierers ist: Anzahl der noch fehlenden Tokens im Satz
	private int tokensToCompleteModifier = 0;
	//ist 'true', wenn das Token ein bekannter Extraktions-Fehler ist
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
	 * @return if this is the first Token of an InformationEntity: number of tokens to complete the InformationEntity )
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
	 * @return if this is the first token of a modifier: number of tokens to complete the modifier
	 */
	public int getTokensToCompleteModifier() {
		return tokensToCompleteModifier;
	}

	/**
	 * @param tokensToCompleteModifier
	 */
	public void setTokensToCompleteModifier(int tokensToCompleteModifier) {
		this.tokensToCompleteModifier = tokensToCompleteModifier;
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
	 * compares this TextToken to the given PatternToken
	 * @param patternToken
	 * @return returns true, if this TextToken matches the given PatternToken
	 */
	public boolean isEqualsPatternToken(PatternToken patternToken) {
		// compare strings
		if (patternToken.getToken() != null) {
			String[] strings = patternToken.getToken().split("\\|");
			boolean match = false;
			for (String string : strings) {
				match = string.equals(this.token);
				if (match)
					break;
			}
			if (!match)
				return false;
		}
		// compare posTags
		if (patternToken.getPosTag() != null) {
			String[] tags = patternToken.getPosTag().split("\\|");
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
		if (patternToken.getLemma() != null) {
			if (patternToken.getLemma().toUpperCase().equals("IMPORTANCE")) {
				return this.modifierToken;
			}
			else {
				String[] lemmas = patternToken.getLemma().split("\\|");
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
		if (patternToken.isInformationEntity()) {
			return this.ieToken;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(token + "\t" + lemma + "\t" + posTag + "\t");
		if (this.ieToken) {
			sb.append("isInformationEntity" + "\t");
		}
		if (this.noEntity) {
			sb.append("isNoEntity" + "\t");
		}
		if (this.modifierToken) {
			sb.append("is (start of) modifier" + "\t");
		}
		return sb.toString();
	}

}
