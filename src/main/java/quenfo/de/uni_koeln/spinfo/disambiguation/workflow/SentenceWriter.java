package quenfo.de.uni_koeln.spinfo.disambiguation.workflow;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerModel;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;

public class SentenceWriter {

	public static void writeSentences(List<ClassifyUnit> jobAds) throws IOException {

		SentenceModel sentModel = null;
		InputStream modelIn = null;
		try {
			modelIn = new FileInputStream("information_extraction/data/openNLPmodels/de-sent.bin");
			sentModel = new SentenceModel(modelIn);
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

		SentenceDetector det = new SentenceDetectorME(sentModel);
		StringBuilder sb = new StringBuilder();
		for (ClassifyUnit cu : jobAds) {
			String[] sentences = det.sentDetect(cu.getContent());
			for (String s : sentences) {
				s = s.trim();
				if (s.isEmpty())
					continue;
				sb.append("SENTENCE: " + s + "\n");
			}
		}

		FileWriter fw = new FileWriter("raw_sentences.txt");
		fw.write(sb.toString());
		fw.close();

	}

}
