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

import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.db_io.Class_DBConnector;
import quenfo.de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.helpers.SingleToMultiClassConverter;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.Pattern;
import quenfo.de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.preprocessing.ExtractionUnitBuilder;
import is2.lemmatizer.Lemmatizer;
import is2.tag.Tagger;

/**
 * @author geduldia
 * 
 *         The main Extractor. Extracts Competences/Tools via Patterns and via
 *         StringMatching
 *
 */
public class Extractor {

	private IEJobs jobs;
	private IEType type;
	private Set<String> knownEntities = new HashSet<String>();
	private Set<String> noEntities = new HashSet<String>();
	private Map<String, String> possCompoundSplits = new HashMap<String, String>();
	private String possibleFileString = "information_extraction/data/compounds/possibleCompounds.txt";
	private boolean resolveCoordinations;
	private File entitiesFile;
	private File noEntitiesFile;
	private File contexts;
	private File modifier;

	// Konstruktor für die Matching-Workflows
	/**
	 * @param entities  file of already known/extracted entities
	 * @param modifiers files with modifier-terms
	 * @param type      type of information
	 * @throws IOException
	 */
	public Extractor(File entities, File modifiers, IEType type, boolean resolveCoordinations) throws IOException {
		this.resolveCoordinations = resolveCoordinations;
		this.entitiesFile = entities;
		this.type = type;
		this.jobs = new IEJobs(entities, null, modifiers, null, type, resolveCoordinations);
		initialize();
	}

	/**
	 * Constructor for competence-matching
	 * 
	 * @param outputConnection
	 * 
	 * @param competences      file of already known/extracted competences
	 * @param noCompetences    file of known typical mistakes
	 * @param contexts         file of context-patterns for comp.-extraction
	 * @param importanceTerms  file of importance-terms
	 * @param type             type of information (competences)
	 * @throws IOException // * @throws SQLException //
	 */
	// public Extractor(Connection outputConnection, File competences, File
	// noCompetences, File contexts,
	// File importanceTerms, IEType type) throws IOException, SQLException {
	// this.entitiesFile = competences;
	// this.noEntitiesFile = noCompetences;
	// this.contexts = contexts;
	// this.type = type;
	// this.jobs = new IEJobs(competences, noCompetences, importanceTerms,
	// contexts, type);
	// if (outputConnection != null) {
	// knownEntities = IE_DBConnector.readAnnotatedEntities(outputConnection, 1,
	// type);
	// noEntities = IE_DBConnector.readAnnotatedEntities(outputConnection, 0,
	// type);
	// jobs.addKnownEntities(knownEntities);
	// jobs.addNoEntities(noEntities);
	// }
	// initialize();
	// }

	/**
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
		this.modifier = modifier;
		this.contexts = contexts;
		this.type = type;
		this.jobs = new IEJobs(entitiesFile, noEntitiesFile, modifier, contexts, type, resolveCoordinations);// new
																												// IEJobs(entitiesFile,
																												// noEntitiesFile,
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
	public Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extract(int startPos, int maxCount, int tablesize,
			Connection inputConnection, Connection outputConnection, boolean gold) throws IOException, SQLException {

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
		is2.tools.Tool lemmatizer = new Lemmatizer(
				"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model");
		is2.tools.Tool tagger = new Tagger(
				"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/tag-ger-3.6.model");

		List<ClassifyUnit> classifyUnits = null;
		List<ExtractionUnit> extractionUnits = null;
		Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extractions = null;
		Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> allExtractions = new HashMap<ExtractionUnit, Map<InformationEntity, List<Pattern>>>();

		int paragraphsPerRound = 10000;
		int readParagraphs = 0;
		int offset = startPos;
		if (maxCount > -1 && paragraphsPerRound > maxCount) {
			paragraphsPerRound = maxCount;
		}

		long before;
		long after;
		double time;
		boolean finished = false;

		// Aus Speichergründen werden jeweils nur 10000 Paragraphen eingelesen,
		// zu ExtractionUnits verarbeitet und zur Extraktion weitergegeben
		// Die Extraktionen aus jeder Runde werden in der Map AllExtractions
		// gesammelt
		while (!finished) {
//			System.out.println(jobs.getNewCompoundsCount() + " neue Komposita");
			before = System.currentTimeMillis();

			// Einlesen der Paragraphen
			if (maxCount > -1 && readParagraphs + paragraphsPerRound > maxCount) {
				paragraphsPerRound = maxCount - readParagraphs;
			}
			System.out.println("\nread ClassifyUnits from DB " + offset + " - " + (offset + paragraphsPerRound));
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
			System.out.println("\ninitialize ExtractionUnits");
			extractionUnits = ExtractionUnitBuilder.initializeIEUnits(classifyUnits, lemmatizer, null, tagger);
			System.out.println("--> " + extractionUnits.size());

			// falls bei der Initialisierung noch lexikaische Daten generiert
			// werden mussten, werden diese fürs nächste Mal in der Input-DB
			// gespeichert.
			IE_DBConnector.upateClassifyUnits(inputConnection, extractionUnits);

			// Informationsextraktion
			jobs.annotateTokens(extractionUnits);
			System.out.println("\nextract " + type.name().toLowerCase() + "s");
			extractions = jobs.extractEntities(extractionUnits, lemmatizer);

			possCompoundSplits.putAll(jobs.getNewCompounds());

			// Entfernen der bereits bekannten Entitäten
			extractions = removeKnownEntities(extractions);

			allExtractions.putAll(extractions);

			after = System.currentTimeMillis();
			time = (((double) after - before) / 1000);
			System.out.println("time: " + time);
			classifyUnits = null;
			extractions = null;
			extractionUnits = null;
			this.jobs = new IEJobs(entitiesFile, noEntitiesFile, modifier, contexts, type, resolveCoordinations);
			jobs.addKnownEntities(knownEntities);
			jobs.addNoEntities(noEntities);
		}
		lemmatizer = null;
		tagger = null;

		jobs.mergeInformationEntities(allExtractions);

		if (allExtractions.isEmpty()) {
			System.out.println("\n no new " + jobs.type.name() + "s found\n");
		} else {
			// Speichern der Extraktionsergebnisse in der Output-DB
			System.out.println("\nwrite extracted " + type.name().toLowerCase() + "s in output-DB ");
			if (gold)
				IE_DBConnector.createExtractionGoldOutputTable(outputConnection, type, true);
			else
				IE_DBConnector.createExtractionOutputTable(outputConnection, type, true);
			if (type == IEType.COMPETENCE) {
				IE_DBConnector.writeCompetenceExtractions(allExtractions, outputConnection, true, gold);
			}
			if (type == IEType.TOOL) {
				IE_DBConnector.writeToolExtractions(allExtractions, outputConnection, true, gold);
			}
		}
		// schreibt die txt-Files (competences.txt & noCompetences.txt) neu, da
		// zu Beginn evtl. neue manuell annotierte eingelesen wurden
		// (eigentlich nicht mehr notwendig, da im BIBB nicht mehr manuell
		// annotiert wird)
		reWriteFiles();
		System.out.print("\nwrite new compounds to evaluate");
		writeNewCoordinations();
		return allExtractions;
	}

	public void stringMatch(File statisticsFile, Connection inputConnection, Connection outputConnection, int maxCount,
			int startPos) throws SQLException, IOException {

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
		is2.tools.Tool lemmatizer = new Lemmatizer(
				"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model");

		List<ClassifyUnit> classifyUnits;
		List<ExtractionUnit> extractionUnits;
		Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> stringMatches;

		// Aus Speichergründen werden jeweils nur 50000 Paragraphen eingelesen
		// und verarbeitet
		int paragraphsPerRound = 50000;
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
			System.out.println("\nread ClassifyUnit " + offset + " - " + (offset + paragraphsPerRound));
			classifyUnits = IE_DBConnector.readClassifyUnits(paragraphsPerRound, offset, inputConnection, jobs.type);
			if (classifyUnits.isEmpty()) {
				break;
			}
			readParagraphs += classifyUnits.size();
			offset = offset + classifyUnits.size();

			// Paragraphen in Sätze splitten und in ExtractionUnits überführen
			System.out.println("\ninitialize ExtractionUnits");
			extractionUnits = ExtractionUnitBuilder.initializeIEUnits(classifyUnits, lemmatizer, null, null);
			System.out.println("--> " + extractionUnits.size());

			// Matching
			System.out.println("\nmatch");
			jobs.annotateTokens(extractionUnits);
			stringMatches = jobs.extractByStringMatch(extractionUnits, lemmatizer);
			stringMatches = jobs.mergeInformationEntities(stringMatches);

			// set Modifiers
			if (jobs.type == IEType.COMPETENCE) {
				jobs.setModifiers(stringMatches);
			}

			// write results in DB
			System.out.println("\nwrite results in output-DB");
			if (jobs.type == IEType.COMPETENCE) {
				IE_DBConnector.writeCompetenceExtractions(stringMatches, outputConnection, false, false);
			}
			if (jobs.type == IEType.TOOL) {
				IE_DBConnector.writeToolExtractions(stringMatches, outputConnection, false, false);
			}

			updateMatchCount(stringMatches, matchCounts);
			if (maxCount > -1 && readParagraphs >= maxCount)
				break;
		}
		// write statisticsFile
		System.out.println("\nwrite Statistics-File");
		writeStatistics(matchCounts, statisticsFile);
		System.out.print("\nwrite new compounds to evaluate");
		writeNewCoordinations();
		lemmatizer = null;
	}

	// private void cluster(Map<ExtractionUnit, Map<InformationEntity,
	// List<Context>>> extractionUnits, Connection outputConnection) throws
	// Exception {
	// // sort contexts by competence
	// Map<String, List<String>> lemmatasByCompetence = new HashMap<String,
	// List<String>>();
	// for (ExtractionUnit eu : extractionUnits.keySet()) {
	// for (InformationEntity ie : extractionUnits.get(eu).keySet()) {
	// String comp = ie.getFullExpression();
	// List<String> lemmata = lemmatasByCompetence.get(comp);
	// if (lemmata == null)
	// lemmata = new ArrayList<String>();
	// lemmata.addAll(Arrays.asList(eu.getLemmata()));
	// lemmatasByCompetence.put(comp, lemmata);
	// }
	// }
	//
	// // get cooccurrences
	// AbstractFeatureQuantifier<String> qf = new
	// CooccurrenceFeatureQuantifier<String>();
	// Map<String, double[]> vectors =
	// qf.getFeatureVectors(lemmatasByCompetence, null);
	//
	// //create Clusterer
	// SimpleKMeans kmeans = new SimpleKMeans(); // new instance of clusterer
	// kmeans.setNumClusters(17);
	// kmeans.setPreserveInstancesOrder(true);
	// kmeans.setInitializationMethod(new SelectedTag(SimpleKMeans.RANDOM,
	// SimpleKMeans.TAGS_SELECTION));
	// kmeans.setDistanceFunction(new ManhattanDistance());
	// kmeans.setMaxIterations(100);
	// WekaClusterer clusterer = new WekaClusterer(kmeans, vectors);
	// clusterer.cluster();
	//
	// Connection c =
	// IE_DBConnector.connect("C:/sqlite/ClusteredCompetences.db");
	// IE_DBConnector.createCluserTable(c);
	// int i = 0;
	// for (String comp : vectors.keySet()) {
	// int clusterID = clusterer.getClusterForInstance(i);
	// IE_DBConnector.writeClusterResult(clusterID, comp, c);
	// i++;
	// }
	//
	// }

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

	// private void readAnnotatedEntitiesFromFile() throws IOException {
	//
	// if (entitiesFile.exists()) {
	// BufferedReader in = new BufferedReader(new FileReader(entitiesFile));
	// String line = in.readLine();
	// while (line != null) {
	// knownEntities.add(line);
	// line = in.readLine();
	// }
	// in.close();
	// }
	//
	// if (noEntitiesFile.exists()) {
	// BufferedReader in = new BufferedReader(new FileReader(noEntitiesFile));
	// String line = in.readLine();
	// while (line != null) {
	// noEntities.add(line);
	// line = in.readLine();
	// }
	// in.close();
	// }
	// }

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

	// private void writeOutputFiles(Map<ExtractionUnit, Map<InformationEntity,
	// List<Context>>> allExtractions,
	// File potentialComps, File potentialCompsWithContext) throws IOException {
	// Set<String> extracted = new HashSet<String>();
	// PrintWriter out;
	// // write entities with contexts
	// if (potentialCompsWithContext != null) {
	// out = new PrintWriter(new FileWriter(potentialCompsWithContext));
	// for (ExtractionUnit iePhrase : allExtractions.keySet()) {
	// out.write("\n" + iePhrase.getSentence() + "\n");
	// for (int t = 1; t < iePhrase.getTokenObjects().size() - 1; t++) {
	// Token token = iePhrase.getTokenObjects().get(t);
	// out.write(token.getLemma() + " " + " [" + token.getPosTag() + "] ");
	// }
	// out.write("\n");
	// Map<InformationEntity, List<Context>> iesWithContext =
	// allExtractions.get(iePhrase);
	// for (InformationEntity ie : iesWithContext.keySet()) {
	// out.write("\n--> " + ie.toString());
	// boolean knowledge = false;
	// List<Context> contexts = iesWithContext.get(ie);
	// for (Context context : contexts) {
	// if (context.getDescription().contains("KNOWLEDGE")) {
	// knowledge = true;
	// break;
	// }
	// }
	// if (knowledge) {
	// extracted.add("[KNOWLEDGE] " + ie.toString());
	// } else {
	// extracted.add(ie.toString());
	// }
	// }
	// out.write("\n");
	// }
	// out.close();
	// }
	// // write entities
	// if (potentialComps != null) {
	// List<String> sorted = new ArrayList<String>(extracted);
	// Collections.sort(sorted);
	// out = new PrintWriter(new FileWriter(potentialComps));
	// for (String string : sorted) {
	// out.write(string + "\n");
	// }
	// out.close();
	// }
	// // rewrite/ reorder knowledge lists
	// // jobs.writeEntitieLists(entitiesFile, noEntitiesFile);
	// }

	private void writeNewCoordinations() {
		System.out.print(": " + possCompoundSplits.size());
		StringBuffer sb = new StringBuffer();
		for (Map.Entry<String, String> e : possCompoundSplits.entrySet()) {
			sb.append(e.getKey() + "|" + e.getValue() + "\n");
		}

		try {
			File f = new File(possibleFileString);
			if (!f.exists()) {
				f.getParentFile().mkdirs();
				f.createNewFile();
			}
			FileWriter fw = new FileWriter(f);
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

	// private Map<ExtractionUnit, Map<InformationEntity, List<Context>>>
	// removeKnowledgeStringMatches(
	// Map<ExtractionUnit, Map<InformationEntity, List<Context>>> stringMatches)
	// {
	// Map<ExtractionUnit, Map<InformationEntity, List<Context>>> toReturn =
	// stringMatches;
	// for (ExtractionUnit eu : stringMatches.keySet()) {
	// Map<InformationEntity, List<Context>> map = new
	// HashMap<InformationEntity, List<Context>>();
	// for (InformationEntity ie : stringMatches.get(eu).keySet()) {
	// if (!ie.isKnowledge()) {
	// map.put(ie, stringMatches.get(eu).get(ie));
	// }
	// }
	// toReturn.put(eu, map);
	// }
	// return stringMatches;
	// }

}
