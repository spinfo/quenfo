package quenfo.de.uni_koeln.spinfo.information_extraction.applications;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import quenfo.de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import quenfo.de.uni_koeln.spinfo.information_extraction.workflow.IEJobs;

public class CompareAMStoComps {
	
	private static File teiFile = new File("information_extraction/data/competences/tei_index/compdict.tei");
	private static File competences = new File("information_extraction/data/competences/competences.txt");
	

	public static void main(String[] args) throws IOException {
		IEJobs jobs = new IEJobs(competences, null, null, null, null, false);
		Map<String, Set<InformationEntity>> competences = jobs.entities;
		
		Set<String> ams = readTEI();
		
		int containedComps = 0;
		int notContainedComps = 0;
		
		Set<String> notCategorized = new HashSet<>();
		System.out.println("ams Comps: " + ams.size());
		for (Map.Entry<String, Set<InformationEntity>> e : competences.entrySet()) {
			for (InformationEntity ie : e.getValue()) {
				List<String> lemmas = ie.getLemmata();
				String lemma = "";
				for (String l : lemmas) {
					lemma = lemma + " " + l;
				}
				lemma = lemma.trim();
				
				if (ams.contains(lemma))
					containedComps++;
				else {
					notContainedComps++;
					notCategorized.add(lemma);
				}
			}
		}
		
		
		System.out.println(containedComps + " -- " + notContainedComps);
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(new File("information_extraction/data/competences/notCategorized.txt")), "UTF8"));
		for(String c : notCategorized) 
			bw.write(c + "\n");
		bw.close();
	}


	private static Set<String> readTEI() throws IOException {
		
		BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(teiFile), "UTF8"));
		
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
