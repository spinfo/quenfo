package quenfo.de.uni_koeln.spinfo.information_extraction.preprocessing;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import is2.data.SentenceData09;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;

/**
 * @author geduldia
 * 
 *         Annotates ExractionUnits with lexical data using the MateTools
 *
 */
public class MateTagger {

	/**
	 * adds lexical data to the given ExtractionUnits (pos-tags, morph-tags, lemma)
	 * 
	 * @param extractionUnits
	 * @throws IOException
	 */
	public static void setLexicalData(List<ExtractionUnit> extractionUnits, is2.tools.Tool lemmatizer,
			is2.tools.Tool morphTagger, is2.tools.Tool tagger) throws IOException {

		IETokenizer tokenizer = new IETokenizer();
		boolean lexicalDataIsStoredInDB;
		SentenceData09 sd = null;

		for (ExtractionUnit extractionUnit : extractionUnits) {

			lexicalDataIsStoredInDB = true;
			sd = new SentenceData09();
			if (extractionUnit.getTokens() == null) {
				lexicalDataIsStoredInDB = false;
				sd.init(tokenizer.tokenizeSentence("<root> " + extractionUnit.getSentence()));
			} else {

				sd.init(extractionUnit.getTokens());
			}
			if (lemmatizer != null) {
				
				if (extractionUnit.getLemmata() == null) {
					lexicalDataIsStoredInDB = false;
					lemmatizer.apply(sd);
				} else {
					sd.setLemmas(extractionUnit.getLemmata());
				}
			}
			if (morphTagger != null) {
				morphTagger.apply(sd);
			}
			if (tagger != null) {
				if (extractionUnit.getPosTags() == null) {
					lexicalDataIsStoredInDB = false;
					tagger.apply(sd);
				} else {
					sd.setPPos(extractionUnit.getPosTags());
					sd.setLemmas(extractionUnit.getLemmata());
				}

			}
			
			extractionUnit.setSentenceData(sd);
			extractionUnit.setLexicalDataIsStoredInDB(lexicalDataIsStoredInDB);

		}

	}

	public static ExtractionUnit setLexicalData(ExtractionUnit eu, is2.tools.Tool lemmatizer, is2.tools.Tool morphTagger,
			is2.tools.Tool tagger) {
		IETokenizer tokenizer = new IETokenizer();
		SentenceData09 sd = new SentenceData09();

		sd.init(tokenizer.tokenizeSentence("<root> " + eu.getSentence()));

		if (lemmatizer != null)
			lemmatizer.apply(sd);

		if (morphTagger != null)
			morphTagger.apply(sd);

		if (tagger != null)
			tagger.apply(sd);
		

		eu.setTokens(sd.forms);
		eu.setLemmata(sd.plemmas);
		eu.setPosTags(sd.ppos);		
		
		return eu;

	}

	public static String[] getLemmata(String[] tokens, is2.tools.Tool lemmatizer) {
		SentenceData09 sd = new SentenceData09();
		sd.init(tokens);
		
		if (lemmatizer != null) 
			lemmatizer.apply(sd);
		
		
		return sd.plemmas;
	}

}
