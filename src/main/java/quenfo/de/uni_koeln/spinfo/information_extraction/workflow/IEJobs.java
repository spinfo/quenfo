package quenfo.de.uni_koeln.spinfo.information_extraction.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.Pattern;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.PatternToken;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.TextToken;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.Token;

/**
 * @author geduldia Enthält alle Methoden zur Extraktion und zum Matching von
 *         Tools bzw. Kompetenezn
 *
 */
public class IEJobs {
	// Liste von Falsch-Extraktionen, die nicht wieder extrahiert werden sollen
	// (sortiert nach dem Anfangswort)
	public Map<String, Set<List<String>>> negExamples;
	// Liste mit modifizierenden Ausdrücken (nur für den Kompetenz-Workflow)
	private Map<String, Set<List<String>>> modifiers;
	// Liste mit den bereits bekannten Kompetenzen/Tools (sortiert nach dem
	// Anfangswort)
	public Map<String, Set<InformationEntity>> entities;
	// Informationstyp (Kompetenz oder Tool)
	IEType type;
	// Anahl der bereits bekannten Kompetenzen/Tools
	public int knownEntities;
	// Extraktionspatterns
	public List<Pattern> patterns;

	/**
	 * @param competences
	 * @param noCompetences
	 * @param importanceTerms
	 * @param patterns
	 * @param type
	 * @throws IOException
	 */
	public IEJobs(File competences, File noCompetences, File importanceTerms, File patterns, IEType type)
			throws IOException {
		this.type = type;
		initialize(competences, noCompetences, importanceTerms, patterns);
	}

	/**
	 * @param tools
	 * @param noTools
	 * @param patterns
	 * @param type
	 * @throws IOException
	 */
	public IEJobs(File tools, File noTools, File patterns, IEType type) throws IOException {
		this.type = type;
		initialize(tools, noTools, patterns);
	}

	/**
	 * @param tools
	 * @param noTools
	 * @param patterns
	 * @throws IOException
	 */
	private void initialize(File tools, File noTools, File patterns) throws IOException {
		initialize(tools, noTools, null, patterns);
	}

	// liest die verschiedenen Files ein und initialisiert die zugehörigen
	// Felder
	private void initialize(File knownEntities, File negativeEntities, File modifiersFile, File patternsFile)
			throws IOException {
		entities = new HashMap<String, Set<InformationEntity>>();
		this.knownEntities = 0;
		if (knownEntities != null) {
			readKnownEntitiesFromFile(knownEntities);
		}
		negExamples = new HashMap<String, Set<List<String>>>();
		if (negativeEntities != null) {
			readWordList(negativeEntities, negExamples);
		}
		modifiers = new HashMap<String, Set<List<String>>>();
		if (modifiersFile != null) {
			readWordList(modifiersFile, modifiers);
		}
		patterns = new ArrayList<Pattern>();
		if (patternsFile != null) {
			readPatterns(patterns, patternsFile);
		}
	}

	/**
	 * reads entities from the innput-File and adds them to the global field
	 * 'entities'
	 * 
	 * @param entitiesFile
	 * @throws IOException
	 */
	public void readKnownEntitiesFromFile(File entitiesFile) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(entitiesFile));
		String line = in.readLine();
		while (line != null) {
			if (line.equals("")) {
				line = in.readLine();
				continue;
			}
			String[] split = line.split(" ");
			String keyword;
			try {
				keyword = normalizeLemma(split[0]);
			} catch (ArrayIndexOutOfBoundsException e) {
				line = in.readLine();
				continue;
			}
			Set<InformationEntity> iesForKeyword = entities.get(keyword);
			if (iesForKeyword == null)
				iesForKeyword = new HashSet<InformationEntity>();
			InformationEntity ie = new InformationEntity(keyword, split.length == 1);
			if (!ie.isSingleWordEntity()) {
				for (String string : split) {
					ie.addLemma(normalizeLemma(string));

				}
			}
			boolean isnew = iesForKeyword.add(ie);
			if (isnew) {
				knownEntities++;
			}
			entities.put(keyword, iesForKeyword);
			line = in.readLine();
		}
		in.close();
	}

	// Liest die Begriffe (Modifizierer oder Falsch-Extraktionen) aus dem
	// input-File und ergänzt sie in der map
	private void readWordList(File inputFile, Map<String, Set<List<String>>> map) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(inputFile));
		String line = in.readLine();
		while (line != null) {
			if (line.equals("")) {
				line = in.readLine();
				continue;
			}
			String keyword;
			String[] split = line.split(" ");
			try {
				keyword = normalizeLemma(split[0]);
			} catch (ArrayIndexOutOfBoundsException e) {
				line = in.readLine();
				continue;
			}
			Set<List<String>> expressionsForKeyword = map.get(keyword);
			if (expressionsForKeyword == null) {
				expressionsForKeyword = new HashSet<List<String>>();
			}

			List<String> expression = Arrays.asList(split);
			expressionsForKeyword.add(expression);
			map.put(keyword, expressionsForKeyword);
			line = in.readLine();
		}
		in.close();
	}

	/**
	 * read extraction-patterns from file
	 * 
	 * @param patternFile
	 *            file with patterns in a specific pattern format
	 * @param patterns
	 *            list to store the read patterns 
	 * @throws IOException
	 */
	private void readPatterns(List<Pattern> patterns, File patternFile) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(patternFile));
		String line = in.readLine();
		Pattern pattern = new Pattern();
		int lineNumber = 0;
		while (line != null) {
			lineNumber++;
			String[] split = line.split("\t");
			// set ID
			try {
				if (line.startsWith("ID:")) {
					pattern.setId(Integer.parseInt(split[1]));
				}
				// set Name
				if (line.startsWith("NAME:")) {
					pattern.setDescription(split[1].trim());
				}
				// set Token
				if (line.startsWith("TOKEN:")) {
					String string = split[1];
					if (string.equals("null"))
						string = null;
					String lemma = split[2];
					if (lemma.equals("null"))
						lemma = null;
					String posTag = split[3];
					if (posTag.equals("null"))
						posTag = null;
					Token token = new PatternToken(string, lemma, posTag, Boolean.parseBoolean(split[4]));
					if (lemma != null && lemma.toUpperCase().equals("IMPORTANCE")) {
						token.setModifier(true);
					}
					pattern.addToken((PatternToken) token);
				}
				// set Entity-Pointer
				if (line.startsWith("EXTRACT:")) {
					List<Integer> pointer = new ArrayList<Integer>();
					String[] ints = split[1].split(",");
					for (String string : ints) {
						pointer.add(Integer.parseInt(string));
					}
					pattern.setPointer(pointer);
					patterns.add(pattern);
					pattern = new Pattern();
				}
			} catch (Exception e) {
				System.out.println("Error in pattern file (line " + lineNumber + ")");
			}
			line = in.readLine();
		}
		in.close();
	}

	/**
	 * annotate Tokens of the given ExtractionUnits as knownEntity, noEntity or
	 * modifier
	 * 
	 * @param extractionUnits
	 * @throws IOException
	 */
	public void annotateTokens(List<ExtractionUnit> extractionUnits) throws IOException {
		for (ExtractionUnit currentExtractionUnit : extractionUnits) {
			List<TextToken> tokens = currentExtractionUnit.getTokenObjects();
			if (!entities.isEmpty()) {
				annotateEntities(tokens);
			}
			if (!negExamples.isEmpty()) {
				annotateNegativeExamples(tokens);
			}
			if (modifiers != null) {
				annotateModifiers(tokens);
			}
		}
	}

	// prüft die Tokens auf bereits bekannte Kompetenzen/Tools und zeichnet sie
	// als solche aus
	private void annotateEntities(List<TextToken> tokens) {
		for (int t = 0; t < tokens.size(); t++) {
			Token currentToken = tokens.get(t);
			String lemma = normalizeLemma(currentToken.getLemma());
			if (entities.keySet().contains(lemma)) {
				for (InformationEntity ie : entities.get(lemma)) {
					if (ie.isSingleWordEntity()) {
						// currentToken ist eine bekannte Kompetenz/Tool
						currentToken.setIEToken(true);
						continue;
					}
					// currentToken könnte das erste Token einer Kompetenz/Tool
					// sein
					boolean matches = false;
					for (int c = 1; c < ie.getLemmata().size(); c++) {
						if (tokens.size() <= t + c) {
							matches = false;
							break;
						}
						matches = ie.getLemmata().get(c).equals(normalizeLemma(tokens.get(t + c).getLemma()));
						if (!matches) {
							break;
						}
					}
					if (matches) {
						currentToken.setIEToken(true);
						((TextToken) currentToken).setTokensToCompleteInformationEntity(ie.getLemmata().size() - 1);
					}
				}
			}
		}
	}

	// prüft die Tokens auf bereits bekannte Falsch-Extraktionen und zeichnet
	// sie als solche aus
	private void annotateNegativeExamples(List<TextToken> tokens) {
		if (negExamples == null)
			return;
		for (int t = 0; t < tokens.size(); t++) {
			Token currentToken = tokens.get(t);
			String lemma = normalizeLemma(currentToken.getLemma());
			if (negExamples.keySet().contains(lemma)) {
				boolean match = false;
				for (List<String> expression : negExamples.get(lemma)) {
					for (int s = 0; s < expression.size(); s++) {
						String string = expression.get(s);
						try {
							match = string.equals(tokens.get(t + s).getLemma());
						} catch (ArrayIndexOutOfBoundsException e) {
							break;
						}
						if (!match)
							break;
					}
					if (match)
						break;
				}
				// current token is negative example
				if (match) {
					((TextToken) currentToken).setNoEntity(true);
				}
			}
		}
	}

	private void annotateModifiers(List<TextToken> tokens) {
		int skip = 0;
		for (int t = 0; t < tokens.size(); t++) {
			if (t + skip >= tokens.size())
				break;
			Token currentToken = tokens.get(t + skip);
			String lemma = normalizeLemma(currentToken.getLemma());
			if (modifiers.keySet().contains(lemma)) {
				int required = -1;
				boolean match = false;
				for (List<String> expression : modifiers.get(lemma)) {
					for (int s = 0; s < expression.size(); s++) {
						String string = expression.get(s);
						try {
							match = string.equals(normalizeLemma(tokens.get(t + skip + s).getLemma()));
						} catch (ArrayIndexOutOfBoundsException e) {
							break;
						}
						if (!match)
							break;
					}

					if (match) {
						if (expression.size() > required) {
							required = expression.size() - 1;
						}
					}
				}
				if (required > -1) {
					currentToken.setModifier(true);
					((TextToken) currentToken).setTokensToCompleteModifier(required);
					skip += required;
				}

			}
		}
	}

	/**
	 * extracts entities.
	 * 
	 * @param extractionUnits
	 * @return
	 */
	public Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extractEntities(
			List<ExtractionUnit> extractionUnits) {
		Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> toReturn = new HashMap<ExtractionUnit, Map<InformationEntity, List<Pattern>>>();
		
		List<TextToken> textTokens;
		Token textToken;
		Token patternToken;
		int entityPointer;
		int requiredForModifier;
		int requiredForEntity;
		boolean match;
		InformationEntity e;
		TextToken entityToken;
		
		for (ExtractionUnit extractionUnit : extractionUnits) {
			textTokens = extractionUnit.getTokenObjects();
			for (Pattern pattern : patterns) {
				for (int t = 0; t <= textTokens.size() - pattern.getSize(); t++) {
					match = false;
					entityPointer = 0;
					requiredForModifier = 0;
					requiredForEntity = 0;
					for (int c = 0; c < pattern.getSize(); c++) {
						int i = t + requiredForModifier + requiredForEntity;
						if (i + c >= textTokens.size())
							continue;
						textToken = textTokens.get(i + c);
						patternToken = pattern.getTokenAt(c);
						match = ((TextToken) textToken).isEqualsPatternToken((PatternToken) patternToken);
						if (!match) {
							break;
						}
						// token und patternToken matchen
						if (pattern.getPointer().get(0) == c) {
							entityPointer = i + c;
						}
						if (patternToken.isInformationEntity()) {
							requiredForEntity = ((TextToken) textToken).getTokensToCompleteInformationEntity();
						}
						if (patternToken.isModifier()) {
							requiredForModifier = ((TextToken) textToken).getTokensToCompleteModifier();
						}
					}
					if (match) {
						entityToken = textTokens.get(entityPointer);
						String normLemma = normalizeLemma(entityToken.getLemma());
						int entity_size = pattern.getPointer().size();
						if (entity_size == 1) {
							if (entityToken.isModifier() || entityToken.isNoEntity()) {
								e = null;
								continue;
							}
							if (normLemma.length() > 1 && !(entityToken.getLemma().equals("--"))) {
								e = new InformationEntity(normLemma, true);
							} else {
								e = null;
								continue;
							}
						} else {
							if (normLemma.length() > 1 && !(entityToken.getLemma().equals("--"))) {
								e = new InformationEntity(normLemma, false);
							} else {
								e = null;
								continue;
							}
							List<String> completeEntity = new ArrayList<String>();
							for (int p = 0; p < pattern.getPointer().size(); p++) {
								Token currentToken = textTokens.get(entityPointer + p);
								String s = normalizeLemma(currentToken.getLemma());
								if (!s.trim().equals("") && !s.trim().equals("--")) {
									completeEntity.add(s);
								}
							}
							if (completeEntity.size() > 1) {
								e = new InformationEntity(completeEntity.get(0), false);
								e.setExpression(completeEntity);
							} else if (completeEntity.size() < 1) {
								e = null;
								continue;
							} else {
								e = new InformationEntity(completeEntity.get(0), true);
							}
						}
						// check if full entity is listed in negExamples
						boolean isNoEntity = false;
						if (negExamples.containsKey(e.getStartLemma())) {
							if (negExamples.get(e.getStartLemma()).contains(e.getLemmata())) {
								isNoEntity = true;
							}
						}
						if (isNoEntity) {
							e = null;
							continue;
						}
						if (type == IEType.COMPETENCE) {
							removeModifier(e);
						}
						if (e.getLemmata().size() < 1) {
							e = null;
							continue;
						}
						Map<InformationEntity, List<Pattern>> map = toReturn.get(extractionUnit);
						if (map == null)
							map = new HashMap<InformationEntity, List<Pattern>>();
						List<Pattern> list = map.get(e);
						if (list == null)
							list = new ArrayList<Pattern>();
						list.add(pattern);
						map.put(e, list);
						extractionUnit.deleteData();
						toReturn.put(extractionUnit, map);
					}
				}
			}
		}
		textTokens = null;
		return toReturn;
	}

	private void removeModifier(InformationEntity e) {
		List<String> lemmata = e.getLemmata();
		List<String> toDelete = new ArrayList<>();
		int skip = 0;
		int required = -1;
		for (int t = 0; t < lemmata.size(); t++) {
			if (t + skip >= lemmata.size())
				break;
			String lemma = normalizeLemma(lemmata.get(t + skip));
			if (modifiers.containsKey(lemma)) {
				required = -1;
				boolean match = false;
				for (List<String> expression : modifiers.get(lemma)) {
					if (expression.size() > lemmata.size())
						continue;
					for (int s = 0; s < expression.size(); s++) {
						String string = expression.get(s);
						try {
							match = string.equals(normalizeLemma(lemmata.get(t + skip + s)));
						} catch (IndexOutOfBoundsException ex) {
							match = false;
						}
						if (!match)
							break;
					}

					if (match) {
						if (expression.size() > required) {
							required = expression.size() - 1;
						}
					}
				}
				if (required > -1) {
					toDelete.add(lemma);
					for (int i = 1; i <= required; i++) {
						toDelete.add(lemmata.get(t + skip + i));
					}
					skip += required;
				}
			}
		}
		for (String s : toDelete) {
			lemmata.remove(s);
		}
		e.setExpression(lemmata);
	}

	/**
	 * merge entities if one fully contains the other (except the larger one
	 * contains 'und' or 'oder')
	 * 
	 * @param extractions
	 *            map of extractionUnits and extracted entities
	 * @return merged extractions
	 */
	public Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> mergeInformationEntities(
			Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extractions) {
		for (ExtractionUnit ieunit : extractions.keySet()) {
			Map<InformationEntity, List<Pattern>> merged = new HashMap<InformationEntity, List<Pattern>>();
			List<InformationEntity> iesForUnit = new ArrayList<>(extractions.get(ieunit).keySet());
			InformationEntity containingIE = null;
			for (int i = 0; i < iesForUnit.size(); i++) {
				InformationEntity currentIE = iesForUnit.get(i);
				boolean isPartOfOtherIE = false;
				for (int j = 0; j < iesForUnit.size(); j++) {
					if (j == i)
						continue;
					InformationEntity otherIE = iesForUnit.get(j);
					// check if currentIE is in otherIE
					isPartOfOtherIE = containsList(otherIE.getLemmata(), currentIE.getLemmata());
					if (isPartOfOtherIE) {
						containingIE = otherIE;
						break;
					}
				}
				if (!isPartOfOtherIE) {
					merged.put(currentIE, extractions.get(ieunit).get(currentIE));
				} else {
					if (containingIE.getLemmata().contains("und") || containingIE.getLemmata().contains("oder")) {
						if (!currentIE.getLemmata().contains("und") && !(currentIE.getLemmata().contains("oder"))) {
							merged.put(currentIE, extractions.get(ieunit).get(currentIE));
						}
					}
				}
			}
			extractions.put(ieunit, merged);
		}
		return extractions;
	}

	/**
	 * 
	 * finds known entities in unknown s and returns both
	 * 
	 * @param extractionUnits
	 * @param patternExtractions
	 * @return
	 */
	public Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extractByStringMatch(
			List<ExtractionUnit> extractionUnits) {
		Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extractions = new HashMap<ExtractionUnit, Map<InformationEntity, List<Pattern>>>();
		List<Pattern> List = new ArrayList<Pattern>();
		for (ExtractionUnit extractionUnit : extractionUnits) {
			List<TextToken> tokens = extractionUnit.getTokenObjects();
			int skip = 0;
			for (int t = 0; t < tokens.size(); t++) {
				if (t + skip >= tokens.size())
					break;
				Token token = tokens.get(t + skip);
				String lemma = normalizeLemma(token.getLemma());
				if (entities.keySet().contains(lemma)) {
					for (InformationEntity ie : entities.get(lemma)) {
						if (ie.isSingleWordEntity()) {
							token.setIEToken(true);
							InformationEntity newIE = new InformationEntity(ie.getStartLemma(), true);
							Map<InformationEntity, List<Pattern>> iesForUnit = extractions.get(extractionUnit);
							if (iesForUnit == null)
								iesForUnit = new HashMap<InformationEntity, List<Pattern>>();
							iesForUnit.put(newIE, List);
							extractions.put(extractionUnit, iesForUnit);

							continue;
						}
						boolean matches = false;
						for (int c = 0; c < ie.getLemmata().size(); c++) {
							if (tokens.size() <= t + c) {
								matches = false;
								break;
							}
							matches = ie.getLemmata().get(c).equals(normalizeLemma(tokens.get(t + c+ skip).getLemma()));
							if (!matches) {
								break;
							}
						}
						if (matches) {
							token.setIEToken(true);
							((TextToken) token).setTokensToCompleteInformationEntity(ie.getLemmata().size() - 1);
							InformationEntity newIE = new InformationEntity(ie.getStartLemma(), false);
							newIE.setExpression(ie.getLemmata());
							Map<InformationEntity, List<Pattern>> iesForUnit = extractions.get(extractionUnit);
							if (iesForUnit == null)
								iesForUnit = new HashMap<InformationEntity, List<Pattern>>();
							iesForUnit.put(newIE, List);
							extractions.put(extractionUnit, iesForUnit);
						}
					}
				}
//				if (token.isInformationEntity()) {
//					skip += ((TextToken) token).getTokensToCompleteInformationEntity();
//					System.out.println("skip: " +skip);
//				}
			}
		}
		return extractions;
	}

	/**
	 * identifies modifiers in the given ExtractionUnits and adds them to all
	 * information-entities in the ExtractionUnit
	 * 
	 * @param extractionUnits
	 */
	public void setModifiers(Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extractionUnits) {
		for (ExtractionUnit extractionUnit : extractionUnits.keySet()) {
			String longestMatchingModifer = null;
			int modifierLength = 0;
			List<TextToken> tokens = extractionUnit.getTokenObjects();
			int skip = 0;
			for (int t = 0; t < tokens.size(); t++) {
				if (t + skip >= tokens.size())
					break;
				Token currentToken = tokens.get(t + skip);
				String lemma = normalizeLemma(currentToken.getLemma());
				if (currentToken.isModifier() && ((TextToken) currentToken).getTokensToCompleteModifier() == 0) {
					if (longestMatchingModifer == null) {
						longestMatchingModifer = lemma;
						modifierLength = 1;
					}
				}

				if (currentToken.isModifier() && ((TextToken) currentToken).getTokensToCompleteModifier() > 0) {
					if (((TextToken) currentToken).getTokensToCompleteModifier() > modifierLength) {
						StringBuffer sb = new StringBuffer();
						for (int i = 0; i < ((TextToken) currentToken).getTokensToCompleteModifier() + 1; i++) {
							sb.append(normalizeLemma(tokens.get(t + skip + i).getLemma()) + " ");
						}
						longestMatchingModifer = sb.toString().trim();
						modifierLength = ((TextToken) currentToken).getTokensToCompleteModifier();
					}
				}
			}
			if (longestMatchingModifer != null) {
				for (InformationEntity ie : extractionUnits.get(extractionUnit).keySet()) {
					ie.setImportance(longestMatchingModifer);
				}
			}
		}
	}

	/**
	 * adds the given Set of Strings to the entities-list
	 * 
	 * @param entities
	 * @throws IOException
	 */
	public void addKnownEntities(Set<String> entities) throws IOException {
		if (entities == null)
			return;
		for (String comp : entities) {
			// create inform.-entity and add to posExample list
			String[] split = comp.split(" ");
			String keyword;
			try {
				keyword = normalizeLemma(split[0]);

			} catch (ArrayIndexOutOfBoundsException e) {
				continue;
			}
			Set<InformationEntity> iesForKeyword = this.entities.get(keyword);
			if (iesForKeyword == null)
				iesForKeyword = new HashSet<InformationEntity>();
			InformationEntity ie = new InformationEntity(keyword, split.length == 1);
			if (!ie.isSingleWordEntity()) {
				for (String string : split) {
					ie.addLemma(normalizeLemma(string));

				}
			}
			boolean isnew = iesForKeyword.add(ie);
			if (isnew) {
				knownEntities++;
			}
			this.entities.put(keyword, iesForKeyword);
		}
	}

	/**
	 * adds the given set of strings to the noEntities-List
	 * 
	 * @param noEntities
	 */
	public void addNoEntities(Set<String> noEntities) {
		if (noEntities == null)
			return;
		for (String line : noEntities) {
			String keyword;
			String[] split = line.split(" ");
			try {
				keyword = normalizeLemma(split[0]);
			} catch (ArrayIndexOutOfBoundsException e) {
				continue;
			}
			Set<List<String>> expressionsForKeyword = negExamples.get(keyword);
			if (expressionsForKeyword == null) {
				expressionsForKeyword = new HashSet<List<String>>();
			}

			List<String> expression = Arrays.asList(split);
			expressionsForKeyword.add(expression);
			negExamples.put(keyword, expressionsForKeyword);

		}
	}

	/**
	 * normalizes the given string - trim - deletes (most) special characters at
	 * the begin and end of the string (with some exceptions)
	 * 
	 * @param lemma
	 *            string to normalize
	 * @return normalized string
	 */
	private String normalizeLemma(String lemma) {
		String before = lemma;
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
		// if(!before.trim().equals(lemma.trim())){
		// System.out.println("\nbefore: "+ before);
		// System.out.println("after: "+lemma);
		// }

		return lemma;

	}

	private boolean containsList(List<String> a, List<String> b) {
		if (a.size() < b.size()) {
			return false;
		}
		for (int i = 0; i <= a.size() - b.size(); i++) {
			boolean match = false;
			for (int j = 0; j < b.size(); j++) {
				match = a.get(i + j).equals(b.get(j));
				if (!match)
					break;
			}
			if (match) {
				return true;
			}
		}
		return false;
	}
}
