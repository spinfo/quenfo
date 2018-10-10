package quenfo.de.uni_koeln.spinfo.information_extraction.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author geduldia
 * 
 *         represents a single information instance (a tool or a
 *         competence) defined by an expression of one or more lemmata.
 *
 */
public class InformationEntity {

	//erstes Wort des Kompetenz-/Tool-Ausdrucks
	private String startLemma;
	
	//ist 'true' wenn der Ausdruck aus nur einem Wort (startLemma) besteht
	private boolean singleWordEntity;
	
	//ist 'true', wenn der Ausdruck aus einer Morphemkoordination aufgelöst wurde
	private boolean fromMorphemCoordination;
	
	 //der vollständige Ausdruck als String-List (lemmata.get(0) = startLemma)
	private List<String> lemmata;
	
	//Modifizierer (z.B. 'zwingend erforderlich')
	private String modifier;
	
	//Index des ersten Lemmatas im Satz
	private int firstIndex;
	
	private List<String> coordinations;
	

	/**
	 * @param startLemma 
	 * 			first token of this IE
	 * @param isSingleWordEntity
	 */
	public InformationEntity(String startLemma, boolean isSingleWordEntity, boolean fromMorphemCoordination) {
		this.startLemma = startLemma;
		this.singleWordEntity = isSingleWordEntity;
		this.setFromMorphemCoordination(fromMorphemCoordination);
		if(isSingleWordEntity){
			lemmata = new ArrayList<>();
			lemmata.add(startLemma);
		}
		this.firstIndex = -1;
	}
	
	public InformationEntity(String startLemma, boolean isSingleWordEntity, 
			boolean fromMorphemCoordination, int firstIndex) {
		this(startLemma, isSingleWordEntity, fromMorphemCoordination);
		this.firstIndex = firstIndex;
	}

	public InformationEntity(String startLemma, boolean isSingleWordEntity, 
			boolean fromMorphemCoordination, int firstIndex, String resolvedCoo) {
		this(startLemma, isSingleWordEntity, fromMorphemCoordination, firstIndex);
		List<String> gold = Arrays.asList(resolvedCoo.split(";"));
		this.coordinations = new ArrayList<String>();
		for (String g : gold) {
			this.coordinations.add(g.trim());
		}
	}

	public int getFirstIndex() {
		return firstIndex;
	}
	
	public void setFirstIndex(int firstIndex) {
		this.firstIndex = firstIndex;
	}
	
	public List<String> getCoordinations()	 {
		return coordinations;
	}

	
	/**
	 * @param modifier
	 */
	public void setImportance(String modifier){
		this.modifier = modifier;
	}
	
	/**
	 * @return modifier
	 */
	public String getModifier(){
		return modifier;
	}


	
	/**
	 * @return full expression of this IE
	 */
	@Override
	public String toString(){
		if(lemmata == null) return null;
		StringBuffer sb = new StringBuffer();
		for (String lemma : lemmata) {
			sb.append(lemma+" ");
		}
		return sb.toString().trim();
	}

	/**
	 * @return firstToken
	 */
	public String getStartLemma() {
		return startLemma;
	}

	/**
	 * @param token first token
	 */
	public void setStartLemma(String lemma) {
		this.startLemma = lemma;
	}

	/**
	 * @return singleWordEntity
	 */
	public boolean isSingleWordEntity() {
		return singleWordEntity;
	}

	/**
	 * @param singleWordEntity
	 * 	
	 */
	public void setSingleWordEntity(boolean singleWordEntity) {
		this.singleWordEntity = singleWordEntity;
	}

	/**
	 * @return ordered list of all lemmata
	 */
	public List<String> getLemmata() {
		return lemmata;
	}

	/**
	 * 
	 * appends a new lemma to the list of lemmata
	 * @param lemma
	 */
	public void addLemma(String lemma) {
		if (lemmata == null) {
			lemmata = new ArrayList<String>();
		}
		lemmata.add(lemma);
	}

	/**
	 * @param lemmata
	 */
	public void setExpression(List<String> lemmata) {
		this.lemmata = lemmata;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return new HashCodeBuilder(3, 17).append(startLemma).append(singleWordEntity).append(lemmata).toHashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		InformationEntity am = (InformationEntity) o;
		return new EqualsBuilder().append(startLemma, am.startLemma).append(singleWordEntity, am.singleWordEntity)
				.append(lemmata, am.lemmata).isEquals();
	}


	public boolean isFromMorphemCoordination() {
		return fromMorphemCoordination;
	}


	public void setFromMorphemCoordination(boolean fromMorphemCoordination) {
		this.fromMorphemCoordination = fromMorphemCoordination;
	}


	

}
