package quenfo.de.uni_koeln.spinfo.information_extraction.preprocessing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import is2.tools.Tool;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;

/**
 * @author geduldia
 * 
 *         A class to separate ClassifyUnits (~paragraphs) into ExtractionUnits
 *         (~sentences)
 *
 */
public class ExtractionUnitBuilder {

	/**
	 * 
	 * transforms a list of classifyUnits (≈ paragraphs) in a list of
	 * extractionUnits (≈ sentences)
	 * 
	 * @param classifyUnits
	 * @param lemmatizer
	 * @param morphTagger
	 * @param tagger
	 * @return list of initialized extractionUnits
	 * @throws IOException
	 */
	public static List<ExtractionUnit> initializeIEUnits(List<ClassifyUnit> classifyUnits, Tool lemmatizer,
			Tool morphTagger, Tool tagger) throws IOException {
		List<ExtractionUnit> extractionUnits = new ArrayList<ExtractionUnit>();
		IETokenizer tokenizer = new IETokenizer();
		List<String> sentences;
		List<String> lemmata;
		List<String> posTags;
		List<String> tokens;
		ExtractionUnit extractionUnit = null;
		for (ClassifyUnit cu : classifyUnits) {
			sentences = null;
			lemmata = null;
			posTags = null;
			tokens = null;
			if (((JASCClassifyUnit) cu).getSentences() == null) {
				sentences = tokenizer.splitIntoSentences(cu.getContent());
			} else {
				sentences = Arrays.asList(((JASCClassifyUnit) cu).getSentences().split("  \\|\\|  "));

			}
			if (((JASCClassifyUnit) cu).getLemmata() != null) {
				lemmata = Arrays.asList(((JASCClassifyUnit) cu).getLemmata().split("  \\|\\|  "));
			}
			if (((JASCClassifyUnit) cu).getPosTags() != null) {
				posTags = Arrays.asList(((JASCClassifyUnit) cu).getPosTags().split("  \\|\\|  "));
			}
			if (((JASCClassifyUnit) cu).getTokens() != null) {
				tokens = Arrays.asList(((JASCClassifyUnit) cu).getTokens().split("  \\|\\|  "));
			}
			for (int i = 0; i < sentences.size(); i++) {
				String sentence = sentences.get(i);

				sentence = correctSentence(sentence);
				if (sentence.length() > 1) {
					extractionUnit = new ExtractionUnit();

					extractionUnit.setSentence(sentence);
					extractionUnit.setJobAdID(((JASCClassifyUnit) cu).getParentID());
					extractionUnit.setSecondJobAdID(((JASCClassifyUnit) cu).getSecondParentID());
					extractionUnit.setClassifyUnitID(cu.getId());
					extractionUnit.setClassifyUnitTableID(((JASCClassifyUnit) cu).getTableID());
					extractionUnit.setClassifyUnitjpaID(((JASCClassifyUnit) cu).getJpaID());
					extractionUnit.setJobAdID(((JASCClassifyUnit) cu).getParentID());

					if (lemmata != null) {
						extractionUnit.setLemmata(lemmata.get(i).split(" \\| "));
					}
					if (posTags != null) {

						extractionUnit.setPosTags(posTags.get(i).split(" \\| "));
					}
					if (tokens != null) {
						extractionUnit.setTokens(tokens.get(i).split(" \\| "));
					}
//					System.out.println(extractionUnit);
					extractionUnits.add(extractionUnit);
				}
			}
		}
		MateTagger.setLexicalData(extractionUnits, lemmatizer, morphTagger, tagger);
		classifyUnits = null;
		return extractionUnits;
	}

	private static String correctSentence(String sentence) {
		String regex = "\\s([\\,\\.])(\\w..)";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(sentence);
		while (m.find()) {
			if (!m.group(2).toLowerCase().equals("net")) {
				sentence = sentence.replace(m.group(1), m.group(1) + " ");
			}
		}
		regex = "[A-Za-z](\\,|\\;)[A-Za-z]";
		p = Pattern.compile(regex);
		m = p.matcher(sentence);
		while (m.find()) {
			sentence = sentence.replace(m.group(), m.group().substring(0, 2) + " " + m.group().substring(2));
		}
		regex = "(\\s[\\*\\/])(\\w\\w)";
		p = Pattern.compile(regex);
		m = p.matcher(sentence);
		while (m.find()) {
			if (!m.group(2).toLowerCase().equals("in")) {
				sentence = sentence.replace(m.group(1), " ");
			} else {
				sentence = sentence.replace(m.group(1), "/");
			}
		}
		// if (sentence.contains(" & ")) {
		// sentence = sentence.replace(" & ", " und ");
		// }
		if (sentence.contains(" UND ")) {
			sentence = sentence.replace(" UND ", " und ");
		}
		if (sentence.contains(" ODER ")) {
			sentence = sentence.replace(" ODER ", " oder ");
		}
		regex = " und[-|\\/| ][\\/| ]?[ ]?oder ";
		p = Pattern.compile(regex);
		m = p.matcher(sentence);
		while (m.find()) {
			sentence = sentence.replace(m.group(), " oder ");
		}
		regex = " oder[-|\\/| ][\\/| ]?[ ]?und ";
		p = Pattern.compile(regex);
		m = p.matcher(sentence);
		while (m.find()) {
			sentence = sentence.replace(m.group(), " und ");
		}

		return sentence;
	}

}
