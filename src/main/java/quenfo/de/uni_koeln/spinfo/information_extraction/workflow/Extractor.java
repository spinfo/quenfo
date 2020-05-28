package quenfo.de.uni_koeln.spinfo.information_extraction.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.db_io.Class_DBConnector;
import quenfo.de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.helpers.SingleToMultiClassConverter;
import quenfo.de.uni_koeln.spinfo.core.helpers.PropertiesHandler;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.Pattern;
import quenfo.de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.preprocessing.ExtractionUnitBuilder;
import is2.lemmatizer.Lemmatizer;
import is2.tag.Tagger;
import is2.tools.Tool;

/**
 * TODO JB: Refactoring
 * 20.02.19: reWriteFiles @Deprecated, auskommentierte Methoden gelöscht
 */

/**
 * @author geduldia
 * 
 *         The main Extractor. Extracts Competences/Tools via Patterns and via
 *         StringMatching
 *
 */
public class Extractor {

	Logger log = Logger.getLogger(getClass());

	private IEJobs jobs;
	private IEType type;
	private Set<String> knownEntities = new HashSet<String>();
	private Set<String> noEntities = new HashSet<String>();
	private Map<String, String> possCompoundSplits = new HashMap<String, String>();
	private File possCompoundsFile, splittedCompoundsFile;
	private File entitiesFile;
	private File noEntitiesFile;
	// TODO Konstruktoren refactoring
	// Konstruktor für die Matching-Workflows
	/**
	 * @param entities  file of already known/extracted entities
	 * @param modifiers files with modifier-terms
	 * @param type      type of information
	 * @throws IOException
	 */
	/**
	 * Constructor to match Tools
	 * 
	 * @param entities
	 * @param modifiers
	 * @param type
	 * @param resolveCoordinations
	 * @throws IOException
	 */
	public Extractor(File entities, File modifiers, IEType type, boolean resolveCoordinations) throws IOException {
		this.entitiesFile = entities;
		this.type = type;
		this.possCompoundsFile = new File(PropertiesHandler.getPossibleCompounds());
		this.splittedCompoundsFile = new File(PropertiesHandler.getSplittedCompounds());
		this.jobs = new IEJobs(entities, null, modifiers, null, type, resolveCoordinations, possCompoundsFile,
				splittedCompoundsFile);
		initialize();

	}

	/**
	 * Constructor to match Competences
	 * 
	 * @param entities
	 * @param modifiers
	 * @param amsComps
	 * @param category
	 * @param type
	 * @param resolveCoordinations
	 * @throws IOException
	 */
	public Extractor(File entities, File modifiers, File amsComps, String category, IEType type,
			boolean resolveCoordinations) throws IOException {
		this.entitiesFile = entities;
		this.type = type;
		this.possCompoundsFile = new File(PropertiesHandler.getPossibleCompounds());
		this.splittedCompoundsFile = new File(PropertiesHandler.getSplittedCompounds());
		this.jobs = new IEJobs(entities, null, amsComps, category, modifiers, null, type, resolveCoordinations,
				possCompoundsFile, splittedCompoundsFile);
		initialize();

	}

	/**
	 * Constructor to extract new Competences/Tools
	 * 
	 * @param outputConnection
	 * @param entitiesFile     file with the already known/extracted entities
	 *                         (competences/tools)
	 * @param noEntitiesFile   file with known typical mistakes
	 * @param contexts         file with the context-patterns
	 * @param type             type of information (tools o.competences)
	 * @throws IOException
	 * @throws SQLException
	 */
	public Extractor(Connection outputConnection, File entitiesFile, File noEntitiesFile, File contexts, File modifier,
			IEType type, boolean resolveCoordinations) throws IOException, SQLException {
		this.entitiesFile = entitiesFile;
		this.noEntitiesFile = noEntitiesFile;
		this.type = type;
		this.possCompoundsFile = new File(PropertiesHandler.getPossibleCompounds());
		this.splittedCompoundsFile = new File(PropertiesHandler.getSplittedCompounds());
		this.jobs = new IEJobs(entitiesFile, noEntitiesFile, modifier, contexts, type, resolveCoordinations,
				possCompoundsFile, splittedCompoundsFile);// new
		// contexts,
		// type);
		if (outputConnection != null) {
			// liest aus der Output-DB, die - falls vorhanen - manuell
			// validierten Entitäten aus dem vorherigen Extraktionsdurchgang ein
			// (Da im BIBB aber nicht mehr manuell annotiert wird, passiert hier
			// nichts mehr)
			try {
				// Einlesen der mit 1 (= isCompetence) annotierten Kompetenzen
				knownEntities = IE_DBConnector.readAnnotatedEntities(outputConnection, 1, type);
				// Einlesen der mit 0 (= isNoCompetence) annotierten Kompetenzen
				noEntities = IE_DBConnector.readAnnotatedEntities(outputConnection, 0, type);
				// Weitergabe an die IEJobs-Dinstanz
				jobs.addKnownEntities(knownEntities);
				jobs.addNoEntities(noEntities);
			} catch (SQLException e) {
				// DB enthält keine annotierten Einträge
			}
		}
		initialize();
	}

	private void initialize() {
		Map<Integer, List<Integer>> translations = new HashMap<Integer, List<Integer>>();
		List<Integer> categories = new ArrayList<Integer>();
		categories.add(1);
		categories.add(2);
		translations.put(5, categories);
		categories = new ArrayList<Integer>();
		categories.add(2);
		categories.add(3);
		translations.put(6, categories);
		SingleToMultiClassConverter stmc = new SingleToMultiClassConverter(6, 4, translations);
		JASCClassifyUnit.setNumberOfCategories(stmc.getNumberOfCategories(), stmc.getNumberOfClasses(),
				stmc.getTranslations());
	}

	/**
	 * Coordinates the Information-Extraction (1) reads the Paragraphs from the
	 * Input-DB (2) splits paragraphs into sentences and initializes ExtractionUnits
	 * (3) hands the ExtractionUnits to the IEJobs-Instance to execute the
	 * extraction (4) stores the extracted Entites in the Output-DB
	 * 
	 * @param startPos
	 * @param maxCount
	 * @param tablesize        number of rows (= paragraphs) in the input-table
	 * @param inputConnection
	 * @param outputConnection
	 * @throws IOException
	 * @throws SQLException
	 */
	public Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extract(int startPos, int maxCount, int paragraphsPerRound,
			int tablesize, Connection inputConnection, Connection outputConnection) throws IOException, SQLException {

		// Falls die lexikalischen Infos (lemmata, POS-Tags etc.) noch nicht in
		// der Input-DB hinterlegt sind, dann werden Sie in diesem Workflow
		// generiert und im Anschluss gespeichert.
		// Dafür werden hier (falls nocht nicht vorhanden) die entsprechenden
		// Spalten eingefügt.
		Class_DBConnector.addColumn(inputConnection, "ExtractionUnits", "ClassifiedParagraphs");
		Class_DBConnector.addColumn(inputConnection, "Lemmata", "ClassifiedParagraphs");
		Class_DBConnector.addColumn(inputConnection, "POSTags", "ClassifiedParagraphs");
		// Lemmatizer und Tagger (nur für den Fall, dass noch lexikalische Infos
		// generiert werden müssen)

		Tool lemmatizer = new Lemmatizer(PropertiesHandler.getLemmatizerModel(), false);
		Tool tagger = new Tagger(PropertiesHandler.getTaggerModel());

		List<ClassifyUnit> classifyUnits = null;
		List<ExtractionUnit> extractionUnits = null;
		Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extractions = null;
		Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> allExtractions = new HashMap<ExtractionUnit, Map<InformationEntity, List<Pattern>>>();

		int readParagraphs = 0;
		int offset = startPos;
		if (maxCount > -1 && paragraphsPerRound > maxCount) {
			paragraphsPerRound = maxCount;
		}

		long before;
		long after;
		double time;
		boolean finished = false;

		while (!finished) {
			before = System.currentTimeMillis();

			// Einlesen der Paragraphen
			if (maxCount > -1 && readParagraphs + paragraphsPerRound > maxCount) {
				paragraphsPerRound = maxCount - readParagraphs;
			}
			log.info("read ClassifyUnits from DB " + offset + " - " + (offset + paragraphsPerRound));
			classifyUnits = IE_DBConnector.readClassifyUnits(paragraphsPerRound, offset, inputConnection, jobs.type);
			if (classifyUnits.isEmpty()) {
				finished = true;
			}

			readParagraphs += classifyUnits.size();
			if (readParagraphs >= maxCount) {
				finished = true;
			}
			if (startPos + readParagraphs >= tablesize) {
				finished = true;
			}
			offset = offset + classifyUnits.size();

			// Paragraphen in Sätze splitten und in ExtractionUnits überführen
			log.info("initialize ExtractionUnits");
			extractionUnits = ExtractionUnitBuilder.initializeIEUnits(classifyUnits, lemmatizer, null, tagger);
			log.info("--> " + extractionUnits.size());

			// falls bei der Initialisierung noch lexikaische Daten generiert
			// werden mussten, werden diese fürs nächste Mal in der Input-DB
			// gespeichert.
			IE_DBConnector.upateClassifyUnits(inputConnection, extractionUnits);

			// Informationsextraktion
			jobs.annotateTokens(extractionUnits);
			log.info("extract " + type.name().toLowerCase());
			extractions = jobs.extractEntities(extractionUnits, lemmatizer);

			possCompoundSplits.putAll(jobs.getNewCompounds());

			// Entfernen der bereits bekannten Entitäten
			extractions = removeKnownEntities(extractions);

			allExtractions.putAll(extractions);

			after = System.currentTimeMillis();
			time = (((double) after - before) / 1000);
			log.info("time: " + time + "\n");
			classifyUnits = null;
			extractions = null;
			extractionUnits = null;
//			this.jobs = new IEJobs(entitiesFile, noEntitiesFile, modifier, contexts, type, resolveCoordinations,
//					possCompoundsFile, splittedCompoundsFile);
			// TODO JB: warum IEJobs neu instanziiert?
			jobs.addKnownEntities(knownEntities);
			jobs.addNoEntities(noEntities);
		}
		lemmatizer = null;
		tagger = null;

		jobs.mergeInformationEntities(allExtractions);

		if (allExtractions.isEmpty()) {
			log.info("no new " + jobs.type.name() + "s found\n");
		} else {
			// Speichern der Extraktionsergebnisse in der Output-DB
			String outputPath = outputConnection.getMetaData().getURL().replace("jdbc:sqlite:", "");
			log.info("write extracted " + type.name().toLowerCase() + "in output-DB " + outputPath);
			IE_DBConnector.createExtractionOutputTable(outputConnection, type, true);
			if (type == IEType.TOOL) {
				IE_DBConnector.writeToolExtractions(allExtractions, outputConnection, true);
			} else {
//				if (type == IEType.COMPETENCE_IN_3) {
				IE_DBConnector.writeCompetenceExtractions(allExtractions, outputConnection, true);
			}

		}
		// schreibt die txt-Files (competences.txt & noCompetences.txt) neu, da
		// zu Beginn evtl. neue manuell annotierte eingelesen wurden
		// (eigentlich nicht mehr notwendig, da im BIBB nicht mehr manuell
		// annotiert wird)
		reWriteFiles();
		if (possCompoundSplits.size() > 0) {
			log.info("write new compounds to evaluate in: " + possCompoundsFile.getAbsolutePath());
			writeNewCoordinations();
		}
		return allExtractions;
	}

	private Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> matchBatch(List<ExtractionUnit> extractionUnits,
			Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> stringMatches, Tool lemmatizer)
			throws SQLException, IOException {

		stringMatches = jobs.extractByStringMatch(extractionUnits, lemmatizer);
		stringMatches = jobs.mergeInformationEntities(stringMatches);

		// set Modifiers
//		if (jobs.type == IEType.COMPETENCE_IN_3) {
		if (jobs.type != IEType.TOOL) {
			jobs.setModifiers(stringMatches);
		}

		return stringMatches;
	}

	/**
	 * Match InformationEntities in ClassifyUnits from SQLite DB
	 * 
	 * @param statisticsFile
	 * @param inputConnection
	 * @param outputConnection
	 * @param maxCount
	 * @param startPos
	 * @throws SQLException
	 * @throws IOException
	 */
	public void stringMatch(File statisticsFile, Connection inputConnection,
			Connection outputConnection/* , String outputDBPath */, int maxCount, int startPos, int paragraphsPerRound)
			throws SQLException, IOException {

		// Falls die lexikalischen Infos (sentences, lemmata) noch nicht in der
		// Input-DB hinterlegt sind, dann werden Sie in diesem Workflow
		// generiert und im Anschluss gespeichert.
		// Dafür werden hier (falls nocht nicht vorhanden) die entsprechenden
		// Spalten eingefügt.
		Class_DBConnector.addColumn(inputConnection, "ExtractionUnits", "ClassifiedParagraphs");
		Class_DBConnector.addColumn(inputConnection, "Lemmata", "ClassifiedParagraphs");
		Class_DBConnector.addColumn(inputConnection, "POSTags", "ClassifiedParagraphs");
		// Lemmatizer (nur für den Fall, dass noch Lemmata generiert werden
		// müssen)
		Tool lemmatizer = new Lemmatizer(PropertiesHandler.getLemmatizerModel(), false);

		List<ClassifyUnit> classifyUnits = null;
		List<ExtractionUnit> extractionUnits = null;
		Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> stringMatches = null;

		int readParagraphs = 0;
		int offset = startPos;

		if (maxCount > -1 && paragraphsPerRound > maxCount) {
			paragraphsPerRound = maxCount;
		}

		Map<String, Integer> matchCounts = new HashMap<String, Integer>();
		while (true) {

			// Einlesen der Paragraphen
			if (maxCount > -1 && readParagraphs + paragraphsPerRound > maxCount) {
				paragraphsPerRound = maxCount - readParagraphs;
			}
			log.info("read ClassifyUnit " + offset + " - " + (offset + paragraphsPerRound));
			classifyUnits = IE_DBConnector.readClassifyUnits(paragraphsPerRound, offset, inputConnection, jobs.type);
			if (classifyUnits.isEmpty()) {
				break;
			}
			readParagraphs += classifyUnits.size();
			offset = offset + classifyUnits.size();

			// Paragraphen in Sätze splitten und in ExtractionUnits überführen
			log.info("initialize ExtractionUnits");
			extractionUnits = ExtractionUnitBuilder.initializeIEUnits(classifyUnits, lemmatizer, null, null);
			log.info("--> " + extractionUnits.size() + " ExtractionUnits");

			// Matching
			jobs.annotateTokens(extractionUnits);

			stringMatches = matchBatch(extractionUnits, stringMatches, lemmatizer);

			// write results in DB
			if (stringMatches.isEmpty()) {
				log.info("no " + jobs.type.name() + " matches found\n");
			} else {
				String outputPath = outputConnection.getMetaData().getURL().replace("jdbc:sqlite:", "");
				log.info("write results in output-DB: " + outputPath + "\n");
				if (jobs.type == IEType.TOOL) {
					IE_DBConnector.writeToolExtractions(stringMatches, outputConnection, false);
				} else {
//				if (jobs.type == IEType.COMPETENCE_IN_3) {
					IE_DBConnector.writeCompetenceExtractions(stringMatches, outputConnection, false);
				}

			}

			updateMatchCount(stringMatches, matchCounts);
			if (maxCount > -1 && readParagraphs >= maxCount)
				break;
		}
		// write statisticsFile
		log.info("write Statistics-File to: " + statisticsFile.getAbsolutePath());
		writeStatistics(matchCounts, statisticsFile);
		if (possCompoundSplits.size() > 0) {
			log.info("write new compounds to evaluate in: " + possCompoundsFile.getAbsolutePath());
			writeNewCoordinations();
		}
		lemmatizer = null;
	}

	private Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> removeKnownEntities(
			Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> allExtractions) {
		Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> toReturn = new HashMap<ExtractionUnit, Map<InformationEntity, List<Pattern>>>();
		for (ExtractionUnit extractionUnit : allExtractions.keySet()) {

			Map<InformationEntity, List<Pattern>> ies = allExtractions.get(extractionUnit);
			Map<InformationEntity, List<Pattern>> filterdIes = new HashMap<InformationEntity, List<Pattern>>();
			for (InformationEntity ie : ies.keySet()) {
				Set<InformationEntity> knownIEs = jobs.entities.get(ie.getStartLemma());
				if (knownIEs == null || (!knownIEs.contains(ie))) {
					filterdIes.put(ie, ies.get(ie));
				}
			}
			if (!filterdIes.isEmpty()) {
				toReturn.put(extractionUnit, filterdIes);
			}
		}
		return toReturn;
	}

	@Deprecated
	private void reWriteFiles() throws IOException {

		if (!entitiesFile.exists()) {
			entitiesFile.createNewFile();
		} else {
			BufferedReader in = new BufferedReader(new FileReader(entitiesFile));
			String line = in.readLine();
			while (line != null) {
				knownEntities.add(line);
				line = in.readLine();
			}
			in.close();
		}
		PrintWriter out = new PrintWriter(new FileWriter(entitiesFile));
		for (String string : knownEntities) {
			out.write(string + "\n");
		}
		out.close();
		if (!noEntitiesFile.exists()) {
			noEntitiesFile.createNewFile();
		} else {
			BufferedReader in = new BufferedReader(new FileReader(noEntitiesFile));
			String line = in.readLine();
			while (line != null) {
				noEntities.add(line);
				line = in.readLine();
			}
			in.close();
		}
		out = new PrintWriter(new FileWriter(noEntitiesFile));
		for (String string : noEntities) {
			out.write(string + "\n");
		}
		out.close();
	}

	private void writeNewCoordinations() {
		StringBuffer sb = new StringBuffer();
		for (Map.Entry<String, String> e : possCompoundSplits.entrySet()) {
			sb.append(e.getKey() + "|" + e.getValue() + "\n");
		}

		try {

			if (!possCompoundsFile.exists()) {
				possCompoundsFile.getParentFile().mkdirs();
				possCompoundsFile.createNewFile();
			}
			FileWriter fw = new FileWriter(possCompoundsFile);
			fw.write(sb.toString());
			fw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void writeStatistics(Map<String, Integer> counts, File statisticsFile) throws IOException {
		Map<Integer, Set<String>> statistics = new TreeMap<Integer, Set<String>>();
		for (String exp : counts.keySet()) {
			int count = counts.get(exp);
			Set<String> expressionsForCount = statistics.get(count);
			if (expressionsForCount == null) {
				expressionsForCount = new HashSet<String>();
			}
			expressionsForCount.add(exp);
			statistics.put(count, expressionsForCount);
		}
		PrintWriter out = new PrintWriter(new FileWriter(statisticsFile));
		out.write("number of types found in database: " + counts.keySet().size() + "\n\n");
		List<Integer> countsList = new ArrayList<>(statistics.keySet());
		for (int i = statistics.size() - 1; i >= 0; i--) {
			int c = countsList.get(i);
			for (String exp : statistics.get(c)) {
				out.write(exp + ": " + c + "\n");
			}
			out.write("\n");
		}
		out.close();
	}

	private void updateMatchCount(Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> stringMatches,
			Map<String, Integer> counts) {
		for (ExtractionUnit iePhrase : stringMatches.keySet()) {

			for (InformationEntity e : stringMatches.get(iePhrase).keySet()) {
				String exp = e.toString();
				Integer c = counts.get(exp);
				if (c == null) {
					c = 0;
				}
				c++;
				counts.put(exp, c);
			}
		}
	}

}
