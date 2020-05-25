package quenfo.de.uni_koeln.spinfo.information_extraction.preprocessing;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import quenfo.de.uni_koeln.spinfo.classification.jasc.preprocessing.ClassifyUnitSplitter;

/**
 * @author geduldia
 *  A Tokenizer using openNLP-models
 *  Includes methods to split text into sentences and sentences into tokens
 *
 */
public class IETokenizer {

	private TokenizerModel tokenizeModel;
	private SentenceModel sentenceModel;

	public IETokenizer() {
		
		setTokenizeModel(ClassLoader.getSystemResourceAsStream("nlp/openNLPmodels/de-token.bin"));
		// TODO JB: custom sentence model
		setSentenceSplittingModel(ClassLoader.getSystemResourceAsStream("nlp/openNLPmodels/de-sent.bin"));
	}
	
	/**
	 * Splits a sentence into tokens.
	 * 
	 * @param sentence
	 *            String to be tokenized, should be result of sentence splitting
	 * @return Array of tokens
	 */
	public String[] tokenizeSentence(String sentence) {
		String tokens[] = null;
		Tokenizer tokenizer = new TokenizerME(tokenizeModel);
		tokens = tokenizer.tokenize(sentence);
		return tokens;
	}
	
	/**splits text into sentences
	 * @param text
	 * @return list of sentences
	 */
	public List<String> splitIntoSentences(String text) {
		String[] sentences = null;

		SentenceDetectorME detector = new SentenceDetectorME(sentenceModel);
		sentences = detector.sentDetect(text);
		List<String> toReturn = new ArrayList<String>();
		for (String string : sentences) {
			List<String> splitted = splitListItems(string);

			toReturn.addAll(splitted);

		}
		return toReturn;
	}

	private void setSentenceSplittingModel(InputStream modelIn) {
//		InputStream modelIn = null;
		try {
//			modelIn = new FileInputStream(model);
			sentenceModel = new SentenceModel(modelIn);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (modelIn != null) {
				try {
					modelIn.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void setTokenizeModel(InputStream modelIn) {
//		InputStream modelIn = null;
		try {
//			modelIn = new FileInputStream(model);
			tokenizeModel = new TokenizerModel(modelIn);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (modelIn != null) {
				try {
					modelIn.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private List<String> splitListItems(String text) {
		List<String> toReturn = new ArrayList<String>();
		List<String> splitted = ClassifyUnitSplitter.splitAtNewLine(text);
		Pattern pattern = Pattern.compile("(^[0-9](\\.)?(\\))?)|^(-|\\+|¿)");
		for (String string : splitted) {
			string = string.trim();
			Matcher m = pattern.matcher(string);
			if (m.find()) {
				string = string.substring(m.end());
				string = string.trim();
			}

			if (string.length() > 0 && !ClassifyUnitSplitter.containsOnlyNonWordChars(string)) {
				toReturn.add(string);
			}
		}
		return toReturn;
	}




}
