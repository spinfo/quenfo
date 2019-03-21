package quenfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.junit.Test;

import quenfo.de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import quenfo.de.uni_koeln.spinfo.information_extraction.workflow.IEJobs;

public class CompareAMStoTXT {

	private File amsFile = new File("information_extraction/data/competences/tei_index/compdict.tei");
	private File compFile = new File("information_extraction/data/competences/competences.txt");
	private File notCatFile = new File("information_extraction/data/competences/notCategorized.txt");
;
	@Test
	public void test() throws IOException {


		IEJobs jobs = new IEJobs(compFile, null, null, null, null, false, null);
		Map<String, Set<InformationEntity>> competences = jobs.entities;

		Set<String> ams = readAMS();

		int containedComps = 0;
		int notContainedComps = 0;
		
		Set<String> notCategorized = new HashSet<String>();
		
		for (Map.Entry<String, Set<InformationEntity>> e : competences.entrySet()) {
			for (InformationEntity ie : e.getValue()) {
				List<String> lemmaList = ie.getLemmata();
				String lemma = "";
				if (lemmaList.size() == 1)
					lemma = lemmaList.get(0);
				else {
					for (String l : lemmaList)
						lemma = lemma + " " + l;
					lemma = lemma.trim();
				}
				if (ams.contains(lemma))
					containedComps++;
				else {
					notContainedComps++;
					notCategorized.add(lemma);
				}
					
			}
		}
		System.out.println("Contained: " + containedComps + " Not Contained: " + notContainedComps);
		exportWordList(notCategorized);
	}



	private void exportWordList(Set<String> notCategorized) throws IOException {
		OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(notCatFile), "UTF8");
		for (String s : notCategorized) {
			osw.append(s + "\n");
		}
		osw.close();
	}



	private Set<String> readAMS() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(amsFile), "UTF8"));

		StringBuilder sb = new StringBuilder();
		String line = "";
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		br.close();
		String teiString = sb.toString();

		Document doc = Jsoup.parse(teiString, "", Parser.xmlParser());

		Set<String> amsComps = new HashSet<String>();

		Elements orthElements = doc.select("orth");
		for (Element orthElement : orthElements) {
			amsComps.add(orthElement.text());
		}

		return amsComps;
	}

}
