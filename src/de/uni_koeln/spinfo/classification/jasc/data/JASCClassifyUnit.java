package de.uni_koeln.spinfo.classification.jasc.data;

import java.util.UUID;

import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;

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
public class JASCClassifyUnit extends ZoneClassifyUnit {

	
	
	private int parentID;
	private int secondParentID = -1;

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

	public int getParentID() {
		return parentID;
	}
	
	public int getSecondParentID() {
		return secondParentID;
	}
	
	public String toString(){
		return parentID + "\t" + actualClassID + "\n" +  content + "\n";
	}

	
}
