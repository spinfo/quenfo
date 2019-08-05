package quenfo.de.uni_koeln.spinfo.categorization.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import quenfo.de.uni_koeln.spinfo.categorization.data.Entity;
import quenfo.de.uni_koeln.spinfo.categorization.data.Pair;
import quenfo.de.uni_koeln.spinfo.categorization.data.Sentence;
import quenfo.de.uni_koeln.spinfo.categorization.db_io.Cat_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;

public class Cat_Jobs {

	public static Map<Integer, List<Entity>> buildMergedGroups(Map<Integer, List<Entity>> cooccGroups,
			Map<Integer, List<Entity>> simGroups) {

		Map<Entity, Integer> reversedCooccGroups = reverseMap2(cooccGroups);
		Map<Entity, Integer> reversedSimGroups = reverseMap2(simGroups);
		Set<Entity> toGroup = new HashSet<Entity>();
		toGroup.addAll(reversedSimGroups.keySet());
		toGroup.addAll(reversedCooccGroups.keySet());
		System.out.println("toGroup: " + toGroup.size());
		List<List<Entity>> groups = new ArrayList<List<Entity>>();
		for (Entity entity : toGroup) {
			List<Integer> matches = new ArrayList<Integer>();
			Integer simID = reversedSimGroups.get(entity);
			Integer cooccID = reversedCooccGroups.get(entity);
			if (simID == null)
				simID = -1;
			if (cooccID == null)
				cooccID = -1;
			for (int i = 0; i < groups.size(); i++) {
				List<Entity> group = groups.get(i);
				for (Entity current : group) {
					Integer currentSimID = reversedSimGroups.get(current);
					Integer currentCooccID = reversedCooccGroups.get(current);
					if (currentSimID == null)
						currentSimID = -1;
					if (currentCooccID == null)
						currentCooccID = -1;
					if (!(simID.equals(-1)) && simID.equals(currentSimID)) {
						matches.add(i);
						break;
					}
					if (!(cooccID.equals(-1)) && cooccID.equals(currentCooccID)) {
						matches.add(i);
						break;
					}
				}
			}
			if (matches.isEmpty()) {
				// neue Gruppe
				List<Entity> newGroup = new ArrayList<Entity>();
				newGroup.add(entity);
				groups.add(newGroup);
			} else if (matches.size() == 1) {
				// Entity der Gruppe hinzufügen
				List<Entity> group = groups.get(matches.get(0));
				group.add(entity);
			} else {
				// Gruppen verbinden
				List<Entity> first = groups.get(matches.get(0));
				first.add(entity);
				for (int j = matches.size() - 1; j >= 1; j--) {
					int index = matches.get(j);
					first.addAll(groups.get(index));
					groups.remove(index);
				}
			}
		}
		Map<Integer, List<Entity>> groupsByID = new HashMap<Integer, List<Entity>>();
		// gruppen nach Mitgliederzahl sortieren
		groups.sort(new Comparator<List<Entity>>() {
			@Override
			public int compare(List<Entity> set1, List<Entity> set2) {
				Integer s1 = set1.size();
				Integer s2 = set2.size();
				return s1.compareTo(s2);
			}
		});
		// Vergabge der Ids
		int id = 1;
		for (List<Entity> group : groups) {
			if (group.size() > 1) {
				groupsByID.put(id, group);
				id++;
			} else {
				List<Entity> noGroupComps = groupsByID.get(0);
				if (noGroupComps == null)
					noGroupComps = new ArrayList<Entity>();
				noGroupComps.addAll(group);
				groupsByID.put(0, noGroupComps);
			}
		}
		return groupsByID;
	}

	/**
	 * Liest die bereits validierten Tools/Kompetenzen ein, und filtert die
	 * bereits kategorisierten wieder heraus
	 * 
	 * Reads the already validated (but not categorized) competences/tools from
	 * File
	 * 
	 * @param file
	 *            txt-File with the validated competenes/tools
	 * @param exclude
	 *            set of all already categoriezd competences/tools
	 * @return set of all validated competemnces/tools excluding the categorized
	 *         comps/tools
	 * @throws IOException
	 */
	public static Set<Entity> readEntitiesFile(File file, Set<Entity> exclude) throws IOException {
		Set<Entity> toReturn = new HashSet<Entity>();
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = in.readLine();
		while (line != null) {
			if (!line.equals("")) {
				Entity e = new Entity(line.trim());
				e.setValidated(true);
				if (exclude != null) {
					if (exclude.contains(e)) {
						line = in.readLine();
						continue;
					}
				}
				toReturn.add(e);
			}
			line = in.readLine();
		}
		in.close();
		return toReturn;
	}

	/**
	 * Builds pairs of similar entities and writes them in an output-DB (Two
	 * Entities are similar if their lemmata s1 & s2 have a
	 * Needleman-Wunsch-Similarity of at least 'miSim' * (s1.length +
	 * s2.length))
	 * 
	 * returns a set of all entities which are similar to at least one other
	 * entity
	 * 
	 * 
	 * @param entities
	 *            list of entities
	 * @param simCalculator
	 * @param minSim
	 * @param connection
	 * @param type
	 * @return set of entities which are similar to at least one other entity
	 * @throws SQLException
	 */
	public static Map<Double, List<Entity>> getSimilarityPairs(List<Entity> entities, SimilarityCalculator simCalculator,
			double minSim, Connection connection, IEType type) throws SQLException {
		Map<Entity, Double> scoresByEntity = new HashMap<Entity,Double>();
//		int numberOfPairs = 0;
		Map<Pair, Double> pairs = new HashMap<Pair, Double>();
		for (int i = 0; i < entities.size(); i++) {
			Entity e1 = entities.get(i);
			for (int j = i + 1; j < entities.size(); j++) {
				Entity e2 = entities.get(j);
				if (e1.equals(e2)) {
					continue;
				}
				double nw = simCalculator.needlemanWunschSimilarity(e1.getLemma(), e2.getLemma());
				if (nw <= minSim * (e1.getLemma().length() + e2.getLemma().length())) {
					continue;
				}
			
				Pair pair = new Pair(e1, e2);
				pair.setScore(nw);
				pairs.put(pair, nw);
				Double d = scoresByEntity.get(e1);
				if(d == null) d = 0.0;
				if(d < pair.getScore()){
					scoresByEntity.put(e1, pair.getScore());
				}
				d = scoresByEntity.get(e2);
				if(d == null) d = 0.0;
				if(d < pair.getScore()){
					scoresByEntity.put(e2, pair.getScore());
				}
				int pairsPerRound = 100000;
				if (pairs.size() >= pairsPerRound) {
					System.out.println("write "+ pairs.size()+" pairs");
					Cat_DBConnector.writePairs(connection, pairs.keySet(), type, "Pairs");
//					numberOfPairs += pairs.size();
					pairs.clear();
				}
			}
		}
		System.out.println("write " +  pairs.size()+" Pairs");
		Cat_DBConnector.writePairs(connection, pairs.keySet(), type, "Pairs");
		Map<Double, List<Entity>> entitiesByScore = new HashMap<Double,List<Entity>>();
		for (Entity e : scoresByEntity.keySet()) {
			Double d = scoresByEntity.get(e);
			List<Entity> list = entitiesByScore.get(d);
			if(list == null) list = new ArrayList<Entity>();
			list.add(e);
			entitiesByScore.put(d, list);
		}
		return entitiesByScore;
	}

	/**
	 * reverses the keys and values of the given map
	 * 
	 * @param toReverse
	 * @return reversed map
	 */
	public static Map<Sentence, List<Entity>> reverseMap(Map<Entity, Set<Sentence>> toReverse) {
		Map<Sentence, List<Entity>> toReturn = new HashMap<Sentence, List<Entity>>();
		for (Entity e : toReverse.keySet()) {
			Set<Sentence> sentences = toReverse.get(e);
			for (Sentence sentence : sentences) {
				List<Entity> entities = toReturn.get(sentence);
				if (entities == null)
					entities = new ArrayList<Entity>();
				entities.add(e);
				toReturn.put(sentence, entities);
			}
		}
		return toReturn;
	}

	/**
	 * groups a set of entities according to their string-similarity OR
	 * cooccurrences
	 * 
	 * @param toGroup
	 * @param minSimilarity
	 *            min similarity for group-membership
	 * @param simCalculator
	 * @param minChiSquare
	 *            min chiSquarefor group-membership
	 * @param cooccurrencePairs
	 *            map of cooccurring pairs (as keys) and their chiSquare-score
	 *            (as values)
	 * @return Map of groupIDs (as keys) and competences/tools ( as values)
	 * @throws SQLException
	 */
	public static Map<Integer, List<Entity>> buildGroups(List<Entity> toGroup, double minSimilarity,
			SimilarityCalculator simCalculator, double minChiSquare, Map<Pair, Double> cooccurrencePairs)
					throws SQLException {
		List<List<Entity>> groups = new ArrayList<List<Entity>>();
		for (Entity current : toGroup) {
			List<Integer> matches = new ArrayList<Integer>();
			for (int i = 0; i < groups.size(); i++) {
				List<Entity> currentGroup = groups.get(i);
				for (Entity groupEntity : currentGroup) {
					Pair pair = new Pair(current, groupEntity);
					if (cooccurrencePairs != null) {
						Double coocc = cooccurrencePairs.get(pair);
						if (coocc != null && coocc >= minChiSquare) {
							matches.add(i);
							break;
						}
					}

					if (minSimilarity > 0.0) {
						double sim = /*
										 * SC_DBConnector.getPairScore(
										 * connection, pair, type);
										 */ simCalculator.needlemanWunschSimilarity(pair.getE1().getLemma(),
								pair.getE2().getLemma());
						if (sim >= minSimilarity * (current.getLemma().length() + groupEntity.getLemma().length())) {
							matches.add(i);
							break;
						}
					}

				}
			}
			if (matches.isEmpty()) {
				// neue Gruppe
				List<Entity> newList = new ArrayList<>();
				newList.add(current);
				groups.add(newList);
			} else if (matches.size() == 1) {
				// current der Gruppe hinzufügen
				List<Entity> list = groups.get(matches.get(0));
				list.add(current);
			} else {
				List<Entity> first = groups.get(matches.get(0));
				first.add(current);
				for (int j = matches.size() - 1; j >= 1; j--) {
					int index = matches.get(j);
					first.addAll(groups.get(index));
					groups.remove(index);
				}

			}
		}
		Map<Integer, List<Entity>> groupsByID = new HashMap<Integer, List<Entity>>();
		// gruppen nach Mitgliederzahl sortieren
		groups.sort(new Comparator<List<Entity>>() {
			@Override
			public int compare(List<Entity> list1, List<Entity> list2) {
				Integer s1 = list1.size();
				Integer s2 = list2.size();
				return s1.compareTo(s2);
			}
		});
		// Vergabge der Ids
		int id = 1;
		for (List<Entity> group : groups) {
			if (group.size() > 1) {
				groupsByID.put(id, group);
				id++;
			} else {
				List<Entity> noGroupComps = groupsByID.get(0);
				if (noGroupComps == null)
					noGroupComps = new ArrayList<Entity>();
				noGroupComps.addAll(group);
				groupsByID.put(0, noGroupComps);
			}
		}
		return groupsByID;
	}

	/**
	 * groups a set of entities according to their cooccurrences
	 * 
	 * @param minChiSquare
	 *            min chi-square-value or group-membership
	 * @param cooccurrencePairs
	 *            map of cooccurring pairs (as key) and their chi-squares (as
	 *            value)
	 * @return Map of groupIDs (as keys) and competences/tools ( as values)
	 * @throws SQLException
	 */
	public static Map<Integer, List<Entity>> buildCooccurrenceGroups(Map<Double,List<Entity>> entitiesByScore, double minChiSquare,
			Map<Pair, Double> cooccurrencePairs) throws SQLException {
		List<Entity> toGroup = new ArrayList<Entity>();
		List<Double> sortedScores = new ArrayList<Double>(entitiesByScore.keySet());
		Collections.sort(sortedScores);
		for(int i = sortedScores.size()-1; i >= 0; i--){
			double d = sortedScores.get(i);
			for (Entity e : entitiesByScore.get(d)) {
				toGroup.add(e);
			}
		}
		return buildGroups(toGroup, -1, null, minChiSquare, cooccurrencePairs);
	}

	/**
	 * groups a set of entities according to their string-similarity
	 * 
	 * @param toGroup
	 * @param minSimilarity
	 *            min similarity for group-membership
	 * @param sc
	 * @return Map of groupIDs (as keys) and competences/tools ( as values)
	 * @throws SQLException
	 */
	public static Map<Integer, List<Entity>> buildStringSimilarityGroups(Map<Double,List<Entity>> entitiesByScore, double minSimilarity,
			SimilarityCalculator sc) throws SQLException {
		List<Entity> toGroup = new ArrayList<Entity>();
		List<Double> sortedScores = new ArrayList<Double>(entitiesByScore.keySet());
		Collections.sort(sortedScores);
		for(int i = sortedScores.size()-1; i >= 0; i--){
			double d = sortedScores.get(i);
			for (Entity e : entitiesByScore.get(d)) {
				toGroup.add(e);
			}
		}
		return buildGroups(toGroup, minSimilarity, sc, -1, null);
	}


	/**
	 * Builds pairs of cooccurring entities and writes them in an output-DB.
	 * Returns a set of all entities which cooccurs with at least one other
	 * entity
	 * 
	 * 
	 * @param sentencesByEntity
	 *            map of entities (as keys) and their containing sentences as
	 *            values
	 * @param entitiesBySentence
	 *            map of sentences (as keys) and the containing entities as
	 *            values
	 * @param connection
	 * @param type
	 * @param tableName
	 * 			name of the pairs-output-table
	 * @return set of entities which are similar to at least one other entity
	 * @throws SQLException
	 */
	public static Map<Double,List<Entity>> getCooccurrencePairs(Map<Entity, Set<Sentence>> sentencesByEntity,
			Map<Sentence, List<Entity>> entitiesBySentence, Connection connection, IEType type, String tableName) throws SQLException {
		Map<Entity, Double> scoresByEntity = new HashMap<Entity, Double>();
		Map<Entity, Map<Entity, Integer>> cooccurenceMap = new HashMap<Entity, Map<Entity, Integer>>();

		Set<Sentence> sentencesWithCurrent;
		Map<Entity, Integer> cooccurrencCounts;
		Set<Integer> hashs = new HashSet<Integer>();
		Map<Pair, Double> pairs = new HashMap<Pair, Double>();

		for (Entity current : sentencesByEntity.keySet()) {
			cooccurrencCounts = new HashMap<Entity, Integer>();
			sentencesWithCurrent = sentencesByEntity.get(current);
			// Finde alle Kookkurrenzen zu current
			for (Sentence sentenceWithCurrent : sentencesWithCurrent) {
				List<Entity> cooccurringEntities = entitiesBySentence.get(sentenceWithCurrent);
				for (Entity cooccurringEntity : cooccurringEntities) {
					if (cooccurringEntity.equals(current))
						continue;
					Integer i = cooccurrencCounts.get(cooccurringEntity);
					if (i == null)
						i = 0;
					i++;
					cooccurrencCounts.put(cooccurringEntity, i);
				}
			}
			cooccurenceMap.put(current, cooccurrencCounts);

			for (Entity cooccurringEntity : cooccurrencCounts.keySet()) {
				Pair pair = new Pair(current, cooccurringEntity);

				double both = cooccurrencCounts.get(cooccurringEntity);
				double onlyCurrent = sentencesByEntity.get(current).size() - both;
				double onlyOther = sentencesByEntity.get(cooccurringEntity).size() - both;
				double noOne = entitiesBySentence.keySet().size() - both - onlyCurrent - onlyOther;
				double square = Math.pow((both * noOne) - (onlyCurrent * onlyOther), 2);
				double denominator = (both + onlyOther) * (both + onlyCurrent) * (onlyOther + noOne)
						* (onlyCurrent + noOne);
				double chiSquare = (square / denominator);
				pair.setScore(chiSquare);
				if (!hashs.contains(pair.hashCode())) {
					pairs.put(pair, chiSquare);
					hashs.add(pair.hashCode());
				}
				if (pairs.size() >= 50000) {
					System.out.println("write " + pairs.size() + " pairs in db");
					Cat_DBConnector.writePairs(connection, pairs.keySet(), type, tableName);

					pairs.clear();
				}
				Double d = scoresByEntity.get(current);
				if(d == null) d = 0.0;
				if(d < pair.getScore()){
					scoresByEntity.put(current, pair.getScore());
				}
				d = scoresByEntity.get(cooccurringEntity);
				if(d == null) d = 0.0;
				if(d < pair.getScore()){
					scoresByEntity.put(cooccurringEntity, pair.getScore());
				}
			}
		}
		System.out.println("write " + pairs.size() + " pairs in db");
		Cat_DBConnector.writePairs(connection, pairs.keySet(), type, tableName);
		Map<Double, List<Entity>> entitiesByScore = new HashMap<Double,List<Entity>>();
		for (Entity e : scoresByEntity.keySet()) {
			Double d = scoresByEntity.get(e);
			List<Entity> list = entitiesByScore.get(d);
			if(list == null) list = new ArrayList<Entity>();
			list.add(e);
			entitiesByScore.put(d, list);
		}
		return entitiesByScore;
	}
	
	public static Map<Double, List<Entity>> getTrimmedCooccurrencePairs(Map<Entity, Set<Sentence>> sentencesByEntity, Map<Sentence, List<Entity>> entitiesBySentence, Connection connection,
			IEType type, String tableName) throws SQLException {
		int sentenceNum = countSentences(sentencesByEntity);
		Map<Entity, Double> scoresByEntity = new HashMap<Entity,Double>();
		Set<Integer> hashs = new HashSet<Integer>();
		Map<Pair,Double> pairs = new HashMap<Pair,Double>();
		Map<Entity, Map<Entity, Integer>> cooccurrenceMap = new HashMap<Entity,Map<Entity,Integer>>();
		for (Entity entity : sentencesByEntity.keySet()) {
			Map<Entity,Integer> cooccCounts = new HashMap<Entity,Integer>();
			for (Sentence sentence : sentencesByEntity.get(entity)) {
				for (Entity other : entitiesBySentence.get(sentence)/*sentencesByEntity.keySet()*/) {
					if(entity.equals(other)) continue;
					String otherLemma = Pattern.quote(other.getLemma());
					String regex = "(^"+otherLemma+" | "+otherLemma+" | "+otherLemma+"$|^"+otherLemma+"$)";
					Pattern p = Pattern.compile(regex);
					Matcher m = p.matcher(sentence.getTrimmed());
					if(m.find()){
						Integer count = cooccCounts.get(other);
						if(count == null) count = 0;
						count++;
						cooccCounts.put(other, count);
					}
				}
			}
			cooccurrenceMap.put(entity, cooccCounts);
			
			for (Entity cooccEntity : cooccCounts.keySet()) {
				Pair pair = new Pair(entity, cooccEntity);
				double both = cooccCounts.get(cooccEntity) *2;
				double onlyCurrent = sentencesByEntity.get(entity).size() - (both/2);
				double onlyOther = sentencesByEntity.get(cooccEntity).size() - (both/2);
				double noOne = sentenceNum - both - onlyCurrent - onlyOther;
				double square = Math.pow((both * noOne) - (onlyCurrent * onlyOther), 2);
				double denominator = (both + onlyOther) * (both + onlyCurrent) * (onlyOther + noOne)
						* (onlyCurrent + noOne);
				double chiSquare = (square / denominator);
				pair.setScore(chiSquare);
				if (!hashs.contains(pair.hashCode())) {
					pairs.put(pair, chiSquare);
					hashs.add(pair.hashCode());
				}
				if (pairs.size() >= 50000) {
					System.out.println("write " + pairs.size() + " pairs in db");
					Cat_DBConnector.writePairs(connection, pairs.keySet(), type, tableName);

					pairs.clear();
				}
				Double d = scoresByEntity.get(entity);
				if(d == null) d = 0.0;
				if(d < pair.getScore()){
					scoresByEntity.put(entity, pair.getScore());
				}
				d = scoresByEntity.get(cooccEntity);
				if(d == null) d = 0.0;
				if(d < pair.getScore()){
					scoresByEntity.put(entity, pair.getScore());
				}
			}
		}
		System.out.println("write " + pairs.size() + " pairs in db");
		Cat_DBConnector.writePairs(connection, pairs.keySet(), type, tableName);
		Map<Double,List<Entity>> entitiesByScore = new HashMap<Double,List<Entity>>();
		for (Entity e : scoresByEntity.keySet()) {
			Double d = scoresByEntity.get(e);
			List<Entity> list = entitiesByScore.get(d);
			if(list == null) list = new ArrayList<Entity>();
			list.add(e);
			entitiesByScore.put(d, list);
		}
		return entitiesByScore;
	}


	public static Map<Entity, Integer> reverseMap2(Map<Integer, List<Entity>> map) {
		Map<Entity, Integer> toReturn = new HashMap<Entity, Integer>();
		for (int i : map.keySet()) {
			for (Entity e : map.get(i)) {
				toReturn.put(e, i);
			}
		}
		return toReturn;
	}

	public static Map<Entity, Set<Sentence>> trimSentences(Map<Entity, Set<Sentence>> allSentencesByEntity,
			int contextSize) {
		Set<Sentence> trimmedSentences = null;
		int errorCount = 0;
		String lemma, trimmed, sentLemmata, normLemma, normSentence = null;
		for (Entity entity : allSentencesByEntity.keySet()) {
			trimmedSentences = new HashSet<Sentence>();
			lemma = entity.getLemma();
			for (Sentence sentence : allSentencesByEntity.get(entity)) {
				sentLemmata = sentence.getLemmata();
				try {
					trimmed = trim(sentLemmata, lemma, contextSize);
					sentence.setTrimmed(trimmed);
					trimmedSentences.add(sentence);
				} catch (IndexOutOfBoundsException e) {
					try {
						normLemma = lemma.replaceAll("(^pc | pc | pc$|^pc$)", " -- ").trim();
						normSentence = sentLemmata.replaceAll("(^pc | pc | pc$|^pc$)", " -- ").trim();
						trimmed = trim(normSentence, normLemma, contextSize);
						sentence.setTrimmed(trimmed);
						trimmedSentences.add(sentence);
					} catch (IndexOutOfBoundsException e2) {
							errorCount++;
					}
				}
			}
			allSentencesByEntity.put(entity, trimmedSentences);
		}
		System.out.println("ErrorCount: " + errorCount);
		return allSentencesByEntity;
	}

	private static String trim(String sentence, String lemma, int contextSize) {
		String before = sentence.substring(0, sentence.indexOf(lemma));
		String[] splitBefore = before.split(" ");
		String after;
		if (sentence.indexOf(lemma) + lemma.length() == sentence.length()) {
			after = "";
		} else {
			after = sentence.substring(sentence.indexOf(lemma) + lemma.length(), sentence.length() - 1);
		}
		String[] splitAfter = after.split(" ");
		StringBuffer trimmed = new StringBuffer();
		if (splitBefore.length <= contextSize) {
			for (String string : splitBefore) {
				trimmed.append(string + " ");
			}
		} else {
			int offset = splitBefore.length - contextSize;
			for (int i = offset; i < (offset + contextSize); i++) {
				trimmed.append(splitBefore[i] + " ");
			}
		}
		trimmed.append(lemma);
		if (splitAfter.length <= contextSize) {
			for (String string : splitAfter) {
				trimmed.append(string + " ");
			}
		} else {
			for (int i = 0; i <= contextSize; i++) {
				trimmed.append(" " + splitAfter[i]);
			}
		}
		return trimmed.toString();
	}

	public static String normalizeSentence(String sentence) {
		StringBuffer sb = new StringBuffer();
		String[] split = sentence.split(" ");
		for (String string : split) {
			sb.append(" " + normalizeLemma(string));
		}
		return sb.toString().substring(1);
	}

	private static String normalizeLemma(String lemma) {
		lemma = lemma.trim();
		if (lemma.equals("--")) {
			return lemma;
		}
		if (lemma.startsWith("<end-")) {
			return lemma;
		}
		if (lemma.startsWith("<root-"))
			if (lemma.length() <= 1) {
				return lemma;
			}
		while (true) {
			lemma = lemma.trim();
			if (lemma.length() == 0) {
				break;
			}
			Character s = lemma.charAt(0);
			if (s == '_') {
				lemma = lemma.substring(1);
				lemma = lemma.trim();
			}
			if (lemma.length() == 0) {
				break;
			}
			if (!Character.isLetter(s) && !Character.isDigit(s) && !(s == '§')) {
				lemma = lemma.substring(1);
				lemma = lemma.trim();
			} else {
				break;
			}
			if (lemma.length() == 0) {
				break;
			}
		}
		while (true) {
			if (lemma.length() == 0) {
				break;
			}
			Character e = lemma.charAt(lemma.length() - 1);
			if (e == '_') {
				lemma = lemma.substring(0, lemma.length() - 1);
				lemma = lemma.trim();
			}

			if (!Character.isLetter(e) && !Character.isDigit(e) && !(e == '+') && !(e == '#')) {
				lemma = lemma.substring(0, lemma.length() - 1);
				lemma = lemma.trim();
			} else {
				break;
			}
		}

		return lemma;

	}

	public static int countSentences(Map<Entity, Set<Sentence>> allSentencesByEntity) {
		int count = 0;
		Set<String> differentSentences = new HashSet<String>();
		Set<String> differentIds = new HashSet<String>();
		for (Entity e : allSentencesByEntity.keySet()) {
			for (Sentence s : allSentencesByEntity.get(e)) {
				boolean isNewSent = differentSentences.add(s.getTrimmed());
				boolean isNewId = differentIds.add(s.getId());
				if(isNewSent){
					count++;
				}
				else if(isNewId){
					count++;
				}
			}
		}
		return count;
	}
	
	

}
