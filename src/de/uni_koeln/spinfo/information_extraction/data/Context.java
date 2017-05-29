package de.uni_koeln.spinfo.information_extraction.data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author geduldia
 * 
 * Represents a ContextPattern to identify InformationEntities.
 * Consist of several ContextTokens which has to be matched and an ExtractionPointer which points to the Token to be extract in case of a match.
 *
 */
public class Context {
	
	
	private List<ContextToken> tokens = new ArrayList<ContextToken>();
	private List<Integer> extractionPointer = new ArrayList<Integer>();
	private String description;
	private int id;
	
	/**
	 * @return context-id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}

	
	/**
	 * @param description
	 */
	public void setDescription(String description){
		this.description = description;
	}
	
	/**
	 * @return description
	 */
	public String getDescription(){
		return description;
	}
	
	/**
	 * adds a new token to the context
	 * @param token toAdd
	 */
	public void addToken(ContextToken token){
		tokens.add(token);
	}
	
	/**
	 * @return number of tokens this context consists of
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
	/**
	 * @return list of all tokens in this context
	 */
	public List<ContextToken> getTokens(){
		return tokens;
	}

	/**
	 * @return list of extractionPointers
	 */
	public List<Integer> getExtractionPointer(){
		return extractionPointer;
	}
	
	/**
	 * 
	 * @param extractionPointer
	 */
	public void setEUPointer(List<Integer> extractionPointer){
		this.extractionPointer = extractionPointer;
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
			Token token = tokens.get(t);
			sb.append("TOKEN:\t");
			sb.append(token.getString()+"\t");
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
}
