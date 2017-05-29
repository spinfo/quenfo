package de.uni_koeln.spinfo.classification.jasc.preprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassifyUnitSplitter {

	public static List<String> splitAtNewLine(String toSplit) {
		List<String> toReturn = new ArrayList<String>();
		String[] split = toSplit.split("\n");
		for (String string : split) {
			toReturn.add(string);
		}
		return toReturn;
	}

	public static List<String> splitAtEmptyLine(String toSplit) {
		List<String> toReturn = new ArrayList<String>();
		List<String> splitted = splitAtNewLine(toSplit);
		StringBuffer merged = new StringBuffer();
		for (String string : splitted) {
			string = string.trim();
			if (string.length() > 0) {
				merged.append(string + "\n");
			} else {
				toReturn.add(merged.toString());
				merged = new StringBuffer();
			}
		}
		toReturn.add(merged.toString());
		return toReturn;
	}

	/**
	 * Splits a String into reasonable paragraphs by performing the following
	 * steps: 1. split at empty line 2. remove lines without any alphanumerical
	 * character 3. merge list items together 4. merge lines where first line
	 * doesn't end with dot and second line doesn't start with upper case char
	 * or is job title
	 */
	public static List<String> splitIntoParagraphs(String s) {
		List<String> toReturn = new ArrayList<String>();
		List<String> splitted = mergeLists(splitAtEmptyLine(s));

		for (String string : splitted) {
			string = string.trim();
			if (string.length() > 0 && !containsOnlyNonWordChars(string)) {
				toReturn.add(string);
			}
		}

		toReturn = mergeLists(toReturn);
		toReturn = mergeWhatBelongsTogether(toReturn);

		return toReturn;
	}

	private static boolean looksLikeJobTitle(String string) {
		Pattern jobTitlePattern = Pattern.compile("^.*\\w+/-?\\w+.*$",
				Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE); // matches
																	// constructions
																	// with
		// slash in between, e.g.
		// 'm/w' or 'Bewerber/in'
		Matcher jobTitleMatcher = jobTitlePattern.matcher(string);

		if (jobTitleMatcher.find()) {
			int start = jobTitleMatcher.start();
			int end = jobTitleMatcher.end();
			if ((end - start) < 70 /*
									 * && (endPositionInText / (double)
									 * string.length() < 0.5)
									 */)
				return true;
		}
		return false;
	}

	public static boolean containsOnlyNonWordChars(String string) {
		Pattern p = Pattern.compile("\\w"); // regex for a word character:
											// [a-zA-Z_0-9]
		if (p.matcher(string).find()) {
			return false;
		}
		return true;
	}

	private static List<String> mergeLists(List<String> strings) {
		List<String> toReturn = new ArrayList<String>();

		String previous = null, next;
		int i = 0;
		if (strings.size() > 0)
			previous = strings.get(i);
		while (i < strings.size() - 1) {
			next = strings.get(i + 1);

			if (isListItem(previous) && isListItem(next)) {
				previous = previous + "\n" + next;
			} else {
				toReturn.add(previous);
				previous = next;
			}
			i++;
		}
		toReturn.add(previous);

		return toReturn;
	}

	private static boolean isListItem(String string) {
		String regex = "^(-\\*|-|\\*|\\d(\\.|\\)|\\.\\)))(\\p{Blank})*(?>\\P{M}\\p{M}*)+$";
		Pattern listPattern = Pattern.compile(regex);

		Matcher listMatcher = listPattern.matcher(string);
		return listMatcher.matches();
	}

	public static List<String> mergeWhatBelongsTogether(List<String> strings) {
		List<String> toReturn = new ArrayList<String>();
		String previous = "", next;
		int i = 0;
		if (strings.size() > 0)
			previous = strings.get(i);
		while (i < strings.size() - 1) {
			if (i == 0)
				previous = strings.get(i);
			next = strings.get(i + 1);

			if (!endsWithDot(previous)
					&& (!startsWithUpperCaseLetter(next) || looksLikeJobTitle(next))) {
				previous = previous + "\n" + next;
			} else {
				toReturn.add(previous);
				previous = next;
			}
			i++;
		}
		toReturn.add(previous);
		return toReturn;
	}

	private static boolean startsWithUpperCaseLetter(String string) {
		String regex = "^\\p{javaUpperCase}"; // regex for "starts with
												// uppercase letter
		Pattern p = Pattern.compile(regex);
		if (p.matcher(string.trim()).find()) {
			return true;
		}
		return false;
	}

	private static boolean endsWithDot(String string) {
		String regex = "\\.$"; // regex for "ends with dot"
		Pattern p = Pattern.compile(regex);
		if (p.matcher(string.trim()).find()) {
			return true;
		}
		return false;
	}
}