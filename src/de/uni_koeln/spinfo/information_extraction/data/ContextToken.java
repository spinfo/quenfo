package de.uni_koeln.spinfo.information_extraction.data;

/**
 * @author geduldia
 * 
 * Represents a single Token of a ContextPattern.
 * The attributes string, lemma and posTag can be null, if values are not specified in pattern
 * A ContextPattern can be marked as optional (= token is not required to match the pattern)
 *
 */
public class ContextToken extends Token {

	
	/**
	 * @param string
	 * @param lemma
	 * @param posTag
	 */
	public ContextToken(String string, String lemma, String posTag) {
		super(string, lemma, posTag);
	}
	
	/**
	 * @param string
	 * @param lemma
	 * @param posTag
	 * @param isInformationEntity
	 */
	public ContextToken(String string, String lemma, String posTag, boolean isInformationEntity) {
		super(string, lemma, posTag, isInformationEntity);
	}
	
	public void setLemma(String lemma){
		this.lemma = lemma;
	}
	
	public void setPosTag(String posTag){
		this.posTag = posTag;
	}
	public void setString(String string){
		this.string = string;
	}
	
	public String getLemma(){
		if(isImportanceTerm()) return "IMPORTANCE";
		return this.lemma;
	}
	
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(string + "\t" + lemma + "\t" + posTag + "\t");
		if(this.informationEntity){
			sb.append("isInformsationEntitiy"+"\t");
		}
		if(this.knowledgeTerm){
			sb.append("isKnowledgeTerm"+"\t");
		}
		if(this.importanceTerm){
			sb.append("is (start of) importanceTerm"+"\t");
		}
		return sb.toString();
	}
}
