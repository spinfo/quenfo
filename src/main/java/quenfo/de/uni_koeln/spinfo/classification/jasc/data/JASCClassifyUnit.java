package quenfo.de.uni_koeln.spinfo.classification.jasc.data;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.data.ZoneClassifyUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;

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
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@MappedSuperclass
@Data
@ToString(of = {}, callSuper = true)
@EqualsAndHashCode(of = {}, callSuper=true)
public class JASCClassifyUnit extends ZoneClassifyUnit {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long jpaID;

	
	
	private int tableID = -1;
	private String sentences;
	
	@Lob
	private String lemmata;
	private String posTags;
	
	@Lob
	private String tokens;

	public JASCClassifyUnit(String content, int parentID, UUID id) {
		super(content,id);
		super.parentID = parentID;
	}

	public JASCClassifyUnit(String content, int parentID, int secondParentID, UUID id) {
		super(content,id);
		super.parentID = parentID;
		super.secondParentID = secondParentID;
	}
	
	public JASCClassifyUnit(String content, int parentID, int secondParentID){
		this(content, parentID, secondParentID, UUID.randomUUID());
	}
	
	public JASCClassifyUnit(String content, int parentID) {
		this(content, parentID, UUID.randomUUID());
	}
	
	/**
	 * default constructor for jpa
	 */
	public JASCClassifyUnit() {
		this("",-1);
	}

	public JASCClassifyUnit(String string, int jahrgang, int zeilenNr, long jobAdJpaID) {
		this(string, jahrgang, zeilenNr);
		super.setJobAdJpaID(jobAdJpaID);
	}

	
}
