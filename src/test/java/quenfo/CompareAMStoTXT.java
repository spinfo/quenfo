package quenfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.junit.Test;

import com.opencsv.CSVReader;

import de.uni_koeln.spinfo.preprocessing.MateTagger;
import is2.lemmatizer.Lemmatizer;
import is2.tools.Tool;
import quenfo.de.uni_koeln.spinfo.categorization.data.Entity;
import quenfo.de.uni_koeln.spinfo.categorization.data.Pair;
import quenfo.de.uni_koeln.spinfo.categorization.db_io.Cat_DBConnector;
import quenfo.de.uni_koeln.spinfo.categorization.workflow.SimilarityCalculator;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import quenfo.de.uni_koeln.spinfo.information_extraction.workflow.IEJobs;

public class CompareAMStoTXT {

	private File amsFile = new File("information_extraction/data/competences/tei_index/compdict.tei");
	private File escoFile = new File("information_extraction/data/competences/skills_de.csv");
	private File compFile = new File("information_extraction/data/competences/competences.txt");
	private File notCatFile = new File("information_extraction/data/competences/notCategorized.txt");;

	@Test
	public void test() throws IOException, ClassNotFoundException, SQLException {

		Set<String> jobsComp = readComps();
		Set<String> ams = readAMS();
		Set<String> esco = readESCO();

		exportWordList(esco, "esco.txt");

		System.out.println(jobsComp.size() + " -- " + ams.size() + " -- " + esco.size());

		Set<String> compsAms = new HashSet<String>(jobsComp);
		compsAms.retainAll(ams);
		System.out.println("Intersection Comps & AMS: " + compsAms.size());

		Set<String> compsEsco = new HashSet<String>(jobsComp);
		compsEsco.retainAll(esco);
		System.out.println("Intersection Comps & ESCO: " + compsEsco.size());

		Set<String> escoAms = new HashSet<String>(esco);
		escoAms.retainAll(ams);
		System.out.println("Intersection ESCO & AMS: " + escoAms.size());

		computeSimilarities(jobsComp, esco);

	}

	private void computeSimilarities(Set<String> set1, Set<String> set2) throws SQLException, ClassNotFoundException {

		Connection connection = Cat_DBConnector.connect("C:/sqlite/categorization/competences/ESCOSimilarity.db");
		Cat_DBConnector.createPairsTable(connection, IEType.COMPETENCE);

		double minSim = 1.0;

		SimilarityCalculator sc = new SimilarityCalculator();

		Map<Entity, Double> scoresByEntity = new HashMap<Entity, Double>();
		Map<Pair, Double> pairs = new HashMap<Pair, Double>();

		int i = 0;
		for (String l1 : set1) {
//				List<String> lemmatas = ie.getLemmata();

			if (( i % 100) == 0)
				System.out.println(i++ + " " + l1);
			Entity e1 = new Entity(l1);

			for (String l2 : set2) {
				Entity e2 = new Entity(l2);

				double nw = sc.needlemanWunschSimilarity(l1, l2);
				
				if (nw <= minSim * (l1.length() + l2.length())) {
					continue;
				}
//					System.out.println(nw);

				Pair pair = new Pair(e1, e2);
				pair.setScore(nw);
				pairs.put(pair, nw);
				Double d = scoresByEntity.get(e1);
				if (d == null)
					d = 0.0;
				if (d < pair.getScore()) {
					scoresByEntity.put(e1, pair.getScore());
				}
				d = scoresByEntity.get(e2);
				if (d == null)
					d = 0.0;
				if (d < pair.getScore()) {
					scoresByEntity.put(e2, pair.getScore());
				}
				int pairsPerRound = 1000;
//					System.out.println(pairs.size());
				if (pairs.size() >= pairsPerRound) {
					System.out.println("write " + pairs.size() + " pairs");
					Cat_DBConnector.writePairs(connection, pairs.keySet(), IEType.COMPETENCE, "Pairs");
					pairs.clear();
				}
			}
		}

	}

	private Set<String> readESCO() throws IOException {
		CSVReader reader = new CSVReader(new FileReader(escoFile));

		String[] line = null;

		// skills und synonyme extrahieren
		List<String> esco = new ArrayList<String>();
		while ((line = reader.readNext()) != null) {
			String preffered = line[4];
			String[] syns = line[5].split("\\n");
			esco.add(preffered);
			if (syns != null)
				for (int i = 0; i < syns.length; i++) {
					esco.add(syns[i].toLowerCase());
				}

		}

		// alle gefundenen Tokens lemmatisieren
		Tool lemmatizer = new Lemmatizer(
				"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model",
				false);
		Set<String> escoLemmas = new HashSet<String>();
		for (String e : esco) {
//			e = e.toLowerCase();
//			System.out.println(e);
			String[] tokens = e.split("\\s");
			String[] lemmata = MateTagger.getLemmata(tokens, lemmatizer);
			String eLemma = "";
			for (int i = 0; i < lemmata.length; i++) {
				eLemma = eLemma + " " + lemmata[i];
			}
			eLemma = eLemma.trim();
			eLemma = eLemma.toLowerCase();
//			if(!eLemma.equals(e))
//				System.out.println(eLemma + " -- " + e);
			escoLemmas.add(eLemma);

		}

		reader.close();
		return escoLemmas;
	}

	private void exportWordList(Set<String> wordList, String path) throws IOException {
		OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(path), "UTF8");
		for (String s : wordList) {
			osw.append(s + "\n");
		}
		osw.close();
	}

	private Set<String> readComps() throws IOException {

		IEJobs jobs = new IEJobs(compFile, null, null, null, null, false, null);
		Set<String> jobsComp = new HashSet<String>();
		Map<String, Set<InformationEntity>> entities = jobs.entities;
		for (Entry<String, Set<InformationEntity>> e : entities.entrySet()) {
			for (InformationEntity ie : e.getValue()) {
				jobsComp.add(ie.toString());
			}
		}
		return jobsComp;
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
			String comp = orthElement.text().toLowerCase();
			amsComps.add(comp);
		}

		return amsComps;
	}

}
