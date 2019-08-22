package quenfo.de.uni_koeln.spinfo.classification.jasc.data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.helpers.SingleToMultiClassConverter;

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
//@Inheritance(strategy = InheritanceType.JOINED)
//@MappedSuperclass
@Data
@ToString(of = {}, callSuper = true)
@EqualsAndHashCode(of = {}, callSuper=true)
public class JASCClassifyUnit extends ClassifyUnit {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long jpaID;
	
	private int parentID;
	private int secondParentID = -1;

	private int actualClassID; //TODO JB: warum protected?
	
	private int tableID = -1;
	private String sentences;
	
	@Setter(AccessLevel.NONE)
	boolean[] classIDs;
	private static int NUMBEROFSINGLECLASSES;
	private static int NUMBEROFMULTICLASSES;
	private static SingleToMultiClassConverter CONVERTER;
	
	@Lob
	private String lemmata;
	private String posTags;
	
	@Lob
	private String tokens;

	public JASCClassifyUnit(String content, int parentID, UUID id) {
		super(content,id);
		this.parentID = parentID;
		setActualClassID(-1);
	}

	public JASCClassifyUnit(String content, int parentID, int secondParentID, UUID id, int actualClassID) {
		super(content,id);
		this.parentID = parentID;
		this.secondParentID = secondParentID;
		setActualClassID(actualClassID);
	}
	
	public JASCClassifyUnit(String content, int parentID, int secondParentID){
		this(content, parentID, secondParentID, UUID.randomUUID(), -1);
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
	
	
	public static void setNumberOfCategories(int categoriesNo, int classesNo,
			Map<Integer, List<Integer>> translations) {
		NUMBEROFMULTICLASSES = categoriesNo;
		NUMBEROFSINGLECLASSES = classesNo;
		CONVERTER = new SingleToMultiClassConverter(NUMBEROFSINGLECLASSES, NUMBEROFMULTICLASSES, translations);
	}
	
	
	public void setClassIDs(boolean[] classIDs) {

		if (classIDs == null)
			return;

		this.classIDs = classIDs;
		if (actualClassID == -1) {
			if (CONVERTER != null) {
				actualClassID = CONVERTER.getSingleClass(classIDs);
			} else {
				for (int i = 0; i < classIDs.length; i++) {
					if (classIDs[i]) {
						actualClassID = i + 1;
						return;
					}
				}
			}
		}
	}

	public void setActualClassID(int classID) {
		this.actualClassID = classID;
		if (classIDs == null) {
			if (CONVERTER != null) {
				classIDs = CONVERTER.getMultiClasses(classID);
			} else {
				classIDs = new boolean[NUMBEROFSINGLECLASSES];
				if (classID > 0) {
					classIDs[classID - 1] = true;
				}
			}
		}

	}

	public void setClassIDsAndActualClassID(boolean[] classIDs) {
		// TODO JB: synchronisierung zwischen Array und Int ClassID kaputt
		if (classIDs == null)
			return;

		this.classIDs = classIDs;

		if (CONVERTER != null) {
			actualClassID = CONVERTER.getSingleClass(classIDs);
		} else {
			for (int i = 0; i < classIDs.length; i++) {
				if (classIDs[i]) {
					actualClassID = i + 1;
					return;
				}
			}
		}

	}

	
}
