package quenfo.de.uni_koeln.spinfo.classification.applicationsjb;

import java.io.File;
import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import quenfo.de.uni_koeln.spinfo.classification.core.classifier.AbstractClassifier;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import quenfo.de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import quenfo.de.uni_koeln.spinfo.classification.core.distance.Distance;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_weighting.AbstractFeatureQuantifier;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_weighting.LogLikeliHoodFeatureQuantifier;
import quenfo.de.uni_koeln.spinfo.classification.jasc.workflow.DerbyDBClassifier;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.ZoneKNNClassifier;

public class ClassifyDerbyDB {

	// Pfad zur Datei mit den Trainingsdaten
	static String trainingdataFile = "classification/data/trainingSets/trainingdata_anonymized.tsv";

	// Anzahl der Stellenanzeigen, die klassifiziert werden sollen (-1 = gesamte
	// Tabelle)
	static int queryLimit = 500;

	// falls nur eine begrenzte Anzahl von SteAs klassifiziert werden soll
	// (s.o.): hier die Startosition angeben
	static int startId = 0;

	// Die SteAs werden (aus Speichergründen) nicht alle auf einmal ausgelesen,
	// sondern Päckchenweise - hier angeben, wieviele jeweils in einem Schwung
	// zusammen verarbeitet werden
	// nach dem ersten Schwung erscheint in der Konsole ein Dialog, in dem man
	// das Programm nochmal stoppen (s), die nächsten xx SteAs klassifizieren
	// (c), oder ohne Unterbrechung zu Ende klassifizieren lassen kann (d)
	static int fetchSize = 100;

	static boolean normalize = true;

	static boolean stem = true;

	static boolean filterSW = true;

	static int[] ngrams = { 3, 4 };

	static boolean continousNGrams = true;

	static int miScore = 0;

	static boolean suffixTree = false;

	private static final String PERSISTENCE_UNIT_NAME = "textkernel";
	private static EntityManager em;
	
	//true, falls alte "ZoneClassifyUnit"-Tabelle gelöscht werden soll
	private static boolean deletePrevious = true;

	public static void main(String[] args) throws IOException {

		// DerbyDB Connection
		EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
		em = factory.createEntityManager();


		DerbyDBClassifier dbClassify = new DerbyDBClassifier(queryLimit, fetchSize,
				startId, trainingdataFile, em);	
		
		
		FeatureUnitConfiguration fuc = new FeatureUnitConfiguration(normalize, stem, filterSW, ngrams,
				continousNGrams, miScore, suffixTree);
		AbstractFeatureQuantifier fq = new LogLikeliHoodFeatureQuantifier();
		AbstractClassifier classifier = new ZoneKNNClassifier(false, 5, Distance.COSINUS);
		ExperimentConfiguration config = new ExperimentConfiguration(fuc, fq, classifier,
				new File(trainingdataFile), null);

		try {
			
			if (deletePrevious) {
				em.getTransaction().begin();
				System.out.println("Delete ...");
				Query deletion = em.createQuery("DELETE FROM JASCClassifyUnit");
				deletion.executeUpdate();
				em.getTransaction().commit();
				
			}

			dbClassify.classify(config);
			
			Query q = em.createQuery("SELECT COUNT(t) from JASCClassifyUnit t");
			System.out.println((long)q.getSingleResult() + " Abschnitte persistiert");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
