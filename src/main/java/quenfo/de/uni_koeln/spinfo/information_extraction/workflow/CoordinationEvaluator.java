package quenfo.de.uni_koeln.spinfo.information_extraction.workflow;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.data.Token;
import de.uni_koeln.spinfo.workflow.CoordinateExpander;
import is2.lemmatizer.Lemmatizer;
import is2.tag.Tagger;
import is2.tools.Tool;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.Pattern;
import quenfo.de.uni_koeln.spinfo.information_extraction.db_io.IE_DBConnector;
import quenfo.de.uni_koeln.spinfo.information_extraction.preprocessing.MateTagger;

public class CoordinationEvaluator {

	public void evaluate(IEType type, Connection inputConnection, Connection outputConnection, int startPos,
			int maxCount) throws SQLException {

		Tool lemmatizer = new Lemmatizer(
				"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model",
				false);
		Tool tagger = new Tagger(
				"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/tag-ger-3.6.model");

		Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> ies = IE_DBConnector
				.readGoldstandard(inputConnection, type, startPos, maxCount);

		Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> resolved = new HashMap<ExtractionUnit, Map<InformationEntity, List<Pattern>>>();

		ExtractionUnit eu = null;
		Map<InformationEntity, List<Pattern>> ieMap = null;
		InformationEntity ie = null;
		List<Pattern> pattern = null;

//		CoordinationResolver cr = new CoordinationResolver();
		CoordinateExpander ce = new CoordinateExpander(new File("src/test/resources/coordinations/possibleCompounds.txt"));

		for (Map.Entry<ExtractionUnit, Map<InformationEntity, List<Pattern>>> e : ies.entrySet()) {
			eu = e.getKey();
			ieMap = e.getValue();

			Map<InformationEntity, List<Pattern>> resolvedIEs = new HashMap<InformationEntity, List<Pattern>>();

			for (Map.Entry<InformationEntity, List<Pattern>> f : ieMap.entrySet()) {
				ie = f.getKey();
				pattern = f.getValue();
				resolvedIEs.put(ie, pattern); // unaufgelöste IE hinzufügen

				int firstIndex = ie.getFirstIndex();

				// Satz vorverarbeiten und linguistische Infos zu IE sammeln
				eu = MateTagger.setLexicalData(eu, lemmatizer, null, tagger);
				String[] tokens = eu.getTokens();
				String[] lemmas = eu.getLemmata();
				String[] pos = eu.getPosTags();

//				List<TextToken> completeEntity = new ArrayList<TextToken>();
//				List<TextToken> extractionUnit = new ArrayList<TextToken>();

//				TextToken token = null;
				
				List<Token> completeEntity = new ArrayList<Token>();
				List<Token> extractionUnit = new ArrayList<Token>();
				
				Token token = null;
				boolean finished = false;
				for (int i = 0; i < tokens.length; i++) {

					// entfernt mögliche "*+ usw, um Abgleich mit "Ideal"-Lemma zu gewährleisten
					lemmas[i] = lemmas[i].replaceAll("[^A-Za-zäÄüÜöÖß-]", "");

					if (pos == null) {
						token = new Token(tokens[i], lemmas[i], null);
//						token = new TextToken(tokens[i], lemmas[i], null);
					} else {
						token = new Token(tokens[i], lemmas[i], pos[i]);
//						token = new TextToken(tokens[i], lemmas[i], pos[i]);
					}

					extractionUnit.add(token);
					if (i >= firstIndex && !finished) {
						completeEntity.add(token);
					}
					if (lemmas[i].equals(ie.getLemmata().get(ie.getLemmata().size() - 1)))
						finished = true;
				}

				// Morphemkoordinationen auflösen und mit Goldstandard vergleichen
				List<String> goldResults = ie.getCoordinations();
				if(Arrays.asList(pos).contains("KON")) {
					List<List<Token>> resultTextToken = ce.resolve(completeEntity, extractionUnit, lemmatizer, false);
					List<String[]> result = new ArrayList<String[]>();
					
					for(List<Token> ttList : resultTextToken) {
						String[] coordination = new String[ttList.size()];
						for(int i = 0; i < ttList.size(); i++) {
							coordination[i] = ttList.get(i).getLemma();
						}
					}
					
					// falls Vergleich fehlschlägt, erneut im "Debug"-Modus auflösen
					// (Details zum Auflösungsprozess in der Konsole)
					boolean identical = compare(goldResults, result);
					if (!identical) {
						System.out.println("-----\nGold Result: " + goldResults + "\nResult");
						for (String[] r : result) {
							System.out.println(Arrays.asList(r));
						}
						ce.resolve(completeEntity, extractionUnit, lemmatizer, true);

						System.out.println("-----");
					}

					// für jede Koordinationsauflösung eine IE bilden
					for (String[] r : result) {
						InformationEntity currIE = null;
						boolean isSingleWordEntity = true;
						List<String> rLemmas = Arrays.asList(r);
						System.out.println(rLemmas);
						if (rLemmas.size() > 1) {
							isSingleWordEntity = false;
						}
						currIE = new InformationEntity(rLemmas.get(0), isSingleWordEntity);
						currIE.setLemmata(rLemmas);
						resolvedIEs.put(currIE, pattern);
					}
				}
	
			}
			resolved.put(eu, resolvedIEs);

		}

		// Ausgabe in Output-DB
		IE_DBConnector.createExtractionOutputTable(outputConnection, type, false);
		if (type.equals(IEType.COMPETENCE))
			IE_DBConnector.writeCompetenceExtractions(resolved, outputConnection, false, false);
		else
			IE_DBConnector.writeToolExtractions(resolved, outputConnection, false, false);

	}

	/**
	 * compares gold standard and resolved coordinations
	 * 
	 * @param goldResults
	 * @param result
	 * @return
	 */
	private boolean compare(List<String> goldResults, List<String[]> result) {

		List<String> fns = new ArrayList<String>(goldResults);
		List<String> fps = new ArrayList<String>();

		for (String[] r : result) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < r.length; i++) {
				sb.append(r[i] + " ");
			}
			String s = sb.substring(0, sb.length() - 1);
			if (!fns.contains(s)) {
				fps.add(s);
			} else {
				fns.remove(s);
			}
		}
		if (fps.isEmpty() && fns.isEmpty())
			return true;
		else
			return false;
	}

}
