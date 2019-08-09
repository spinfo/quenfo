package quenfo.de.uni_koeln.spinfo.jpatut;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.sessions.Session;

import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;

public class DatabaseTransformer {
	
	private Logger log = Logger.getLogger(DatabaseTransformer.class);

	private EntityManager em;

	public DatabaseTransformer(EntityManager em) {
		this.em = em;
		
//		
//		Vector<DatabaseField> fields = em.unwrap(Session.class).getDescriptor(ExtractionUnit.class).getFields();
//		System.out.println("ExtractionUnit fields: ");
//		for (DatabaseField f : fields) {
//			System.out.println(f.index + " " + f.getName() + " " + f.typeName);
//		}
//		
//		fields = em.unwrap(Session.class).getDescriptor(JASCClassifyUnit.class).getFields();
//		System.out.println("JASCClassifyUnit fields: ");
//		for (DatabaseField f : fields) {
//			System.out.println(f.index + " " + f.getName() + " " + f.getTypeName());
//		}
//		
//		
		
		
	}

	public List<ClassifyUnit> readClassifyUnits() {

		Query count = em.createQuery("SELECT COUNT(t) FROM JASCClassifyUnit t");
		log.info((long) count.getSingleResult() + " ClassifyUnits");

		Query query = em.createQuery("SELECT t from JASCClassifyUnit t"); 
		List<ClassifyUnit> result = query.getResultList();

		return result;
	}
	
	/**
	 * reads extraction units from DerbyDB
	 * @param classID id of "parent" classify unit
	 * @return
	 */
	public List<ExtractionUnit> readExtractionUnits(int classID) {
		
		// Select all ExtractionUnits coming from "Class-3"-Paragraphs
		Query query = em.createNamedQuery("getClassXExtractionUnits");
		query.setParameter("class", classID);
		
//		Query query = em.createQuery("SELECT e FROM ExtractionUnit e JOIN e.classifyUnitjpaID c WHERE c.actualClassID = '3'");
		List<ExtractionUnit> result = query.getResultList();
		
		return result;
	}

	public void persistToDerby(List<ClassifyUnit> classifyUnits) {

		em.getTransaction().begin();
		for (ClassifyUnit cu : classifyUnits) {
			em.persist(cu);
		}

		em.getTransaction().commit();
	}

}
