package quenfo.de.uni_koeln.spinfo.information_extraction.utils;

import java.util.List;

/**
 * utility class that contains methods for data processing, e.g. string normalization
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
		// if(!before.trim().equals(lemma.trim())){
		// System.out.println("\nbefore: "+ before);
		// System.out.println("after: "+lemma);
		// }

		return lemma;
	}
	
	/**
	 * proofs if all letters in the string are upper case (if so, the hyphen must
	 * not be deleted)
	 * 
	 * @param string
	 * @return
	 */
	@Deprecated
	public static boolean isAllUpperCase(String string) {

		string = string.replaceAll("-", "");

		int i = 0;
		try {
			// character ist entweder kein Buchstabe oder ein großgeschriebener Buchstabe
			while (!Character.isLetter(string.charAt(i)) || Character.isUpperCase(string.charAt(i))) {
				i++;
			}
		} catch (StringIndexOutOfBoundsException e) {
			return true;
		}
		return false;
	}
	
	
	/**
	 * checks if list b is a sublist of list a
	 * @param a
	 * @param b
	 * @return
	 */
	@Deprecated
	public static boolean containsList(List<String> a, List<String> b) {

		
		if (a.size() < b.size()) {
			return false;
		}
		for (int i = 0; i <= a.size() - b.size(); i++) {
			boolean match = false;
			for (int j = 0; j < b.size(); j++) {
				match = a.get(i + j).equals(b.get(j));
				if (!match)
					break;
			}
			if (match) {
				return true;
			}
		}
		return false;
	}

}
