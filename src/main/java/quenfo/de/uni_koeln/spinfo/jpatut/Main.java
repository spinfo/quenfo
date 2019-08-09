package quenfo.de.uni_koeln.spinfo.jpatut;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.eclipse.persistence.exceptions.DatabaseException;

import is2.lemmatizer.Lemmatizer;
import is2.tag.Tagger;
import is2.tools.Tool;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.data.ZoneClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.helpers.SingleToMultiClassConverter;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.preprocessing.ExtractionUnitBuilder;

public class Main {

	private static Logger log = Logger.getLogger(Main.class);

	private static final String PERSISTENCE_UNIT_NAME = "textkernel";
	private static EntityManagerFactory factory;
	private static EntityManager em;
	private static DatabaseTransformer dt;

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {

		// DerbyDB Connection
		EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
		em = factory.createEntityManager();
		dt = new DatabaseTransformer(em);

		boolean createNew = false;
		
		List<ExtractionUnit> extractionUnits = null;

		if (createNew) {
			writeSQLtoDerby(200);
			extractionUnits = writeCUtoEU();
		} else {
			extractionUnits = dt.readExtractionUnits(3);
			log.info("" + extractionUnits.size());	
		}
		
		em.close();
		
		
		for(ExtractionUnit eu : extractionUnits) {
//			System.out.println(eu.getLemmata());
			List<String> lemmata = Arrays.asList(eu.getLemmata());
			if (lemmata.contains("beratung"))
				System.out.println(eu.getSentence());
		}

	}

	private static void writeSQLtoDerby(int limit) throws ClassNotFoundException, SQLException {

		log.info("Read SQLite and write to DerbyDB");

		List<ClassifyUnit> allClassifyUnits = readClassifyUnits();

		List<ClassifyUnit> classifyUnits = null;
		if (limit < 0)
			classifyUnits = new ArrayList<>(allClassifyUnits);
		else
			classifyUnits = new ArrayList<ClassifyUnit>(allClassifyUnits.subList(0, limit));

//		DatabaseTransformer dt = new DatabaseTransformer(em);
		dt.persistToDerby(classifyUnits);

	}

	public static List<ClassifyUnit> readClassifyUnits() throws SQLException, ClassNotFoundException {
		Connection connection;
		Class.forName("org.sqlite.JDBC");
		connection = DriverManager
				.getConnection("jdbc:sqlite:C:/sqlite/classification/CorrectableParagraphs_textkernel.db");

		connection.setAutoCommit(false);

		String query = "SELECT * FROM ClassifiedParagraphs";
		ResultSet result;
		PreparedStatement prepStmt = connection.prepareStatement(query);

		result = prepStmt.executeQuery();
		List<ClassifyUnit> classifyUnits = new ArrayList<>();
		ClassifyUnit classifyUnit;

		while (result.next()) {

			int class1 = result.getInt("ClassOne");
			int class2 = result.getInt("ClassTwo");
			int class3 = result.getInt("ClassThree");
			int class4 = result.getInt("ClassFour");

			boolean[] classIDs = new boolean[4];
			classIDs[0] = (class1 == 1);
			classIDs[1] = (class2 == 1);
			classIDs[2] = (class3 == 1);
			classIDs[3] = (class4 == 1);

			// TODO JB: multiclass

			classifyUnit = new JASCClassifyUnit(result.getString("Text"), result.getInt("Jahrgang"),
					result.getInt("ZEILENNR"));
			((JASCClassifyUnit) classifyUnit).setTableID(result.getInt("ID"));
			((ZoneClassifyUnit) classifyUnit).setClassIDs(classIDs);
			classifyUnits.add(classifyUnit);
		}
		result.close();
		prepStmt.close();
		connection.commit();

		return classifyUnits;
	}

	/**
	 * reads ClassifyUnits from DerbyDB, creates ExtractionUnits from each
	 * ClassifyUnit and persists ExtractionUnits (batch size = 1000)
	 * @return 
	 * 
	 * @throws IOException
	 */
	private static List<ExtractionUnit> writeCUtoEU() throws IOException {

		log.info("Read ClassifyUnit DB and write to ExtractionUnit DB");

		List<ExtractionUnit> extractionUnits = new ArrayList<ExtractionUnit>();

		List<ClassifyUnit> classifyUnits = dt.readClassifyUnits();

		Tool lemmatizer = new Lemmatizer(
				"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model",
				false);
		Tool tagger = new Tagger(
				"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/tag-ger-3.6.model");

		// DerbyDB Connection
		factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
		EntityManager em = factory.createEntityManager();

		List<ClassifyUnit> current = new ArrayList<>();
		for (int i = 0; i < classifyUnits.size(); i++) {

			current.add(classifyUnits.get(i));

			if (current.size() == 1000) {

				List<ExtractionUnit> currentEUs = ExtractionUnitBuilder.initializeIEUnits(current, lemmatizer,
						null, tagger);
				extractionUnits.addAll(currentEUs);
				System.out.println(currentEUs.size() + "  " + current.size() + "  " + i);

				em.getTransaction().begin();
				for (ExtractionUnit eu : currentEUs) {
					em.persist(eu);
				}
				try {
					em.getTransaction().commit();
				} catch (DatabaseException e) {
					System.err.print(i);
				}

				current = new ArrayList<>();
			}
		}
		List<ExtractionUnit> currentEUs = ExtractionUnitBuilder.initializeIEUnits(current, lemmatizer, null,
				tagger);
		extractionUnits.addAll(currentEUs);
		
		em.getTransaction().begin();
		for (ExtractionUnit eu : currentEUs) {
			em.persist(eu);
		}
		em.getTransaction().commit();

		em.close();
		
		return extractionUnits;
	}

}
