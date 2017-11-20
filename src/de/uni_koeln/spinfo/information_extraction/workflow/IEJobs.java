package de.uni_koeln.spinfo.information_extraction.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uni_koeln.spinfo.information_extraction.data.Context;
import de.uni_koeln.spinfo.information_extraction.data.ContextToken;
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.data.IEType;
import de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import de.uni_koeln.spinfo.information_extraction.data.Token;
import de.uni_koeln.spinfo.information_extraction.data.TextToken;

public class IEJobs {

	private Map<String, Set<List<String>>> negExamples;
	// list of importance terms
	private Map<String, Set<List<String>>> importanceTerms;
	// list of extracted entities
	public Map<String, Set<InformationEntity>> entities;

	IEType type;

	public int knownEntities;

	public List<Context> contextPatterns;
	
	public IEJobs(File competences, File noCompetences, File importanceTerms, File contextPatterns, IEType type) throws IOException{
		this.type = type;
		initialize(competences, noCompetences, importanceTerms, contextPatterns);
	}

	public IEJobs(File tools, File noTools, File contextPatterns, IEType type) throws IOException {
		this.type = type;
		initialize(tools, noTools, contextPatterns);
	}

	private void initialize(File tools, File noTools, File contextPatterns) throws IOException {
		initialize(tools, noTools, null, contextPatterns);
	}

	public IEJobs(IEType type) {
		this.type = type;
	}
	
	/**
	 * annotate Tokens as entity, noEntity or importanceTerm
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
			if(!negExamples.isEmpty()){
				annotateNegativeExamples(tokens);
			}
			if (importanceTerms != null) {
				annotateImportances(tokens);
			}
		}
	}
	
	/**
	 * extracts entities by contextPatterns
	 * @param extractionUnits
	 * @return
	 */
	public Map<ExtractionUnit, Map<InformationEntity, List<Context>>> extractByPatterns(
			List<ExtractionUnit> extractionUnits) {
		return extractByPatterns(extractionUnits, false);
	}

	/**
	 * extracts entities by contextPatterns
	 * @param extractionUnits
	 * @param unsupervised directly annotates matching tokens as entity
	 * @return
	 */
	public Map<ExtractionUnit, Map<InformationEntity, List<Context>>> extractByPatterns(
			List<ExtractionUnit> extractionUnits, boolean unsupervised) {
		Map<ExtractionUnit, Map<InformationEntity, List<Context>>> toReturn = new HashMap<ExtractionUnit, Map<InformationEntity, List<Context>>>();
		for (ExtractionUnit extractionUnit : extractionUnits) {
			List<TextToken> tokens = extractionUnit.getTokenObjects();
			for (Context context : contextPatterns) {
				for (int t = 0; t <= tokens.size() - context.getSize(); t++) {
					boolean match = false;
					int euTokenIndex = 0;
					int requiredForImportance = 0;
					int requiredForEntity = 0;
					for (int c = 0; c < context.getSize(); c++) {
						if (c >= context.getSize()) {
							break;
						}
						int i = t + requiredForImportance  + requiredForEntity;
						if (i + c >= tokens.size())
							continue;
						Token token = tokens.get(i + c);
						Token contextToken = context.getTokenAt(c);
						match = ((TextToken) token).isEqualsContextToken((ContextToken) contextToken);
						if (!match) {
							break;
						}
						if (context.getExtractionPointer().get(0) == c) {
							euTokenIndex = i + c;
						}
						if (contextToken.isInformationEntity()) {
							requiredForEntity = ((TextToken) token).getTokensToCompleteInformationEntity();
						}
						if (contextToken.isImportanceTerm()) {
							requiredForImportance = ((TextToken) token).getTokensToCompleteImportance();
						}
					}
					if (match) {
						TextToken euToken = tokens.get(euTokenIndex);
							int ie_size = context.getExtractionPointer().size();
							InformationEntity e;
							if (ie_size == 1) {
								if (normalizeLemma(euToken.getLemma()).length() > 1 && !(euToken.isNoEntity())) {
									e = new InformationEntity(normalizeLemma(euToken.getLemma()), true);
								} else {
									e = null;
									continue;
								}
							} else {
								e = new InformationEntity(normalizeLemma(euToken.getLemma()), false);
								List<String> expression = new ArrayList<String>();
								for (int s = 0; s < context.getExtractionPointer().size(); s++) {
									String ex = normalizeLemma(tokens.get(euTokenIndex + s).getLemma());
									if (!ex.trim().equals("") && !ex.trim().equals("--")) {
										expression.add(normalizeLemma(tokens.get(euTokenIndex + s).getLemma()));
									}
								}
								if (expression.size() > 1) {
									e.setExpression(expression);
								} else if (expression.size() < 1) {
									e = null;
									continue;
								} 
								else {
									e = new InformationEntity(normalizeLemma(euToken.getLemma()), true);
								}
							}
							//check if full expression is noEntity
							boolean isNoEntity = false;
							if(negExamples.containsKey(e.getToken())){
								Set<List<String>> set = negExamples.get(e.getToken());
								if(set.contains(e.getTokens())){
									isNoEntity = true;
								}
							}
							if(isNoEntity){
								e = null; continue;
							}
							if (unsupervised) {
								euToken.setInformationEntity(true);
								((TextToken) euToken).setTokensToCompleteInformationEntity(ie_size - 1);

							}
							Map<InformationEntity, List<Context>> map = toReturn.get(extractionUnit);
							if (map == null)
								map = new HashMap<InformationEntity, List<Context>>();
							List<Context> list = map.get(e);
							if (list == null)
								list = new ArrayList<Context>();
							list.add(context);
							e.addContextID(context.getId());
							map.put(e, list);
							toReturn.put(extractionUnit, map);
					}
				}
			}
		}
		return toReturn;
	}
	
	/**
	 * merge entities if one fully contains the other (except the larger one contains 'und' or 'oder')
	 * 
	 * @param extractions
	 *            map of extractionUnits and extracted entities
	 * @return merged extractions
	 */
	public Map<ExtractionUnit, Map<InformationEntity, List<Context>>> mergeInformationEntities(
			Map<ExtractionUnit, Map<InformationEntity, List<Context>>> extractions) {
		for (ExtractionUnit ieunit : extractions.keySet()) {
			Map<InformationEntity, List<Context>> merged = new HashMap<InformationEntity, List<Context>>();
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
					isPartOfOtherIE = containsList(otherIE.getTokens(), currentIE.getTokens());
					if (isPartOfOtherIE) {
						containingIE = otherIE;
						break;
					}
				}
				if (!isPartOfOtherIE) {
					merged.put(currentIE, extractions.get(ieunit).get(currentIE));
				} else {
					if (containingIE.getTokens().contains("und") || containingIE.getTokens().contains("oder")) {
						if(!currentIE.getTokens().contains("und")&& !(currentIE.getTokens().contains("oder"))){
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
	 * updates the list of all entities with the given new extractions
	 * @param extractions
	 */
	public void updateEntitiesList(Map<ExtractionUnit, Map<InformationEntity, List<Context>>> extractions) {
		for (ExtractionUnit ieUnit : extractions.keySet()) {
			Set<InformationEntity> ies = extractions.get(ieUnit).keySet();
			Set<InformationEntity> emptyIEs = new HashSet<InformationEntity>();
			for (InformationEntity ie : ies) {
				if (!containsLetter(ie.getFullExpression())) {
					emptyIEs.add(ie);
					continue;
				}
				Set<InformationEntity> set = entities.get(ie.getToken());
				if (set == null) {
					set = new HashSet<InformationEntity>();
				}
				boolean isNew = set.add(ie);
				if (isNew) {
					knownEntities++;
				}
				entities.put(ie.getToken(), set);
			}
			ies.removeAll(emptyIEs);
		}
	}
	
	/**
	 * 
	 * Find Competences in unknown Context via Stringmatch with the Entities-List
	 * only returns new matches
	 * @param extractionUnits
	 * @param patternExtractions
	 * @return
	 */
	public Map<ExtractionUnit, Map<InformationEntity, List<Context>>> extractByStringMatch(
			List<ExtractionUnit> extractionUnits,
			Map<ExtractionUnit, Map<InformationEntity, List<Context>>> patternExtractions) {
		Map<ExtractionUnit, Map<InformationEntity, List<Context>>> extractions = new HashMap<ExtractionUnit, Map<InformationEntity, List<Context>>>();
		List<Context> contextList = new ArrayList<Context>();
		for (ExtractionUnit extractionUnit : extractionUnits) {
			List<TextToken> tokens = extractionUnit.getTokenObjects();
			int skip = 0;
			for (int t = 0; t < tokens.size(); t++) {
				if (t + skip >= tokens.size())
					break;
				Token token = tokens.get(t + skip);
				if (token.isInformationEntity()) {
					skip += ((TextToken) token).getTokensToCompleteInformationEntity();
				}
				String lemma = normalizeLemma(token.getLemma());
				// check if current token is posExample
				if (entities.keySet().contains(lemma)) {
					for (InformationEntity ie : entities.get(lemma)) {
						if (ie.isComplete()) {
							token.setInformationEntity(true);
							if (patternExtractions.get(extractionUnit) == null
									|| !(patternExtractions.get(extractionUnit).containsKey(ie))) {

								InformationEntity newIE = new InformationEntity(ie.getToken(), true);
								Map<InformationEntity, List<Context>> iesForUnit = extractions.get(extractionUnit);
								if (iesForUnit == null)
									iesForUnit = new HashMap<InformationEntity, List<Context>>();
								iesForUnit.put(newIE, contextList);
								extractions.put(extractionUnit, iesForUnit);
							}
							continue;
						}
						boolean matches = false;
						for (int c = 1; c < ie.getTokens().size(); c++) {
							if (tokens.size() <= t + c) {
								matches = false;
								break;
							}
							matches = ie.getTokens().get(c).equals(normalizeLemma(tokens.get(t + c).getLemma()));
							if (!matches) {
								break;
							}
						}
						// Token is start of entity
						if (matches) {
							token.setInformationEntity(true);
							((TextToken) token).setTokensToCompleteInformationEntity(ie.getTokens().size() - 1);
							if (patternExtractions.get(extractionUnit) == null
									|| !(patternExtractions.get(extractionUnit).containsKey(ie))) {
								InformationEntity newIE = new InformationEntity(ie.getToken(), false);
								newIE.setExpression(ie.getTokens());
								// newIE.setKnowledge(ie.isKnowledge());
								Map<InformationEntity, List<Context>> iesForUnit = extractions.get(extractionUnit);
								if (iesForUnit == null)
									iesForUnit = new HashMap<InformationEntity, List<Context>>();
								iesForUnit.put(newIE, contextList);
								extractions.put(extractionUnit, iesForUnit);
							}
						}
					}
				}
			}
		}
		return extractions;
	}
	
	/**
	 * set importance-attributes
	 * @param extractionUnits
	 */
	public void setImportances(Map<ExtractionUnit, Map<InformationEntity, List<Context>>> extractionUnits) {
		for (ExtractionUnit extractionUnit : extractionUnits.keySet()) {
			String longestImportance = null;
			int importanceLength = 0;
			List<TextToken> tokens = extractionUnit.getTokenObjects();
			int skip = 0;
			for (int t = 0; t < tokens.size(); t++) {
				if (t + skip >= tokens.size())
					break;
				Token token = tokens.get(t + skip);
				String lemma = normalizeLemma(token.getLemma());
				// check if token is (start of) importance expression
				if (token.isImportanceTerm() && ((TextToken) token).getTokensToCompleteImportance() == 0) {
					if (longestImportance == null) {
						longestImportance = lemma;
						importanceLength = 1;
					}
				}
				if (token.isImportanceTerm() && ((TextToken) token).getTokensToCompleteImportance() > 0) {
					if (((TextToken) token).getTokensToCompleteImportance() > importanceLength) {
						StringBuffer sb = new StringBuffer();
						for (int i = 0; i < ((TextToken) token).getTokensToCompleteImportance() + 1; i++) {
							sb.append(normalizeLemma(tokens.get(t + skip + i).getLemma()) + " ");
						}
						longestImportance = sb.toString().trim();
						importanceLength = ((TextToken) token).getTokensToCompleteImportance();
					}
				}
			}
			if (longestImportance != null) {
				for (InformationEntity ie : extractionUnits.get(extractionUnit).keySet()) {
					ie.setImportance(longestImportance);
				}
			}
		}
	}
	
	
	/**
	 * reads entities from a textfile and adds them to the entities-List
	 * @param entitiesFile
	 * @throws IOException
	 */
	public void readKnownEntitiesFromFile(File entitiesFile) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(entitiesFile));
		String line = in.readLine();
		while (line != null) {
			if(line.equals("")) {
				line = in.readLine();
				continue;
			}
			// create inform.-entity and add to posExample list
			String[] split = line.split(" ");
			String keyword;
			// boolean isKnowledge = false;
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
			if (!ie.isComplete()) {
				for (String string : split) {
					ie.addToExpression(normalizeLemma(string));

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

	/**
	 * adds the given Set of Strings to the entities-list
	 * @param entities
	 * @throws IOException
	 */
	public void addKnownEntities(Set<String> entities) throws IOException {
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
			if (!ie.isComplete()) {
				for (String string : split) {
					ie.addToExpression(normalizeLemma(string));

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
	 * @param noEntities
	 */
	public void addNoEntities(Set<String> noEntities){
		
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



	//read lists and patterns
	private void initialize(File knownEntities, File negativeEntities, File importanceTermsFile,
			File contextPatternsFile) throws IOException {
		entities = new HashMap<String, Set<InformationEntity>>();
		this.knownEntities = 0;
		if(knownEntities != null){
			readKnownEntitiesFromFile(knownEntities);
		}
		negExamples = new HashMap<String, Set<List<String>>>();
		importanceTerms = new HashMap<String, Set<List<String>>>();
		if (negativeEntities != null) {
			readWordList(negativeEntities, negExamples);
		}
		if (importanceTermsFile != null) {
			readWordList(importanceTermsFile, importanceTerms);
		}
		contextPatterns = new ArrayList<Context>();
		if(contextPatternsFile!= null){
			readContextPatterns(contextPatterns, contextPatternsFile);
		}
		
	}

	private void readWordList(File inputFile, Map<String, Set<List<String>>> map) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(inputFile));
		String line = in.readLine();
		while (line != null) {
			if(line.equals("")){
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
	 * read contexts/templates for entities from context-file
	 * 
	 * @param contextFile
	 *            file of contexts in a specific context format
	 * @return a list of contexts
	 * @throws IOException
	 */
	private void readContextPatterns(List<Context> contexts, File contextFile) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(contextFile));
		String line = in.readLine();
		Context context = new Context();
		int lineNumber = 0;
		while (line != null) {
			lineNumber++;
			String[] split = line.split("\t");
			// set id
			try {
				if (line.startsWith("ID:")) {
					context.setId(Integer.parseInt(split[1]));
				}
				// set name
				if (line.startsWith("NAME:")) {
					context.setDescription(split[1].trim());
				}
				// set Token
				if (line.startsWith("TOKEN:") || line.startsWith("[TOKEN:]")) {
					String string = split[1];
					if (string.equals("null"))
						string = null;
					String lemma = split[2];
					if (lemma.equals("null"))
						lemma = null;
					String posTag = split[3];
					if (posTag.equals("null"))
						posTag = null;
					Token token = new ContextToken(string, lemma, posTag, Boolean.parseBoolean(split[4]));
					if (lemma != null && lemma.toUpperCase().equals("IMPORTANCE")) {
						token.setImportanceTerm(true);
					}
					context.addToken((ContextToken) token);
				}
				// set extraction indices
				if (line.startsWith("EXTRACT:")) {
					List<Integer> euPointer = new ArrayList<Integer>();
					String[] ints = split[1].split(",");
					for (String string : ints) {
						euPointer.add(Integer.parseInt(string));
					}
					context.setEUPointer(euPointer);
					contexts.add(context);
					context = new Context();
				}
			} catch (Exception e) {
				System.out.println("Error in context file (line " + lineNumber + ")");
			}
			line = in.readLine();
		}
		in.close();
	}



	private void annotateEntities(List<TextToken> tokens) {
		for (int t = 0; t < tokens.size(); t++) {
			Token token = tokens.get(t);
			String lemma = normalizeLemma(token.getLemma());

			// check if current token is posExample
			if (entities.keySet().contains(lemma)) {
				for (InformationEntity ie : entities.get(lemma)) {
					if (ie.isComplete()) {
						// token is complete entity
						token.setInformationEntity(true);
						continue;
					}
					boolean matches = false;
					for (int c = 1; c < ie.getTokens().size(); c++) {
						if (tokens.size() <= t + c) {
							matches = false;
							break;
						}
						matches = ie.getTokens().get(c).equals(normalizeLemma(tokens.get(t + c).getLemma()));
						if (!matches) {
							break;
						}
					}
					// Token is start of entity
					if (matches) {
						token.setInformationEntity(true);
						((TextToken) token).setTokensToCompleteInformationEntity(ie.getTokens().size() - 1);
					}
				}
			}
		}
	}

	private void annotateNegativeExamples(List<TextToken> tokens) {
		if (negExamples == null)
			return;
		for (int t = 0; t < tokens.size(); t++) {
			Token token = tokens.get(t);
			String lemma = normalizeLemma(token.getLemma());
			// check if current token is negExample
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
					((TextToken) token).setNoEntity(true);
				}
			}
		}
	}

	private void annotateImportances(List<TextToken> tokens) {
		int skip = 0;
		for (int t = 0; t < tokens.size(); t++) {
			if (t + skip >= tokens.size())
				break;
			Token token = tokens.get(t + skip);
			String lemma = normalizeLemma(token.getLemma());

			// check if token is importance
			if (importanceTerms.keySet().contains(lemma)) {
				int required = -1;
				boolean match = false;
				for (List<String> expression : importanceTerms.get(lemma)) {
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
					if (match) {
						if (expression.size() > required) {
							required = expression.size() - 1;
						}
					}
				}
				// token is start of importance
				token.setImportanceTerm(true);
				((TextToken) token).setTokensToCompleteImportance(required);
				skip += required;
			}
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
		lemma = lemma.trim();
		if (lemma.equals("--")) {
			return lemma;
		}
		if (lemma.startsWith("<")) {
			return lemma;
		}
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
			}
			if (lemma.length() == 0) {
				break;
			}
			Character e = lemma.charAt(lemma.length() - 1);
			if (e == '_') {
				lemma = lemma.substring(0, lemma.length() - 1);
				lemma = lemma.trim();
			}
			if (lemma.length() == 0) {
				break;
			}
			if (!Character.isLetter(e) && !Character.isDigit(e) && !(e == '+') && !(e == '#')) {
				lemma = lemma.substring(0, lemma.length() - 1);
				lemma = lemma.trim();
			} else {
				break;
			}
		}
		return lemma.trim();
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



	private boolean containsLetter(String string) {
		Pattern p = Pattern.compile("[A_Z]|[a-z]");
		Matcher m = p.matcher(string);
		return m.find();
	}

	

}
