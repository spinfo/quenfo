package quenfo.de.uni_koeln.spinfo.information_extraction.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.danielnaber.jwordsplitter.AbstractWordSplitter;
import de.danielnaber.jwordsplitter.GermanWordSplitter;
import is2.tools.Tool;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.TextToken;
import quenfo.de.uni_koeln.spinfo.information_extraction.preprocessing.MateTagger;

public class CoordinationResolver {

	private AbstractWordSplitter splitter;

	// key = stamm, value = suffix
	private Map<String, String> possResolvations;

	private String resolvedFileString = "src/test/resources/coordinations/resolvedCompounds.txt";//"C://sqlite/coordinations/resolvedCompounds.txt";// TODO pfad für BIBB anpassen
	private String possibleFileString = "src/test/resources/coordinations/possibleCompounds.txt";//C://sqlite/coordinations/possibleCompounds.txt";

	public CoordinationResolver() {

		possResolvations = new HashMap<String, String>();
		try {
			this.splitter = new GermanWordSplitter(true); // true = interfix-Zeichen werden beibehalten
															// (BahnhofSvorstand)
			Map<String, List<String>> rCompounds = readCompounds();
			for (Map.Entry<String, List<String>> e : rCompounds.entrySet()) {
				splitter.addException(e.getKey(), e.getValue());
			}
			// fügt die neu evaluierten Komposita der txt-Datei mit den feststehenden
			// Komposita hinzu
			writeResolvedCoordinations(rCompounds);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Map<String, String> getPossResolvations() {
		return possResolvations;
	}

	/**
	 * liest alle bereits bekannten Komposita und alle neu evaluierten
	 * 
	 * @return
	 * @throws IOException
	 */
	private Map<String, List<String>> readCompounds() throws IOException {

		Map<String, List<String>> toReturn = new HashMap<String, List<String>>();
		// liest bereits bekannte Komposita ein
		toReturn.putAll(readFromFile(resolvedFileString));
		// liest evaluierte Komposita ein
		toReturn.putAll(readFromFile(possibleFileString));

		return toReturn;
	}

	private void writeResolvedCoordinations(Map<String, List<String>> toReturn) {
		StringBuilder sb = new StringBuilder();
		for (List<String> list : toReturn.values()) {
			for (String morphem : list) {
				sb.append(morphem + "|");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append("\n");
		}

		try {
			File f = new File(resolvedFileString);
			if (!f.exists()) {
				f.getParentFile().mkdirs();
				f.createNewFile();
			}
			FileWriter fw = new FileWriter(f);
			fw.write(sb.toString());
			fw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private Map<String, List<String>> readFromFile(String fileName) throws IOException {

		Map<String, List<String>> toReturn = new HashMap<String, List<String>>();

		BufferedReader in = new BufferedReader(new FileReader(new File(fileName)));
		String line;
		while ((line = in.readLine()) != null) {
			String[] parts = line.split("\\|");
			if (parts.length > 1)//
				toReturn.put(parts[0] + parts[1], Arrays.asList(parts));
		}
		in.close();

		return toReturn;

	}

	public void writeNewCoordinations() {
		System.out.print(": " + possResolvations.size());
		StringBuffer sb = new StringBuffer();
		for (Map.Entry<String, String> e : possResolvations.entrySet()) {
			sb.append(e.getKey() + "|" + e.getValue() + "\n");
		}

		try {
			File f = new File(possibleFileString);
			if (!f.exists()) {
				f.getParentFile().mkdirs();
				f.createNewFile();
			}
			FileWriter fw = new FileWriter(f);
			fw.write(sb.toString());
			fw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public List<String[]> resolve(List<TextToken> completeEntity, Tool lemmatizer) {
		return resolve(completeEntity, completeEntity, lemmatizer, false);
	}

	/**
	 * löst alle Koordinationen im Ausdruck auf und gibt für jede Auflösung eine
	 * Liste von Tokens zurück. Merkmal einer Koordination ist hier eine Konjunktion
	 * (POS-tag KON)
	 * @param completeEntity
	 * @param extractionUnit
	 * @param lemmatizer
	 * @param debug
	 * @return list of lemmas for every resolved coordination
	 */
	public List<String[]> resolve(List<TextToken> completeEntity, List<TextToken> extractionUnit, Tool lemmatizer,
			boolean debug) {
		List<String[]> toReturn = new ArrayList<String[]>();
		String[] tokens = new String[completeEntity.size()];
		String[] pos = new String[completeEntity.size()];
		String[] lemma = new String[completeEntity.size()];

		for (int i = 0; i < completeEntity.size(); i++) {
			tokens[i] = completeEntity.get(i).getString();
			pos[i] = completeEntity.get(i).getPosTag();
			lemma[i] = completeEntity.get(i).getLemma();
		}
		if (debug)
			System.out.println("Entity Tokens: " + Arrays.asList(tokens));

		if (Arrays.asList(pos).contains("TRUNC")) { // Rechtsellipse Morphemkoordination
			return resolveTruncEllipsis(tokens, pos, lemma, extractionUnit, lemmatizer, debug);
		} else { // TODO Linksellipse, andere Koordinationen
			
			return toReturn;
		}
	}

	private List<String[]> resolveTruncEllipsis(String[] tokens, String[] pos, 
			String[] lemmata, List<TextToken> extractionUnit,
			Tool lemmatizer, boolean debug) {

		List<String[]> toReturn = new ArrayList<String[]>();
		int startKoo = Arrays.asList(pos).indexOf("TRUNC");
		// prüfen, ob es sich um die Koordination von NN oder ADJA handelt
		boolean startsWithCapLetter = Character.isUpperCase(tokens[startKoo].charAt(0));
		String konjunctPOS = "";
		if (startsWithCapLetter) {
			try {
				konjunctPOS = "NN";
				// prüfen, ob vor erster NN ein ADJA steht
				if (pos[startKoo - 1].equals("ADJA")) {
					startKoo--;
					if (pos[startKoo - 1].equals("ADV"))
						startKoo--;
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				// falls vor NN keine Tokens stehen
			}
		} else
			konjunctPOS = "ADJA";

		// Ende der Koordination bestimmen
		int endKoo = Arrays.asList(pos).subList(startKoo, pos.length).indexOf(konjunctPOS);
		
		
		
		if (endKoo < 0) { // falls Koordination abgeschnitten ist
			List<TextToken> missingPart = completeCoordination(extractionUnit, tokens, startKoo, konjunctPOS);
			
			List<String> tokenList = new ArrayList<String>(Arrays.asList(tokens));
			List<String> posList = new ArrayList<String>(Arrays.asList(pos));
			List<String> lemmaList = new ArrayList<String>(Arrays.asList(lemmata));

			for (TextToken tt : missingPart) {
				tokenList.add(tt.getString());
				posList.add(tt.getPosTag());
				lemmaList.add(tt.getLemma());
			}		

			tokens = new String[tokenList.size()];
			tokenList.toArray(tokens);
			pos = new String[posList.size()];
			posList.toArray(pos);
			lemmata = new String[lemmaList.size()];
			lemmaList.toArray(lemmata);

			endKoo = tokens.length - 1;
			
		} else
			endKoo = endKoo + startKoo; // wegen subList-Index

		// Tokens, die vor der Koordination stehen
		List<String> before = new ArrayList<String>();
		for (int i = 0; i < startKoo; i++) {
			before.add(tokens[i]);
		}

		// Token, die nach der Koordination stehen
		List<String> after = new ArrayList<String>();
		for (int i = endKoo + 1; i < tokens.length; i++) {
			after.add(tokens[i]);
		}
		
		if (debug)
			System.out.println("Before: " + before + "\nAfter: " + after);

		// sammelt alle konjunkt-modifikator-paare (0 = modifier, 1 = trunc/nn/adja)
		List<String[]> konjuncts = new ArrayList<String[]>();

		String[] konjunct = new String[2];
		String currentMod = ""; //

		List<String> posToIgnore = new ArrayList<String>();
		posToIgnore.add("KON");
		posToIgnore.add("$,");
		boolean startNewKonjunct = true; // benötigt, damit mods auf neue Konjunkte übertragen werden

		for (int i = startKoo; i <= endKoo; i++) {
			if (pos[i].equals("TRUNC") || pos[i].equals(konjunctPOS)) {
				konjunct[0] = currentMod.trim();
				konjunct[1] = tokens[i];
				konjuncts.add(konjunct);
				konjunct = new String[2];
				startNewKonjunct = true;
			} else if (posToIgnore.contains(pos[i]))
				continue;
			else {
				if (startNewKonjunct)
					currentMod = tokens[i].trim();
				else
					currentMod = currentMod + " " + tokens[i];
				startNewKonjunct = false;
			}
		}

		// Koordinationen auflösen
		List<String[]> coordinations = combineCoordinations(konjuncts);

		// Auflösung in restlichen Satz zurückführen
		for (String[] c : coordinations) {
			List<String> combined = new ArrayList<String>();
			combined.add("<root>");
			combined.addAll(before);
			if (!c[0].isEmpty())
				combined.add(c[0].replaceAll("[^A-Za-zäÄüÜöÖß-]", ""));
			combined.add(c[1].replaceAll("[^A-Za-zäÄüÜöÖß-]", ""));
			combined.addAll(after);

			String[] combiArray = new String[combined.size()];
			combined.toArray(combiArray);

			//Aufgelösten Satz lemmatisieren
			String[] lemmataResolved = MateTagger.getLemmata(combiArray, lemmatizer);						
			
			if(lemmataResolved.length == 2) { //falls IE nur aus einem lemma (+root) besteht
				
				String rightLemma = lemmata[endKoo].replaceAll("[^A-Za-zäÄüÜöÖß-]", "");
				
				//prüfen, ob zusammengesetztes Token richtig lemmatisiert wurde
				String l = lemmataResolved[before.size() + 1];				
				if(!(l.charAt(l.length()-1) 
						== rightLemma.charAt(rightLemma.length()-1))) {
					lemmataResolved[before.size() + 1] = correctLemma(l, rightLemma);
				}
			}

			String[] result = new String[lemmataResolved.length - 1];
			for (int i = 1; i < lemmataResolved.length; i++) {
				result[i - 1] = lemmataResolved[i];
			}
			toReturn.add(result);
		}
		return toReturn;
	}

	/**
	 * gleicht das Suffix des falschen Lemmas an das Suffix des richtigen Lemmas an
	 * @param wrongLemma
	 * @param rightLemma
	 * @return korrigiertes (urspr. falsches) Lemma
	 */
	private String correctLemma(String wrongLemma, String rightLemma) {
		
		char[] wrongC = wrongLemma.toCharArray();
		char[] rightC = rightLemma.toCharArray();
		
		// übereinstimmenden Substring finden
		//Suffix von rightLemma ist auf jeden Fall richtig
		//wrongLemma ist zu kurz oder zu lang
		int i = wrongLemma.lastIndexOf(rightLemma.charAt(rightLemma.length()-1));
		
		String suffix = "";
		if(i < 0) { //suffix des richtigen Lemma ist nicht im falschen Lemma -> wrongLemma zu kurz
			i = rightC.length-1;
			while(rightC[i] != wrongC[wrongC.length-1]) {
				suffix = rightC[i] + suffix;
				i--;
			}
			wrongLemma = wrongLemma + suffix;
		} else { //wrongLemma zu lang
			wrongLemma = wrongLemma.substring(0, i+1);
		}

		return wrongLemma;
	}

	private List<TextToken> completeCoordination(List<TextToken> extractionUnit, String[] tokens, int startKoo,
			String konjunctPOS) {

		List<TextToken> toReturn = new ArrayList<TextToken>();
		for (int i = 0; i < extractionUnit.size(); i++) {
			TextToken tt = extractionUnit.get(i);

			if (tt.getString().equals(tokens[tokens.length - 1])) { // sobald der Satz am Beginn der IE ist
				int j = i + 1;
				do {
					tt = extractionUnit.get(j);

					toReturn.add(tt);
					j++;
				} while (!tt.getPosTag().equals(konjunctPOS));
				break;
			}
		}
		return toReturn;
	}

	/**
	 * ermittelt im letzten Konjunkt das elliptische Suffix und hängt dieses an alle
	 * anderen Konjunkte
	 * 
	 * @param konjuncts
	 * @param debug 
	 * @return zusammengesetzte Konjunkte
	 */
	private List<String[]> combineCoordinations(List<String[]> konjuncts) {
		String lastKonjunct = konjuncts.get(konjuncts.size() - 1)[1];
		lastKonjunct = lastKonjunct.replaceAll("[^A-Za-zäÄüÜöÖß-]", "");
		
		// zerteilung in morpheme
		List<String> subtokens = splitter.splitWord(lastKonjunct);
		String compound = ""; // Morphem(komposita), das an jede Ellipse gehängt werden soll

		if (subtokens.size() == 1) { // falls Wordsplitter keine Trennung gefunden hat
			// Methoden, um selbst eine Trennung zu finden
			compound = subtokens.get(0);
			if (compound.contains("-")) {
				String[] splits = compound.split("-");
				if (splits.length == 2) {
					compound = splits[1];
					possResolvations.put(splits[0], splits[1]);
				} else { // mehr als ein Bindestrich
					String firstMorphem = splits[0];
					compound = lastKonjunct.replaceAll(firstMorphem + "-", "");
					possResolvations.put(firstMorphem, compound);
				}
			} else {
				possResolvations.put(subtokens.get(0), "");
				compound = subtokens.get(0);
			}

		} else if (subtokens.size() == 2) { // falls genau zwei Morpheme gefunden wurden, wird das letztere gewählt
			compound = subtokens.get(1);
		} else { // splitter hat mehr als zwei Morpheme gefunden
			compound = subtokens.get(subtokens.size() - 1);
			possResolvations.put(lastKonjunct.replace(compound, ""), compound);
		}

		for (int i = 0; i < konjuncts.size() - 1; i++) {

			String ellipse = konjuncts.get(i)[1];

			// Fälle, bei denen der Ergänzungsstrich als Bindestrich bleiben muss (PC-
			// // MS-Office- // ...)
			boolean ellipseIsUppercase = isAllUpperCase(ellipse);
			if (!compound.equals("")) {				
				if (ellipseIsUppercase) {
					// Bindestrich beibehalten, Compound großschreiben
					compound = Character.toUpperCase(compound.charAt(0)) + compound.substring(1);
				} else {
					ellipse = ellipse.replaceAll("-", "");
				}
			}
			konjuncts.get(i)[1] = ellipse + compound;
		}
		return konjuncts;
	}

	/**
	 * proofs if all letters in the string are upper case (if so, the hyphen must
	 * not be deleted)
	 * 
	 * @param ellipse
	 * @return
	 */
	private boolean isAllUpperCase(String ellipse) {

		ellipse = ellipse.replaceAll("-", "");

		int i = 0;
		try {
			// character ist entweder kein Buchstabe oder ein großgeschriebener Buchstabe
			while (!Character.isLetter(ellipse.charAt(i)) || Character.isUpperCase(ellipse.charAt(i))) {
				i++;
			}
		} catch (StringIndexOutOfBoundsException e) {
			return true;
		}
		return false;
	}

}
