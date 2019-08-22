package quenfo.de.uni_koeln.spinfo.information_extraction.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author geduldia
 * 
 *         represents a single information instance (a tool or a
 *         competence) defined by an expression of one or more lemmata.
 *
 */
@Data
@EqualsAndHashCode(of = {"startLemma", "singleWordEntity", "lemmata"})
public class InformationEntity {

	//erstes Wort des Kompetenz-/Tool-Ausdrucks
	@Setter(AccessLevel.NONE)
	private String startLemma;
	
	//ist 'true' wenn der Ausdruck aus nur einem Wort (startLemma) besteht
	@Setter(AccessLevel.NONE)
	private boolean singleWordEntity;
	
	//ist 'true', wenn der Ausdruck aus einer Morphemkoordination aufgelöst wurde
//	@Getter(AccessLevel.NONE)
//	@Setter(AccessLevel.NONE)
//	private boolean fromMorphemCoordination;
	
	 //der vollständige Ausdruck als String-List (lemmata.get(0) = startLemma)
	private List<String> lemmata;
	
	//Modifizierer (z.B. 'zwingend erforderlich')
	private String modifier;
	
	//übergeordnetes Konzept der Kompetenz (des Tools)
	@Setter(AccessLevel.NONE)
	private Set<String> labels;
	
	//Index des ersten Lemmatas im Satz
	@Setter(AccessLevel.NONE)
	private int firstIndex;
	
	//expandierte Koordinationen im Ausdruck (für Evaluierung)
	@Setter(AccessLevel.NONE)
	private List<String> coordinations;
	
	//Tokens des Ausdrucks
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private List<TextToken> originalEntity;
	

	/**
	 * @param startLemma 
	 * 			first token of this IE
	 * @param isSingleWordEntity
	 */
	public InformationEntity(String startLemma, boolean isSingleWordEntity) {
		this.startLemma = startLemma;
		this.singleWordEntity = isSingleWordEntity;
		if(isSingleWordEntity){
			lemmata = new ArrayList<>();
			lemmata.add(startLemma);
		}
		this.firstIndex = -1;
	}
	
	public InformationEntity(String startLemma, boolean isSingleWordEntity, 
			int firstIndex) {
		this(startLemma, isSingleWordEntity);
		this.firstIndex = firstIndex;
	}
	
	public InformationEntity(String startLemma, boolean isSingleWordEntity,
			String label) {
		this(startLemma, isSingleWordEntity);
		this.labels = new HashSet<>();
		labels.add(label);
	}
	
	public InformationEntity(String startLemma, boolean isSingleWordEntity,
			Set<String> labels) {
		this(startLemma, isSingleWordEntity);
		this.labels = labels;
	}
	
	
	public void addLabel(String label) {
		labels.add(label);
	}

	public void setCoordinates(String resolvedCoo) {
		List<String> gold = Arrays.asList(resolvedCoo.split(";"));
		this.coordinations = new ArrayList<String>();
		for (String g : gold) {
			this.coordinations.add(g.trim());
		}
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


}
