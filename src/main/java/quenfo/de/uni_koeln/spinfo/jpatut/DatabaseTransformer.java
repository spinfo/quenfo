package quenfo.de.uni_koeln.spinfo.jpatut;

import java.util.List;
import java.util.Vector;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.sessions.Session;


import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;

public class DatabaseTransformer {

	private static final String PERSISTENCE_UNIT_NAME = "classifyunits";
	private static EntityManagerFactory factory;

	

	public List<ClassifyUnit> readDerby() {

		// DerbyDB Connection
		factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
		EntityManager em = factory.createEntityManager();
		Vector<DatabaseField> fields = em.unwrap(Session.class).getDescriptor(JobAd.class).getFields();
		
		for (DatabaseField f : fields) {
			System.out.println(f.index  + f.getName());
		}
		
		
		// read the existing entries and write to console
		// where t.id = '1'
		Query count = em.createQuery("SELECT count(t) FROM JobAd t where t.CLASSIDS = '3'");
		System.out.println((long) count.getSingleResult());
		return null;
	}
	
	
	public void transformIntoDerby(List<JobAd> classifyUnits) {

		// DerbyDB Connection
		factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
		EntityManager em = factory.createEntityManager();
		
		// read the existing entries and write to console
		// where t.id = '1'
		Query count = em.createQuery("SELECT count(t) FROM JobAd t");
		System.out.println((long) count.getSingleResult());

		int maxLength = 0;
		
		
		em.getTransaction().begin();
		for (JobAd cu : classifyUnits) {
			
			int length = cu.getText().length();
			
			if (length > maxLength) {
				maxLength = length;
				System.out.println(maxLength);
			}
				
			em.persist(cu);
		}

		em.getTransaction().commit();
		em.close();

	}

}
