package de.uni_koeln.spinfo.information_extraction.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author geduldia
 * 
 *         represents a single information instance (e.g. a tool or a
 *         competence) defined by an expression of one or more lemmata.
 *
 */
public class InformationEntity {

	/**
	 * first token of the entity expression
	 */
	private String firstToken;
	/**
	 * is true if this Information Entity consist of only one lemma
	 */
	 private boolean complete;
	/**
	 * ordered list of all tokens of this entity
	 */
	private List<String> tokens;
	/**
	 * IDs of all Contexts that produced'
	 */
	private Set<Integer> contextIDs;
	
	/**
	 * importance of this Entity
	 */
	private String importance;
	

	
	/**
	 * @return Ids of the producing Contexts of this IE
	 */
	public Set<Integer> getContextIDs() {
		return contextIDs;
	}

	/**
	 * add new contextID
	 * @param contextID
	 */
	public void addContextID(int contextID) {
		if(contextIDs == null)
			contextIDs = new HashSet<Integer>();
		contextIDs.add(contextID);
	}
	
	/**
	 * sets the importance expression of this IE
	 * @param importance
	 */
	public void setImportance(String importance){
		this.importance = importance;
	}
	
	/**
	 * @return importance
	 */
	public String getImportance(){
		return importance;
	}

	/**
	 * @param token 
	 * 			first token of this IE
	 * @param complete 
	 * 			IE is already complete
	 */
	public InformationEntity(String token, boolean complete) {
		this.firstToken = token;
		this.complete = complete;
		if(complete){
			tokens = Arrays.asList(firstToken);
		}
	}
	
	/**
	 * @return full expression of this IE
	 */
	public String getFullExpression(){
		if(tokens == null) return null;
		StringBuffer sb = new StringBuffer();
		for (String token : tokens) {
			sb.append(token+" ");
		}
		return sb.toString().trim();
	}

	/**
	 * @return firstToken
	 */
	public String getToken() {
		return firstToken;
	}

	/**
	 * @param token first token
	 */
	public void setToken(String token) {
		this.firstToken = token;
	}

	/**
	 * @return complete
	 */
	public boolean isComplete() {
		return complete;
	}

	/**
	 * @param complete entity consist of one token only
	 */
	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	/**
	 * @return expression
	 */
	public List<String> getTokens() {
		return tokens;
	}

	/**
	 * 
	 * appends a new lemma to the expression
	 * @param lemma
	 */
	public void addToExpression(String lemma) {
		if (tokens == null) {
			tokens = new ArrayList<String>();
		}
		tokens.add(lemma);
	}

	/**
	 * @param expression full expression of the entity
	 */
	public void setExpression(List<String> expression) {
		this.tokens = expression;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return new HashCodeBuilder(3, 17).append(firstToken).append(complete).append(tokens)/*.append(importance)*/.toHashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		InformationEntity am = (InformationEntity) o;
		return new EqualsBuilder().append(firstToken, am.firstToken).append(complete, am.complete)
				.append(tokens, am.tokens)./*append(importance, am.importance).*/isEquals();
	}

	@Override
	public String toString(){
		return this.getFullExpression();
	}


}
