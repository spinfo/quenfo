package quenfo.de.uni_koeln.spinfo.classification.jasc.data;

import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.data.ZoneClassifyUnit;

/** 
 * A basic unit for all classify tasks.
 * 
 * @author jhermes
 *
 */
/**
 * @author geduldia
 *
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(of = {}, callSuper=true)
public class JASCClassifyUnit extends ZoneClassifyUnit {

	private int parentID;
	
	private int secondParentID = -1;
	
	private int tableID = -1;
	private String sentences;
	private String lemmata;
	private String posTags;
	private String tokens;

	public JASCClassifyUnit(String content, int parentID, UUID id) {
		super(content,id);
		this.parentID = parentID;
	}

	public JASCClassifyUnit(String content, int parentID, int secondParentID, UUID id) {
		super(content,id);
		this.parentID = parentID;
		this.secondParentID = secondParentID;
	}
	
	public JASCClassifyUnit(String content, int parentID, int secondParentID){
		this(content, parentID, secondParentID, UUID.randomUUID());
	}
	
	public JASCClassifyUnit(String content, int parentID) {
		this(content, parentID, UUID.randomUUID());
	}

//	public int getParentID() {
//		return parentID;
//	}
//	
//	public int getSecondParentID() {
//		return secondParentID;
//	}
//	
//	public String toString(){
//		return parentID + "\t" + actualClassID + "\n" +  content + "\n";
//	}
	
//	public String getTokens() {
//		return tokens;
//	}
//
//	public void setTokens(String tokens) {
//		this.tokens = tokens;
//	}
//
//	public String getPosTags() {
//		return posTags;
//	}
//
//	public void setPosTags(String posTags) {
//		this.posTags = posTags;
//	}
//
//	public String getLemmata() {
//		return lemmata;
//	}
//
//	public void setLemmata(String lemmata) {
//		this.lemmata = lemmata;
//	}
//
//	public String getSentences() {
//		return sentences;
//	}
//
//	public void setSentences(String sentencesAsString) {
//		this.sentences = sentencesAsString;
//	}
//
//	public void setTableID(int id){
//		this.tableID = id;
//	}
//	
//	public int getTableID(){
//		return this.tableID;
//	}

	
}
