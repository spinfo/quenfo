package quenfo.de.uni_koeln.spinfo.information_extraction.applicationsjb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import is2.lemmatizer.Lemmatizer;
import is2.tools.Tool;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.preprocessing.ExtractionUnitBuilder;
import quenfo.de.uni_koeln.spinfo.information_extraction.workflow.Extractor;

/**
 * @author geduldia
 * 
 *         workflow to match the already validated competences (from
 *         competences.txt) against the as class 3 classified paragraphs
 * 
 *         input: as class 3 (= applicants profile) classified paragraphs
 *         output: all matching competences together with their containing
 *         sentence
 * 
 */
public class MatchCompetences {

	// Pfad zur Input-DB mit den klassifizierten Paragraphen
//	static String paraInputDB = /* "D:/Daten/sqlite/CorrectableParagraphs.db"; */null; //

	// Ordner in dem die neue Output-DB angelegt werden soll
	static String compMOutputFolder = /* "D:/Daten/sqlite/"; */null;

	// Name der Output-DB
	static String compMOutputDB = null;

	// txt-File mit den validierten Kompetenzen
	// static File competences = new
	// File("information_extraction/data/competences/competences.txt");
	static File notCatComps = null;// new File("information_extraction/data/competences/notCategorized.txt");
									// //TODO refactoring

	// tei-File mit kategorisierten Kompetenzen
	static File catComps = null;// new File("information_extraction/data/competences/tei_index/compdict.tei");

	// Ebene, auf der die Kompetenz zugeordnet werden soll(div1, div2, div3, form,
	// orth)
	static String category = null;// "div3";

	// txt-File mit allen 'Modifier'-Ausdrücken
	static File modifier = null;// new File("information_extraction/data/competences/modifier.txt");

	// static File tokensToRemove = new
	// File("information_extraction/data/competences/fuellwoerter.txt");

	// txt-File zur Speicherung der Match-Statistiken
	static File statisticsFile = null;// new File("information_extraction/data/competences/matchingStats.txt");

	// Anzahl der Paragraphen aus der Input-DB, gegen die gematcht werden soll
	// (-1 = alle)
	static int queryLimit = -1;

	// Falls nicht alle Paragraphen gematcht werden sollen, hier die
	// Startposition angeben
	static int startPos = 0;

	// true, falls Koordinationen in Informationseinheit aufgelöst werden sollen
	static boolean expandCoordinates = false;

	private static final String PERSISTENCE_UNIT_NAME = "textkernel";
	private static EntityManager em;

	public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {

		// DerbyDB Connection
		EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
		em = factory.createEntityManager();

		loadProperties();

		// Verbindung mit Output-DB
		if (!new File(compMOutputFolder).exists()) {
			new File(compMOutputFolder).mkdirs();
		}
		Connection outputConnection = IE_DBConnector.connect(compMOutputFolder + compMOutputDB);
		IE_DBConnector.createExtractionOutputTable2(outputConnection, IEType.COMPETENCE, false);

		// starte Matching
		long before = System.currentTimeMillis();

		Extractor extractor = new Extractor(notCatComps, modifier, catComps, category, IEType.COMPETENCE,
				expandCoordinates);

		List<ExtractionUnit> extractionUnits;

		Query countQuery = em.createQuery("SELECT COUNT(t) from ExtractionUnit t");

		long extractionUnitSize = (long) countQuery.getSingleResult();
		if (extractionUnitSize == 0) {
			System.err.println("ExtractionUnits aus ClassifyUnits(class=3) werden zuerst persistiert.");
			Query cuQuery = em.createQuery("SELECT t from ZoneClassifyUnit t where t.actualClassID = '3'"); // where
																											// t.actualClassID
																											// = '3'

			List<ClassifyUnit> classifyUnits = cuQuery.getResultList();	
			if (classifyUnits.size() == 0) {
				System.err.print("Keine relevanten ClassifyUnits gefunden. Bitte zuerst klassifizieren.");
				System.exit(0);
			}
			System.out.println(classifyUnits.size() + " ClassifyUnits gefunden");
			
			// Lemmatizer (nur für den Fall, dass noch Lemmata generiert werden
			// müssen)
			Tool lemmatizer = new Lemmatizer(
					"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model",
					false);

			extractionUnits = ExtractionUnitBuilder.initializeFromDerby(classifyUnits, lemmatizer, null, null);

			em.getTransaction().begin();
			for (ExtractionUnit eu : extractionUnits) {
				em.persist(eu);
			}
			em.getTransaction().commit();
			System.out.println((long) em.createQuery("SELECT COUNT(t) FROM ExtractionUnit t").getSingleResult()
					+ "ExtractionUnits persistiert");

		}

		extractor.stringMatch(statisticsFile, outputConnection, em, startPos, queryLimit);
		long after = System.currentTimeMillis();
		double time = (((double) after - before) / 1000) / 60;
		if (time > 60.0) {
			System.out.println("\nfinished matching in " + (time / 60) + " hours");
		} else {
			System.out.println("\nfinished matching in " + time + " minutes");
		}
	}

	private static void loadProperties() throws IOException {
		Properties props = new Properties();
		InputStream is = MatchCompetences.class.getClassLoader().getResourceAsStream("config.properties");
		props.load(is);
		String jahrgang = props.getProperty("jahrgang");
//		paraInputDB = props.getProperty("paraInputDB") + jahrgang + ".db";
		compMOutputFolder = props.getProperty("compMOutputFolder");
		compMOutputDB = props.getProperty("compMOutputDB") + jahrgang + ".db";
		catComps = new File(props.getProperty("catComps"));
		notCatComps = new File(props.getProperty("notCatComps"));
		category = props.getProperty("category");
		modifier = new File(props.getProperty("modifier"));
		queryLimit = Integer.parseInt(props.getProperty("maxCount"));
		statisticsFile = new File(props.getProperty("statisticsFile"));
		startPos = Integer.parseInt(props.getProperty("startPos"));
		expandCoordinates = Boolean.parseBoolean(props.getProperty("expandCoordinates"));

	}

}
