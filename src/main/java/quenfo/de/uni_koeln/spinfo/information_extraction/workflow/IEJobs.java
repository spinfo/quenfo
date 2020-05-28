package quenfo.de.uni_koeln.spinfo.information_extraction.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.io.Files;

import de.uni_koeln.spinfo.workflow.CoordinateExpander;
import is2.tools.Tool;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.Pattern;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.PatternToken;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.TextToken;
import de.uni_koeln.spinfo.data.Token;
import quenfo.de.uni_koeln.spinfo.information_extraction.utils.Util;

/**
 * TODO JB: Refactoring
 * 20.02.19: Methoden zur String-Verarbeitung in "Util" verschoben
 * public in knownEntities @Deprecated
 * 
 */

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

	// Liste mit den bereits bekannten Kompetenzen/Tools (key = Anfangswort)
	// public Map<String, Set<InformationEntity>> catEntities;

	public Map<String, Set<List<String>>> tokensToRemove;
	// Informationstyp (Kompetenz oder Tool)
	IEType type;
	// Anahl der bereits bekannten Kompetenzen/Tools
	@Deprecated
	public int knownEntities;
	// Extraktionspatterns
	public List<Pattern> patterns;
	// Tool zur Morphemkoordinations-Auflösung
	// private CoordinationResolver cr;
	private CoordinateExpander ce;

	//private static Logger log = LoggerFactory.getLogger(IEJobs.class);
	Logger log = Logger.getLogger(getClass());
	
	/**
	 * @param competences
	 * @param noCompetences
	 * @param importanceTerms
	 * @param patterns
	 * @param type
	 * @param possCompoundsFile
	 * @throws IOException
	 */
	public IEJobs(File competences, File noCompetences, File importanceTerms, File patterns, IEType type,
			boolean resolveCoordinations, File possCompoundsFile, File splittedCompoundsFile) throws IOException {
		this.type = type;
		// TODO passt der Konstruktor so?
		initialize(competences, noCompetences, null, null, importanceTerms, patterns, resolveCoordinations,
				possCompoundsFile, splittedCompoundsFile);
	}

	/**
	 * @param tools
	 * @param noTools
	 * @param patterns
	 * @param type
	 * @throws IOException
	 */
	public IEJobs(File tools, File noTools, File patterns, IEType type, boolean resolveCoordinations)
			throws IOException {
		this.type = type;
		initialize(tools, noTools, patterns, resolveCoordinations);
	}

	public IEJobs(File competences, File noCompetences, File amsComps, String category, File importanceTerms,
			File patterns, IEType type, boolean resolveCoordinations) throws IOException {
		this.type = type;
		initialize(competences, noCompetences, amsComps, category, importanceTerms, patterns, resolveCoordinations,
				null, null);
	}

	public IEJobs(File competences, File noCompetences, File amsComps, String category, File importanceTerms,
			File patterns, IEType type, boolean resolveCoordinations, File possCompoundsFile,
			File splittedCompoundsFile) throws IOException {
		this.type = type;
		initialize(competences, noCompetences, amsComps, category, importanceTerms, patterns, resolveCoordinations,
				possCompoundsFile, splittedCompoundsFile);
	}

	/**
	 * @param tools
	 * @param noTools
	 * @param patterns
	 * @throws IOException
	 */
	private void initialize(File tools, File noTools, File patterns, boolean resolveCoordinations) throws IOException {
		initialize(tools, noTools, null, null, null, patterns, resolveCoordinations, null, null);
	}

	// liest die verschiedenen Files ein und initialisiert die zugehörigen
	// Felder
	private void initialize(File knownEntities, File negativeEntities, File teiFile, String category,
			File modifiersFile, File patternsFile, boolean resolveCoordinations, File possCoordinates,
			File splittedCompoundsFile) throws IOException {

		if (resolveCoordinations)
			this.ce = new CoordinateExpander(possCoordinates, splittedCompoundsFile); 


		this.knownEntities = 0;
		entities = new HashMap<String, Set<InformationEntity>>();
		if (knownEntities != null) {
			readKnownEntitiesFromFile(knownEntities);
		}
		if (teiFile != null) {
			readTEIFile(teiFile, category);
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

	private void readTEIFile(File teiFile, String category) throws IOException {

		entities = Util.readTEI(teiFile, category, entities);

	}

	/**
	 * reads entities from the innput-File and adds them to the global field
	 * 'entities'
	 * 
	 * @param entitiesFile
	 * @throws IOException
	 */
	private void readKnownEntitiesFromFile(File entitiesFile) throws IOException {

		String extension = Files.getFileExtension(entitiesFile.getAbsolutePath());
		
		
		if (extension.equals("txt")) {
			log.info("Read Skills from txt-File: " + entitiesFile.getAbsolutePath());
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
					keyword = Util.normalizeLemma(split[0]);
				} catch (ArrayIndexOutOfBoundsException e) {
					line = in.readLine();
					continue;
				}
				Set<InformationEntity> iesForKeyword = entities.get(keyword);
				if (iesForKeyword == null)
					iesForKeyword = new HashSet<InformationEntity>();
				// nicht kategorisierte IEs haben kein Label
				InformationEntity ie = new InformationEntity(keyword, split.length == 1, "");
				if (!ie.isSingleWordEntity()) {
					for (String string : split) {
						ie.addLemma(Util.normalizeLemma(string));

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
		} else if (extension.equals("ttl")) {
			log.info("Read Skills from RDF Model: " + entitiesFile.getAbsolutePath());
			entities = Util.readRDF(entitiesFile, entities);
		} else if (extension.equals("csv")) {
			log.info("Read Skills from CSV: " + entitiesFile.getAbsolutePath());
			entities = Util.readCSV(entitiesFile, entities);
		} else {
			System.err.println("unbekanntes Dateiformat: " + entitiesFile.getAbsolutePath());
		}

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
				keyword = Util.normalizeLemma(split[0]);
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
	 * @param patternFile file with patterns in a specific pattern format
	 * @param patterns    list to store the read patterns
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
					pattern.setExtractionPointer(pointer);
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
			String lemma = Util.normalizeLemma(currentToken.getLemma());
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
						matches = ie.getLemmata().get(c).equals(Util.normalizeLemma(tokens.get(t + c).getLemma()));
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
			String lemma = Util.normalizeLemma(currentToken.getLemma());
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
			String lemma = Util.normalizeLemma(currentToken.getLemma());
			if (modifiers.keySet().contains(lemma)) {
				int required = -1;
				boolean match = false;
				for (List<String> expression : modifiers.get(lemma)) {
					for (int s = 0; s < expression.size(); s++) {
						String string = expression.get(s);
						try {
							match = string.equals(Util.normalizeLemma(tokens.get(t + skip + s).getLemma()));
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
	 * @param lemmatizer
	 * @return
	 */
	public Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extractEntities(
			List<ExtractionUnit> extractionUnits, Tool lemmatizer) {
		Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> toReturn = new HashMap<ExtractionUnit, Map<InformationEntity, List<Pattern>>>();

		List<TextToken> ieTokens;
		Token token;
		Token patternToken;
		int entityPointer;
		int requiredForModifier;
		int requiredForEntity;
		boolean match;
		InformationEntity ie;
		TextToken entityToken;

		for (ExtractionUnit extractionUnit : extractionUnits) {

			ieTokens = extractionUnit.getTokenObjects();

			for (Pattern pattern : patterns) {
				for (int t = 0; t <= ieTokens.size() - pattern.getSize(); t++) {
					match = false;
					entityPointer = 0;
					requiredForModifier = 0;
					requiredForEntity = 0;
					for (int c = 0; c < pattern.getSize(); c++) {
						int i = t + requiredForModifier + requiredForEntity;
						if (i + c >= ieTokens.size())
							continue;
						token = ieTokens.get(i + c);

						patternToken = pattern.getTokenAt(c);
						match = ((TextToken) token).isEqualsPatternToken((PatternToken) patternToken);
						if (!match) {
							break;
						}
						// token und patternToken matchen
						if (pattern.getExtractionPointer().get(0) == c) {
							entityPointer = i + c;
						}
						if (patternToken.isInformationEntity()) {
							requiredForEntity = ((TextToken) token).getTokensToCompleteInformationEntity();
						}
						if (patternToken.isModifier()) {
							requiredForModifier = ((TextToken) token).getTokensToCompleteModifier();
						}
					}
					if (match) {
						List<InformationEntity> informationEntities = new ArrayList<InformationEntity>();
						entityToken = ieTokens.get(entityPointer);
						String normLemma = Util.normalizeLemma(entityToken.getLemma());

						int entity_size = pattern.getExtractionPointer().size();
						if (entity_size == 1) {
							if (entityToken.isModifier() || entityToken.isNoEntity()) {
								ie = null;
								continue;
							}
							if (normLemma.length() > 1 && !(entityToken.getLemma().equals("--"))) {
								ie = new InformationEntity(normLemma, true);
							} else {
								ie = null;
								continue;
							}
						} else {
							if (normLemma.length() > 1 && !(entityToken.getLemma().equals("--"))) {
								ie = new InformationEntity(normLemma, false);
							} else {
								ie = null;
								continue;
							}
							List<Token> completeEntity = new ArrayList<>();
							for (int p = 0; p < pattern.getExtractionPointer().size(); p++) {
								TextToken currentToken = ieTokens.get(entityPointer + p);
								String s = Util.normalizeLemma(currentToken.getLemma());
								if (!s.trim().equals("") && !s.trim().equals("--")) {
									completeEntity.add(currentToken);
								}
							}
							if (completeEntity.size() > 1) { // Entität besteht aus mehr als eine Token
								List<String> entities = new ArrayList<String>();

//								boolean isMorphemCoordination = false;

								// prüfen, ob es sich um eine morphemkoordination handelt
								for (Token tt : completeEntity) {
									// solange kein TRUNC auftaucht, werden alle Lemmas dem Ausdruck hinzugefügt
									entities.add(Util.normalizeLemma(tt.getLemma()));

									// sobald ein KON auftaucht, wird die morphemkoordination aufgelöst
									if (ce != null && tt.getPosTag().equals("KON")) {
//										isMorphemCoordination = true;
										List<Token> euTokens = new ArrayList<Token>(ieTokens);
										List<List<Token>> combinations = ce.resolve(completeEntity, euTokens,
												lemmatizer, false);
										// für jede Expansion wird eine InformationEntity erzeugt
										for (List<Token> list : combinations) {
											List<String> lemmata = new ArrayList<String>();
											for (Token currTT : list) {
												lemmata.add(currTT.getLemma());
											}
											ie = new InformationEntity(lemmata.get(0), false);
											ie.setLemmata(lemmata);
											informationEntities.add(ie);
										}
									}
								}

								ie = new InformationEntity(entities.get(0), false, entityPointer);
								ie.setLemmata(entities);

							} else if (completeEntity.size() < 1) { // Entität besteht aus weniger als einem Token
								ie = null;
								continue;
							} else { // Entität besteht aus genau einem Token
								ie = new InformationEntity(completeEntity.get(0).getToken(), true, entityPointer);
							}
							informationEntities.add(ie);

						}

						for (InformationEntity e : informationEntities) {
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
							if (type != IEType.TOOL) {
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
		}
		ieTokens = null;
		if (ce != null)
			log.info(ce.getPossResolvations().size() + " new Compounds");

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
			String lemma = Util.normalizeLemma(lemmata.get(t + skip));
			if (modifiers.containsKey(lemma)) {
				required = -1;
				boolean match = false;
				for (List<String> expression : modifiers.get(lemma)) {
					if (expression.size() > lemmata.size())
						continue;
					for (int s = 0; s < expression.size(); s++) {
						String string = expression.get(s);
						try {
							match = string.equals(Util.normalizeLemma(lemmata.get(t + skip + s)));
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
		e.setLemmata(lemmata);
	}

	/**
	 * merge entities if one fully contains the other (except the larger one
	 * contains 'und' or 'oder')
	 * 
	 * @param extractions map of extractionUnits and extracted entities
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
				// boolean isPartOfOtherIE = false;
				int subList = -1;
				for (int j = 0; j < iesForUnit.size(); j++) {
					if (j == i)
						continue;
					InformationEntity otherIE = iesForUnit.get(j);
					// check if currentIE is sublist of otherIE
					subList = Collections.indexOfSubList(otherIE.getLemmata(), currentIE.getLemmata());
					// boolean isSubList = false;
					// if (subList != -1)
					// isSubList = true;
					// System.out.println(subList);
					// isPartOfOtherIE = Util.containsList(otherIE.getLemmata(),
					// currentIE.getLemmata());
					if (subList != -1) {// if (isPartOfOtherIE) {
						containingIE = otherIE;
						break;
					}
				}
				if (subList == -1) {// if (!isPartOfOtherIE) {
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
	 * @param lemmatizer
	 * @param patternExtractions
	 * @return
	 */
	public Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extractByStringMatch(
			List<ExtractionUnit> extractionUnits, Tool lemmatizer) {
		Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extractions = new HashMap<ExtractionUnit, Map<InformationEntity, List<Pattern>>>();
		List<Pattern> List = new ArrayList<Pattern>();
		for (ExtractionUnit extractionUnit : extractionUnits) {

			List<TextToken> tokens = extractionUnit.getTokenObjects();
			int skip = 0;
			for (int t = 0; t < tokens.size(); t++) {
				if (t + skip >= tokens.size())
					break;
				Token token = tokens.get(t + skip);
				String lemma = Util.normalizeLemma(token.getLemma());
				if (entities.keySet().contains(lemma)) {
					for (InformationEntity ie : entities.get(lemma)) {
						if (ie.isSingleWordEntity()) {
							token.setIEToken(true);
							ie.getLabels();
							InformationEntity newIE = new InformationEntity(ie.getStartLemma(), true, ie.getLabels());
							Map<InformationEntity, List<Pattern>> iesForUnit = extractions.get(extractionUnit);
							if (iesForUnit == null)
								iesForUnit = new HashMap<InformationEntity, List<Pattern>>();
							iesForUnit.put(newIE, List);
							extractions.put(extractionUnit, iesForUnit);

							continue;
						}

						// speichert Token-Objekte, die die InformationEntity enthalten und ihre POS
						// gesondert
						List<Token> completeEntity = new ArrayList<Token>();
						List<String> posList = new ArrayList<String>();

						boolean matches = false;
						for (int c = 0; c < ie.getLemmata().size(); c++) {
							if (tokens.size() <= t + c) {
								matches = false;
								break;
							}
							matches = ie.getLemmata().get(c)
									.equals(Util.normalizeLemma(tokens.get(t + c + skip).getLemma()));
							if (!matches) {
								break;
							}

							TextToken tt = tokens.get(t + c);
							completeEntity.add(tt);
							posList.add(tt.getPosTag());
						}
						if (matches) {
							token.setIEToken(true);
							((TextToken) token).setTokensToCompleteInformationEntity(ie.getLemmata().size() - 1);
//							System.out.println(token);
//							System.out.println(extractionUnit.getSentence());
//							System.out.println(ie.getLemmata());
							InformationEntity newIE = new InformationEntity(ie.getStartLemma(), false);
							newIE.setLemmata(ie.getLemmata());
							Map<InformationEntity, List<Pattern>> iesForUnit = extractions.get(extractionUnit);
							if (iesForUnit == null)
								iesForUnit = new HashMap<InformationEntity, List<Pattern>>();
							iesForUnit.put(newIE, List);
							extractions.put(extractionUnit, iesForUnit);

							// TODO match auf koordinierte Ausdrücke überprüfen
							if (posList.contains("KON"))
								System.out.println(extractionUnit.getSentence() + "\n" + newIE + "\n");
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
			// int skip = 0; //TODO Delete skip
			for (int t = 0; t < tokens.size(); t++) {
				if (t /* + skip */ >= tokens.size())
					break;
				Token currentToken = tokens.get(t /* + skip */);
				String lemma = Util.normalizeLemma(currentToken.getLemma());
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
							sb.append(Util.normalizeLemma(tokens.get(t /* + skip */ + i).getLemma()) + " ");
						}
						longestMatchingModifer = sb.toString().trim();
						modifierLength = ((TextToken) currentToken).getTokensToCompleteModifier();
					}
				}
			}
			if (longestMatchingModifer != null) {
				for (InformationEntity ie : extractionUnits.get(extractionUnit).keySet()) {
					ie.setModifier(longestMatchingModifer);
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
				keyword = Util.normalizeLemma(split[0]);

			} catch (ArrayIndexOutOfBoundsException e) {
				continue;
			}
			Set<InformationEntity> iesForKeyword = this.entities.get(keyword);
			if (iesForKeyword == null)
				iesForKeyword = new HashSet<InformationEntity>();
			InformationEntity ie = new InformationEntity(keyword, split.length == 1);
			if (!ie.isSingleWordEntity()) {
				for (String string : split) {
					ie.addLemma(Util.normalizeLemma(string));
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
				keyword = Util.normalizeLemma(split[0]);
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

	public Map<String, String> getNewCompounds() {
		if (ce == null)
			return new HashMap<String, String>();
		return ce.getPossResolvations();
	}
}
