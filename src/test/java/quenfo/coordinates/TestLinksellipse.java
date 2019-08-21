package quenfo.coordinates;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.uni_koeln.spinfo.data.Token;
import de.uni_koeln.spinfo.workflow.CoordinateExpander;
import is2.lemmatizer.Lemmatizer;
import is2.tag.Tagger;
import is2.tools.Tool;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.preprocessing.MateTagger;

public class TestLinksellipse {

	@Test
	public void test() {
		List<String> testSentences = new ArrayList<String>();
		testSentences.add("Warenannahme und -lagerung kontrollieren");
		testSentences.add("Zahnärzte und -ärztinnen gesucht");
		testSentences.add("Warenannahme, -lagerung und -kontrolle");
		testSentences.add("Fehlersuche und -behebung");
		testSentences.add("SAP-Kenntnisse und -Erfahrungen");
		
//		CoordinationResolver cr = new CoordinationResolver();
		CoordinateExpander ce = new CoordinateExpander(new File("src/test/resources/coordinations/resolvedCompounds.txt"),
				new File("src/test/resources/coordinations/splittedCompounds.txt"));
		Tool lemmatizer = new Lemmatizer(
				"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model",
				false);
		Tool tagger = new Tagger(
				"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/tag-ger-3.6.model");
		
		for(String s : testSentences) {
			
			ExtractionUnit eu = MateTagger.setLexicalData(new ExtractionUnit(s), lemmatizer, null, tagger);
			
			String[] tokens = eu.getTokens();
			String[] lemmas = eu.getLemmata();
			String[] pos = eu.getPosTags();

			List<Token> completeEntity = new ArrayList<Token>();

			Token token = null;

			for (int i = 0; i < tokens.length; i++) {

				// entfernt mögliche "*+ usw, um Abgleich mit "Ideal"-Lemma zu gewährleisten
				lemmas[i] = lemmas[i].replaceAll("[^A-Za-zäÄüÜöÖß-]", "");

				if (pos == null) {
					token = new Token(tokens[i], lemmas[i], null);
				} else {
					token = new Token(tokens[i], lemmas[i], pos[i]);
				}
				
				completeEntity.add(token);

			}
			
			List<List<Token>> result = ce.resolve(completeEntity, lemmatizer);
			for(List<Token> r : result) {
				System.out.println(r);
			}
			System.out.println("--------------");
		}
	}

}
