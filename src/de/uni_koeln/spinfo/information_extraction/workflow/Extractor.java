package de.uni_koeln.spinfo.information_extraction.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.net.SyslogAppender;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
import de.uni_koeln.spinfo.grouping.SimilarityCalculator;
import de.uni_koeln.spinfo.grouping.featureEngineering.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.grouping.featureEngineering.CooccurrenceFeatureQuantifier;
import de.uni_koeln.spinfo.grouping.weka.WekaClusterer;
import de.uni_koeln.spinfo.information_extraction.data.Context;
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.data.IEType;
import de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import de.uni_koeln.spinfo.information_extraction.preprocessing.ExtractionUnitBuilder;
import weka.clusterers.SimpleKMeans;
import weka.core.ManhattanDistance;
import weka.core.SelectedTag;

/**
 * @author geduldia
 * 
 *         The main Extractor. Has methods to extract Information and to store
 *         them in a new Database
 *
 */
public class Extractor {

	// private File entitiesFile;
	// private File noEntitiesFile;
	private IEJobs jobs;
	private IEType type;
	private Set<String> knownEntities;
	private Set<String> noEntities;
	private File entitiesFile;
	private File noEntitiesFile;

	/**
	 * Constructor for competence-extraction
	 * 
	 * @param competences
	 *            file of already known/extracted competences
	 * @param noCompetences
	 *            file of known typical mistakes
	 * @param contexts
	 *            file of context-patterns for comp.-extraction
	 * @param knowledgeTerms
	 *            file of knowledge-terms
	 * @param importanceTerms
	 *            file of importance-terms
	 * @param type
	 *            type of information (usually competences)
	 * @throws IOException
	 * @throws SQLException
	 */
	public Extractor(Connection outputConnection, File competences, File noCompetences, File contexts,
			File importanceTerms, IEType type) throws IOException, SQLException {

		this.entitiesFile = competences;
		this.noEntitiesFile = noCompetences;
		this.type = type;
		this.jobs = new IEJobs(competences, noCompetences, importanceTerms, contexts, type);
		if (outputConnection != null) {
			knownEntities = IE_DBConnector.readAnnotatedEntities(outputConnection, 1, type);
			noEntities = IE_DBConnector.readAnnotatedEntities(outputConnection, 0, type);
			jobs.addKnownEntities(knownEntities);
			jobs.addNoEntities(noEntities);
		}

		initialize();
	}

	/**
	 * Constructor for tool-extraction
	 * 
	 * @param tools
	 *            file of already known/extracted tools
	 * @param noTools
	 *            file of known typical mistakes
	 * @param contexts
	 *            file of context-patterns for comp.-extraction
	 * @param type
	 *            type of information (usually tools)
	 * @throws IOException
	 * @throws SQLException
	 */
	public Extractor(Connection outputConnection, File tools, File noTools, File contexts, IEType type)
			throws IOException, SQLException {
		this.entitiesFile = tools;
		this.noEntitiesFile = noTools;
		this.type = type;
		this.jobs = new IEJobs(tools, noTools, contexts, type);
		if (outputConnection != null) {
			knownEntities = IE_DBConnector.readAnnotatedEntities(outputConnection, 1, type);
			noEntities = IE_DBConnector.readAnnotatedEntities(outputConnection, 0, type);
			jobs.addKnownEntities(knownEntities);
			jobs.addNoEntities(noEntities);
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
	 * Extracts Information (e.g. competences) from the input-database of
	 * ClassifyUnits and stores them in a file
	 * 
	 * @param startPos
	 *            db startPosition of classifyUnits to extract from
	 * @param count
	 *            max number of classifyUnits to extract from
	 * @param tablesize
	 *            tablesize of the input-db
	 * @param inputConnection
	 *            db-connection
	 * @param potentialComps
	 *            file for extracted informationEntities
	 * @param potentialCompsWithContext
	 *            file for the extracted informationEntities with their
	 *            containing sentences
	 * @throws IOException
	 * @throws SQLException
	 */
	public Map<ExtractionUnit, Map<InformationEntity, List<Context>>> extract(int startPos, int count, int tablesize,
			Connection inputConnection, Connection outputConnection, double similarityScore) throws IOException, SQLException {
		Map<ExtractionUnit, Map<InformationEntity, List<Context>>> allExtractions = new HashMap<ExtractionUnit, Map<InformationEntity, List<Context>>>();
		boolean finished = false;
		int readClassifyUnits = 0;
		int start = startPos;
		while (!finished) {
			// get Paragraphs from ClassesCorrectable-DB
			int maxCount = 100000;
			if (readClassifyUnits + maxCount > count) {
				maxCount = count - readClassifyUnits;
			}

			List<ClassifyUnit> classifyUnits = IE_DBConnector.getClassifyUnitsUnitsFromDB(maxCount, start,
					inputConnection, jobs.type);

			System.out.println("\nselected " + classifyUnits.size() + " classifyUnits from DB for " + jobs.type.name()
					+ "-detection\n");

			readClassifyUnits = readClassifyUnits + maxCount;
			if (readClassifyUnits >= count) {
				finished = true;
			}
			if (startPos + readClassifyUnits >= tablesize) {
				finished = true;
			}
			start = start + maxCount;

			List<ExtractionUnit> extractionUnits = ExtractionUnitBuilder.initializeIEUnits(classifyUnits);
			jobs.annotateTokens(extractionUnits);
			Map<ExtractionUnit, Map<InformationEntity, List<Context>>> extractions = jobs
					.extractByPatterns(extractionUnits);
			allExtractions.putAll(extractions);
		}
		// remove already known entities
		jobs.mergeInformationEntities(allExtractions);
		allExtractions = removeKnownEntities(allExtractions);
		if (allExtractions.isEmpty()) {
			System.out.println("\n no new " + jobs.type.name() + "s found\n");
		} else {

			// group Competences
			Map<String, Integer> groupIds = groupCompetences(allExtractions, similarityScore);
			// System.out.println(groupIds);

			// write results in DB
			IE_DBConnector.createOutputTable(outputConnection, type, true);
			if (type == IEType.COMPETENCE) {
				IE_DBConnector.writeCompetences(allExtractions, outputConnection, true, groupIds);
			}
			if (type == IEType.TOOL) {
				IE_DBConnector.writeTools(allExtractions, outputConnection, true, groupIds);
			}
			System.out.println("\n finished " + jobs.type.name() + "s extraction\n");
			System.out.println(
					"\n see and edit results in database '" + outputConnection.getMetaData().getURL().substring(12));
		}
		writeOutputFile();
		return allExtractions;
	}

	private Map<String, Integer> groupCompetences(
			Map<ExtractionUnit, Map<InformationEntity, List<Context>>> extractions, double similarityScore) {
		SimilarityCalculator sc = new SimilarityCalculator();
		Set<String> expressions = new HashSet<String>();
		for (ExtractionUnit eUnit : extractions.keySet()) {
			for (InformationEntity ie : extractions.get(eUnit).keySet()) {
				expressions.add(ie.getFullExpression());
			}
		}
		List<List<String>> groups = new ArrayList<List<String>>();
		for (String string : expressions) {
			List<Integer> matches = new ArrayList<Integer>();
			for (int i = 0; i < groups.size(); i++) {
				List<String> currentGroup = groups.get(i);
				for (String groupString : currentGroup) {
					int sim = sc.smithWatermanSimilarity(string, groupString);
					if (sim >= similarityScore * (string.length() + groupString.length())) {
						matches.add(i);
						break;
					}
				}
			}
			if (matches.isEmpty()) {
				List<String> newList = new ArrayList<>();
				newList.add(string);
				groups.add(newList);
			} else if (matches.size() == 1) {
				List<String> list = groups.get(matches.get(0));
				list.add(string);
			} else {
				List<String> first = groups.get(matches.get(0));
				first.add(string);
				for (int j = matches.size()-1; j >= 1; j--) {
					int index = matches.get(j);
					first.addAll(groups.get(index));
					groups.remove(index);
				}
				
			}
		}
		
		Map<String, Integer> groupIds = new HashMap<String, Integer>();
		groups.sort(new Comparator<List<String>>() {

			@Override
			public int compare(List<String> o1, List<String> o2) {
				Integer s1 = o1.size();
				Integer s2 = o2.size();
				return s1.compareTo(s2);
			}
		});
		
		int id = 1;
		for (List<String> group : groups) {
			if(group.size() > 1){
				for (String string : group) {
					groupIds.put(string, id);
				}
				id++;
			}
			else{
				groupIds.put(group.get(0), 0);
			}
		}
		return groupIds;
	}

	/**
	 * creates a database of all extracted Information Entities
	 * 
	 * @param statisticsFile
	 *            file for statistics (e.g. how many times an InformationEntity
	 *            was found in the db)
	 * @param inputConnection
	 * @param outputConnection
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws Exception 
	 */
	public void finalStringMatch(File statisticsFile, Connection inputConnection, Connection outputConnection) throws SQLException, IOException
			 {
		System.out.println("\n match " + jobs.type.name() + "s with database. \n");

		// (re)create output tables
		IE_DBConnector.createOutputTable(outputConnection, jobs.type, false);

		Map<String, Integer> counts = new HashMap<String, Integer>();

		int offset = 0;

		while (true) {
			// read and prepare paragraphs for matching
			List<ClassifyUnit> allUnits = IE_DBConnector.getClassifyUnitsUnitsFromDB(100000, offset, inputConnection,
					jobs.type);
			if (allUnits.isEmpty()) {
				break;
			}
			System.out.println("\nmatch ClassifyUnits " + offset + " - " + (offset + allUnits.size()) + "\n");
			offset = offset + allUnits.size();

			// init. ExtractionUnits
			List<ExtractionUnit> extractionUnits = ExtractionUnitBuilder.initializeIEUnits(allUnits, true, false,
					false);
			jobs.annotateTokens(extractionUnits);
			// contextMatches bleibt leer, da nur ein Stringmatch ausgeführt
			// werden soll
			Map<ExtractionUnit, Map<InformationEntity, List<Context>>> contextMatches = new HashMap<ExtractionUnit, Map<InformationEntity, List<Context>>>();// jobs.extractByPatterns(extractionUnits);
			Map<ExtractionUnit, Map<InformationEntity, List<Context>>> stringMatches = jobs
					.extractByStringMatch(extractionUnits, contextMatches);

			stringMatches = jobs.mergeInformationEntities(stringMatches);

			// setImportances
			if (jobs.type == IEType.COMPETENCE) {
				jobs.setImportances(stringMatches);
				// jobs.setImportances(contextMatches);
			}


			// write in DB
			if (jobs.type == IEType.COMPETENCE) {
				// IE_DBConnector.writeCompetences(contextMatches,
				// outputConnection, false);
				IE_DBConnector.writeCompetences(stringMatches, outputConnection, false, null);
			}
			if (jobs.type == IEType.TOOL) {
				// IE_DBConnector.writeTools(contextMatches, outputConnection,
				// false);
				IE_DBConnector.writeTools(stringMatches, outputConnection, false, null);
			}
			count(statisticsFile, stringMatches, counts);
			// count(statisticsFile, contextMatches, counts);
		}
		// write statisticsFile
		writeStatistics(counts, statisticsFile);

		System.out.println("\n finished DB-update! \n");
	}

//	private void cluster(Map<ExtractionUnit, Map<InformationEntity, List<Context>>> extractionUnits, Connection outputConnection) throws Exception {
//		// sort contexts by competence
//		Map<String, List<String>> lemmatasByCompetence = new HashMap<String, List<String>>();
//		for (ExtractionUnit eu : extractionUnits.keySet()) {
//			for (InformationEntity ie : extractionUnits.get(eu).keySet()) {
//				String comp = ie.getFullExpression();
//				List<String> lemmata = lemmatasByCompetence.get(comp);
//				if (lemmata == null)
//					lemmata = new ArrayList<String>();
//				lemmata.addAll(Arrays.asList(eu.getLemmata()));
//				lemmatasByCompetence.put(comp, lemmata);
//			}
//		}
//
//		// get cooccurrences
//		AbstractFeatureQuantifier<String> qf = new CooccurrenceFeatureQuantifier<String>();
//		Map<String, double[]> vectors = qf.getFeatureVectors(lemmatasByCompetence, null);
//		
//		//create Clusterer
//		SimpleKMeans kmeans = new SimpleKMeans(); // new instance of clusterer
//		kmeans.setNumClusters(17);
//		kmeans.setPreserveInstancesOrder(true);
//		kmeans.setInitializationMethod(new SelectedTag(SimpleKMeans.RANDOM, SimpleKMeans.TAGS_SELECTION));
//		kmeans.setDistanceFunction(new ManhattanDistance());
//		kmeans.setMaxIterations(100);
//		WekaClusterer clusterer = new WekaClusterer(kmeans, vectors);
//		clusterer.cluster();
//		
//		Connection c = IE_DBConnector.connect("C:/sqlite/ClusteredCompetences.db");
//		IE_DBConnector.createCluserTable(c);
//		int i = 0;
//		for (String comp : vectors.keySet()) {
//			int clusterID = clusterer.getClusterForInstance(i);
//			IE_DBConnector.writeClusterResult(clusterID, comp, c);
//			i++;
//		}
//
//	}

	private Map<ExtractionUnit, Map<InformationEntity, List<Context>>> removeKnownEntities(
			Map<ExtractionUnit, Map<InformationEntity, List<Context>>> allExtractions) {
		Map<ExtractionUnit, Map<InformationEntity, List<Context>>> toReturn = new HashMap<ExtractionUnit, Map<InformationEntity, List<Context>>>();
		for (ExtractionUnit extractionUnit : allExtractions.keySet()) {

			Map<InformationEntity, List<Context>> ies = allExtractions.get(extractionUnit);
			Map<InformationEntity, List<Context>> filterdIes = new HashMap<InformationEntity, List<Context>>();
			for (InformationEntity ie : ies.keySet()) {
				Set<InformationEntity> knownIEs = jobs.entities.get(ie.getToken());
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

	private void writeOutputFile() throws IOException {

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

	private void count(File statisticsFile, Map<ExtractionUnit, Map<InformationEntity, List<Context>>> stringMatches,
			Map<String, Integer> counts) {
		for (ExtractionUnit iePhrase : stringMatches.keySet()) {
			List<InformationEntity> ies = new ArrayList<InformationEntity>(stringMatches.get(iePhrase).keySet());

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
