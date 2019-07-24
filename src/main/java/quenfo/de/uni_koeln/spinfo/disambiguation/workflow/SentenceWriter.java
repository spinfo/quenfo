package quenfo.de.uni_koeln.spinfo.disambiguation.workflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerModel;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;

public class SentenceWriter {
	
	private SentenceDetector det = null;

	public SentenceWriter(File file) {
		SentenceModel sentModel = null;
		InputStream modelIn = null;
		try {
			modelIn = new FileInputStream(file);
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

		this.det = new SentenceDetectorME(sentModel);
	}
	
	public List<String> createSentences(String content) {
		
		List<String> sentenceList = new ArrayList<String>();
		String[] sentences = det.sentDetect(content);
		for (String s : sentences) {
			s = s.trim();
			if (s.isEmpty())
				continue;
			sentenceList.add(s);
		}
		
		return sentenceList;
	}

	public void writeSentencesToTXT(List<ClassifyUnit> jobAds) throws IOException {

		
		StringBuilder sb = new StringBuilder();
		for (ClassifyUnit cu : jobAds) {
			for(String s : createSentences(cu.getContent())) {
				sb.append("SENTENCE: " + s + "\n");
			}
		}

		FileWriter fw = new FileWriter("raw_sentences.txt");
		fw.write(sb.toString());
		fw.close();

	}

}
