package quenfo.de.uni_koeln.spinfo.information_extraction.data;

import de.uni_koeln.spinfo.data.Token;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author geduldia
 * 
 * Represents a single Token as part of an Extraction-Pattern.
 * The attributes string, lemma and posTag can be null, if values are not specified in the pattern
 *
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class PatternToken extends Token {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;



	/**
	 * @param string
	 * @param lemma
	 * @param posTag
	 */
	public PatternToken(String string, String lemma, String posTag) {
		super(string, lemma, posTag);
	}
	
	/**
	 * @param string
	 * @param lemma
	 * @param posTag
	 * @param isInformationEntity
	 */
	public PatternToken(String string, String lemma, String posTag, boolean isInformationEntity) {
		super(string, lemma, posTag, isInformationEntity);
	}
	
	@Override
	public String getLemma(){
		if(isModifier()) return "IMPORTANCE";
		return this.lemma;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(token + "\t" + lemma + "\t" + posTag + "\t");
		if(this.ieToken){
			sb.append("isInformationEntity"+"\t");
		}
		if(this.modifierToken){
			sb.append("is (start of) modifier"+"\t");
		}
		return sb.toString();
	}
}
