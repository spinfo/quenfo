package quenfo.de.uni_koeln.spinfo.information_extraction.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import is2.data.SentenceData09;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author geduldia
 * 
 *         A part of a job-ad smaller than a paragraph. (usually a sentence)
 *         Includes a lot of lexial data (lemmata, posTags,...) which is needed
 *         for the information extraction.
 *
 */

@NamedQuery(
		name = "getClassXExtractionUnits", 
		query = "SELECT e FROM ExtractionUnit e JOIN JASCClassifyUnit  c ON e.classifyUnitjpaID = c.jpaID WHERE c.actualClassID = :class"		
		)

@Entity
//@Table(name = "EXTRACTIONUNIT")
@Data
@EqualsAndHashCode(of = { "jobAdID", "secondJobAdID", "classifyUnitID", "sentence" })
@ToString(of = { "sentence" })
public class ExtractionUnit implements Serializable {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long jpaID;

	private UUID sentenceID;

	@Lob
	private String sentence;

	// wird von den Mate-Tools produziert (enthält Lemmata, posTags und Tokens)
//	private SentenceData09 sentenceData = new SentenceData09();

	// ID der beinhaltenden ClassifyUnit
	private UUID classifyUnitID;

	// Table-ID der beinhaltenden ClassifyUnit
	private int classifyUnitTableID;
	
	// Derby ID der beinhaltenden ClassifyUnit
	private long classifyUnitjpaID;

	@Getter(AccessLevel.NONE)
	private String[] tokens;

	@Getter(AccessLevel.NONE)
	private String[] lemmata;

	@Getter(AccessLevel.NONE)
	private String[] posTags;
	//
	private boolean lexicalDataIsStoredInDB;

	/**
	 * first ID of the containing JobAd (Jahrgang)
	 */
	private int jobAdID;
	/**
	 * second ID of the containing JobAd (Zeilennummer)
	 */
	private int secondJobAdID;
	/**
	 * Tokens in this sentence
	 */
	private List<TextToken> tokenObjects = new ArrayList<TextToken>();

	public ExtractionUnit(String sentence) {
		this.sentenceID = UUID.randomUUID();
		this.sentence = sentence;
	}

	public ExtractionUnit() {
		this.sentenceID = UUID.randomUUID();
	}

	/**
	 * @return tokens produced by the MateTool
	 */
	public String[] getTokens() {
		if (this.tokens != null)
			return tokens;
//		if (this.sentenceData != null)
//			return sentenceData.forms;
		return null;
	}

	/**
	 * @return lemmata produced by the MateTool
	 */
	public String[] getLemmata() {
		if (this.lemmata != null)
			return this.lemmata;
//		if (this.sentenceData != null)
//			return sentenceData.plemmas;
		return null;
	}

	/**
	 * @return posTags produced by the MateTool
	 */
	public String[] getPosTags() {
		if (this.posTags != null)
			return this.posTags;
//		if (this.sentenceData != null)
//			return sentenceData.ppos;
		return null;
	}

//	/**
//	 * @return morphTags produced by the MateTool
//	 */
//	@Deprecated
//	public String[] getMorphTags() {
//		return sentenceData.pfeats;
//	}

	/**
	 * creates the List of Token-objects for this ExtractionUnit Sets a Root-Token
	 * as first Token and an End-Token as last Token
	 * 
	 * @param sentenceData
	 */
	public void setSentenceData(SentenceData09 sentenceData) {
//		this.sentenceData = sentenceData;
		TextToken token = null;
//		String[] tokens = getTokens();
//		String[] lemmas = getLemmata();
//		String[] posTags = getPosTags();
		
		this.tokens = sentenceData.forms;
		this.lemmata = sentenceData.plemmas;
		this.posTags = sentenceData.ppos;
		
		for (int i = 0; i < tokens.length; i++) {
			if (posTags == null) {
				token = new TextToken(tokens[i], lemmata[i], null);
			} else {
				token = new TextToken(tokens[i], lemmata[i], posTags[i]);
			}
			this.tokenObjects.add(token);
		}
		token = new TextToken(null, "<end-LEMMA>", "<end-POS>");
		
		this.tokenObjects.add(token);
	}

	public void deleteData() {
//		this.sentenceData = null;
		this.tokenObjects = null;
	}


}
