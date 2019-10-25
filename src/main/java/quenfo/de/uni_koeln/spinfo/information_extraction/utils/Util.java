package quenfo.de.uni_koeln.spinfo.information_extraction.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.tagging.TaggedWord;
import org.languagetool.tagging.de.GermanTagger;

import com.opencsv.CSVReader;

import is2.data.SentenceData09;
import is2.lemmatizer.Lemmatizer;
import is2.tools.Tool;
import quenfo.de.uni_koeln.spinfo.core.helpers.PropertiesHandler;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import quenfo.de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;

/**
 * utility class that contains methods for data processing, e.g. string
 * normalization
 * 
 * @author Johanna Binnewitt
 *
 */
public final class Util {

	/**
	 * normalizes the given string - trim - deletes (most) special characters at the
	 * begin and end of the string (with some exceptions)
	 * 
	 * @param lemma string to normalize
	 * @return normalized string
	 */
	public static String normalizeLemma(String lemma) {

		// String before = lemma;
		lemma = lemma.trim();
		if (lemma.equals("--")) {
			return lemma;
		}
		if (lemma.startsWith("<end-")) {
			return lemma;
		}
		if (lemma.startsWith("<root-"))
			if (lemma.length() <= 1) {
				return lemma;
			}
		while (true) {
			lemma = lemma.trim();
			if (lemma.length() == 0) {
				break;
			}
			Character s = lemma.charAt(0);
			if (s == '_') {
				lemma = lemma.substring(1);
				lemma = lemma.trim();
			}
			if (lemma.length() == 0) {
				break;
			}
			if (!Character.isLetter(s) && !Character.isDigit(s) && !(s == '§')) {
				lemma = lemma.substring(1);
				lemma = lemma.trim();
			} else {
				break;
			}
			if (lemma.length() == 0) {
				break;
			}
		}
		while (true) {
			if (lemma.length() == 0) {
				break;
			}
			Character e = lemma.charAt(lemma.length() - 1);
			if (e == '_') {
				lemma = lemma.substring(0, lemma.length() - 1);
				lemma = lemma.trim();
			}

			if (!Character.isLetter(e) && !Character.isDigit(e) && !(e == '+') && !(e == '#')) {
				lemma = lemma.substring(0, lemma.length() - 1);
				lemma = lemma.trim();
			} else {
				break;
			}
		}

		return lemma;
	}

	public static Map<String, Set<InformationEntity>> readRDF(File rdfFile,
			Map<String, Set<InformationEntity>> entities) {

		Model model = ModelFactory.createDefaultModel();
		model = model.read(rdfFile.getAbsolutePath());

		String querySkills = "Select	*	Where { ?subj	<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>	<http://data.europa.eu/esco/model#Skill> }";
		Query query = QueryFactory.create(querySkills);
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		org.apache.jena.query.ResultSet allSkills = qe.execSelect();	

		Property broader = model.createProperty("http://www.w3.org/2004/02/skos/core#broader");
		Property broaderTrans = model.createProperty("http://www.w3.org/2004/02/skos/core#broaderTransitive");
		Property prefLabel = model.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");
		
		List<Property> props = new ArrayList<>();
		props.add(prefLabel);
		props.add(model.createProperty("http://www.w3.org/2004/02/skos/core#altLabel"));
		while (allSkills.hasNext()) {
			Resource skill = allSkills.nextSolution().getResource("?subj");
			
//			System.out.println(skill);
			// broaderTrans (Hyperonym) parsen

			List<String> broaderLabels = new ArrayList<>();
			
			StmtIterator broaderIter = skill.listProperties(broaderTrans);
			while (broaderIter.hasNext()) {
				Resource r = broaderIter.next()
						.getObject()
						.asResource();				
				String broaderLabel = r.getProperty(prefLabel, "de").getString();
				broaderLabels.add(broaderLabel);				
			}			
			if(broaderLabels.isEmpty()) {
				broaderIter = skill.listProperties(broader);
				while (broaderIter.hasNext()) {

					Resource r = broaderIter.next()
							.getObject()
							.asResource();
					
					String broaderLabel = r.getProperty(prefLabel, "de").getString();
					broaderLabels.add(broaderLabel);					
				}
			}

			String cat = broaderLabels.toString(); // TODO JB: broader Cat parsen
			
			
			// Label und Synonyme parsen
			for (Property p : props) {

				StmtIterator iter = skill.listProperties(p, "de");
				while (iter.hasNext()) {
					String label = iter.next().getString(); // .toLowerCase()

					// Label normalisieren und
					String[] split = label.split(" ");
					String keyword;
					try {
						keyword = Util.normalizeLemma(split[0]);
					} catch (ArrayIndexOutOfBoundsException e) {
						continue;
					}
					Set<InformationEntity> iesForKeyword = entities.get(keyword);
					if (iesForKeyword == null)
						iesForKeyword = new HashSet<InformationEntity>();
					InformationEntity ie = new InformationEntity(keyword, split.length == 1, cat);
					if (!ie.isSingleWordEntity()) {
						for (String string : split) {
							ie.addLemma(Util.normalizeLemma(string));

						}
					}
					if (iesForKeyword.contains(ie)) {
						for (InformationEntity curr : iesForKeyword) {
							if (curr.equals(ie)) {
								curr.addLabel(cat);
								iesForKeyword.add(curr);
							}
						}
					} else
						iesForKeyword.add(ie);

					entities.put(keyword, iesForKeyword);
				}
			}
		}
		return entities;
	}

	public static Map<String, Set<InformationEntity>> readTEI(File teiFile, String category,
			Map<String, Set<InformationEntity>> entities) throws IOException, FileNotFoundException {

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(teiFile), "UTF8"));

		StringBuilder sb = new StringBuilder();
		String line = "";
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		br.close();
		String teiString = sb.toString();

		Document doc = Jsoup.parse(teiString, "", Parser.xmlParser());

		Map<String, Set<String>> allOrths = new HashMap<>();

		Elements catElements = doc.select(category);
		for (Element catElement : catElements) {
			String cat = catElement.attr("label");
			Elements orthElements = catElement.select("orth");
			for (Element orthElement : orthElements) {
				String orth = orthElement.text();
				if (orth.equals(""))
					continue;

				Set<String> labels = allOrths.get(orth);
				if (labels == null)
					labels = new HashSet<String>();
				labels.add(cat);
				allOrths.put(orth, labels);

				String[] split = orth.split(" ");
				String keyword;
				try {
					keyword = Util.normalizeLemma(split[0]);
				} catch (ArrayIndexOutOfBoundsException e) {
					continue;
				}
				Set<InformationEntity> iesForKeyword = entities.get(keyword);
				if (iesForKeyword == null)
					iesForKeyword = new HashSet<InformationEntity>();
				InformationEntity ie = new InformationEntity(keyword, split.length == 1, cat);
				if (!ie.isSingleWordEntity()) {
					for (String string : split) {
						ie.addLemma(Util.normalizeLemma(string));

					}
				}
				if (iesForKeyword.contains(ie)) {
					for (InformationEntity curr : iesForKeyword) {
						if (curr.equals(ie)) {
							curr.addLabel(cat);
							iesForKeyword.add(curr);
						}
					}
				} else
					iesForKeyword.add(ie);

				entities.put(keyword, iesForKeyword);

			}

		}

		return entities;
	}

	public static Map<String, Set<InformationEntity>> readCSV(File entitiesFile,
			Map<String, Set<InformationEntity>> entities) throws IOException {
		
		IETokenizer tokenizer = new IETokenizer();
		CSVReader reader = new CSVReader(new FileReader(entitiesFile));		
		GermanTagger tagger = new GermanTagger();

		String keyword;
		for (String[] line : reader) {
			String uri = line[1];
			if (uri.equals("conceptUri")) //Kopfzeile
				continue;
			
			List<String> skills = new ArrayList<>(Arrays.asList(line[5].split("\n")));
			skills.add(line[4]);
			
			for(String skill : skills) {
				
				if (skill.isEmpty())
					continue;
				
				List<String> tokens = Arrays.asList(tokenizer.tokenizeSentence(skill));
				
				List<String> lemmas = new ArrayList<>();
				for (String token : tokens) {
					System.out.println(token);
					List<TaggedWord> readings = tagger.tag(token);
					if(readings.size() == 0)
						lemmas.add(token.toLowerCase());
					else
						lemmas.add(readings.get(0).getLemma().toLowerCase());
				}
				
				System.out.println("Tokens: " + tokens + "Lemmas: " + lemmas);
				
				keyword = Util.normalizeLemma(lemmas.get(0));
				Set<InformationEntity> iesForKeyword = entities.get(keyword);
				if (iesForKeyword == null)
					iesForKeyword = new HashSet<InformationEntity>();
				InformationEntity ie = new InformationEntity(keyword, lemmas.size() == 1, uri);
				if (!ie.isSingleWordEntity()) {
					for (String lemma : lemmas) {
						ie.addLemma(Util.normalizeLemma(lemma));

					}
				}
				if (iesForKeyword.contains(ie)) {
					for (InformationEntity curr : iesForKeyword) {
						if (curr.equals(ie)) {
							curr.addLabel(uri);
							iesForKeyword.add(curr);
						}
					}
				} else
					iesForKeyword.add(ie);
				
				//System.out.println(ie.getLemmata() + " " + ie.getLabels());
				entities.put(keyword, iesForKeyword);
				
				
//				List<AnalyzedTokenReadings> tagged = tagger.tag(tokens);
//							
//				List<String> lemmaList = new ArrayList<>();
//				
//				for(AnalyzedTokenReadings readings : tagged) {
//					
//					Set<String> lemmas = new HashSet<>();
//					
//					List<AnalyzedToken> currReadings = readings.getReadings();
//					String prefReading = currReadings.get(0).getLemma();
//					if (prefReading != null)
//						lemmaList.add(prefReading);
//					else
//						System.out.println(readings);
//					
//					for (AnalyzedToken t : currReadings) {						
//						lemmas.add(t.getLemma());
//					}
//					if(lemmas.size() > 1)
//						System.out.println(lemmas);				
//					// TODO JB: richtiges Lemma auswählen				
//				}
//				
//				
				
				

			}
		}
		reader.close();
		return entities;
	}

}
