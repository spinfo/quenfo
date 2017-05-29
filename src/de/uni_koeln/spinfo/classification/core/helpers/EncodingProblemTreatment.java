package de.uni_koeln.spinfo.classification.core.helpers;

import java.util.ArrayList;
import java.util.List;

public class EncodingProblemTreatment {

	public static List<String> normalizeEncoding(List<String> paragraphs) {
		List<String> toReturn = new ArrayList<String>();
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
