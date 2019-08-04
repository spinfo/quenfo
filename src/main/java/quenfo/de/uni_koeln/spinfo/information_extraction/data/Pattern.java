package quenfo.de.uni_koeln.spinfo.information_extraction.data;

import java.util.ArrayList;
import java.util.List;

import de.uni_koeln.spinfo.data.Token;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * @author geduldia
 * 
 * Represents an Extraction-Pattern to identify Information (e.g. competences or tools) in JobAds.
 * Consist of several PatternTokens and a Pointer to the Token(s) which has to be extracted in case of a match.
 *
 */
@Data
public class Pattern {
	
	@Setter(AccessLevel.NONE)
	private List<PatternToken> tokens = new ArrayList<PatternToken>();
	private List<Integer> extractionPointer = new ArrayList<Integer>();
	private String description;
	private int id;
	
	/**
	 * adds a new token to this Pattern
	 * @param token toAdd
	 */
	public void addToken(PatternToken token){
		tokens.add(token);
	}
	
	/**
	 * @return number of tokens in this Pattern
	 */
	public int getSize(){
		return tokens.size();
	}
	
	/**
	 * returns the Token at the given index
	 * @param index
	 * @return token at index
	 */
	public Token getTokenAt(int index){
		return tokens.get(index);
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("ID:\t"+id+"\n");
		sb.append("NAME:\t"+description+"\n");
		for (int t = 0; t < tokens.size(); t++) {
			de.uni_koeln.spinfo.data.Token token = tokens.get(t);
			sb.append("TOKEN:\t");
			sb.append(token.getToken()+"\t");
			sb.append(token.getLemma()+"\t");
			sb.append(token.getPosTag()+"\t");
			sb.append(token.isInformationEntity()+"\n");
		}	
		sb.append("EXTRACT:\t");
		for (Integer i : extractionPointer) {
			sb.append(i+",");
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append("\n");
		sb.append("CONF:\t"+"0.0");
		return sb.toString();
	}
	
//	/**
//	 * @return list of all tokens in this Pattern
//	 */
//	public List<PatternToken> getTokens(){
//		return tokens;
//	}
	
//	/**
//	 * @return pattern-id
//	 */
//	public int getId() {
//		return id;
//	}
//
//	/**
//	 * @param id
//	 */
//	public void setId(int id) {
//		this.id = id;
//	}

	
//	/**
//	 * @param description
//	 */
//	public void setDescription(String description){
//		this.description = description;
//	}
//	
//	/**
//	 * @return description
//	 */
//	public String getDescription(){
//		return description;
//	}
	


//	/**
//	 * returns the index/the indices which point to the Patterns to extract in case of a match
//	 * @return list of indices
//	 */
//	public List<Integer> getPointer(){
//		return extractionPointer;
//	}
//	
//	/**
//	 * 
//	 * @param extractionPointer
//	 */
//	public void setPointer(List<Integer> extractionPointer){
//		this.extractionPointer = extractionPointer;
//	}

	

}
