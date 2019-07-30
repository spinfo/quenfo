package quenfo.de.uni_koeln.spinfo.classification.core.helpers;

import java.util.HashSet;
import java.util.Set;

public class EncodingProblemTreatment {

	public static Set<String> normalizeEncoding(Set<String> paragraphs) {
		Set<String> toReturn = new HashSet<String>();
		for (String paragraph : paragraphs) {

			toReturn.add(normalizeEncoding(paragraph));
		}
		return toReturn;
	}

	public static String normalizeEncoding(String string) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (Character.isWhitespace(c)) {
				sb.append(c);
			}
			if (c >= '!' && c <= 'z') {
				sb.append(c);
			}
		}
		return sb.toString();
	}

}
