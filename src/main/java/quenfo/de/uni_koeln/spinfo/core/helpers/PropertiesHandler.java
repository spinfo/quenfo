package quenfo.de.uni_koeln.spinfo.core.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import quenfo.de.uni_koeln.spinfo.information_extraction.data.IEType;


public class PropertiesHandler {
	
	private static Properties generalProps = null;
	private static Properties ieProps = null;
	private static Properties matchingProps = null;
	private static Properties classificationProps = null;
	
	private static String quenfoData = null;
	
	
	private static String splittedCompounds = null;
	private static String possibleCompounds = null;
	private static String regex = null;
	private static String stopwords = null;
	
	private static String lemmatizerModel = null;
	private static String taggerModel  = null;
	
	
	
	
	public static void initialize(File configFolder) throws IOException {
		
		PropertiesHandler.quenfoData = configFolder.getParent();
		
		splittedCompounds = quenfoData + "/resources/nlp/compounds/splittedCompounds.txt";
		possibleCompounds = quenfoData + "/resources/nlp/compounds/possibleCompounds.txt";
		regex = quenfoData + "/resources/classification/regex.txt";
		stopwords = quenfoData + "/resources/classification/stopwords.txt";
		
		lemmatizerModel = quenfoData + "/resources/nlp/matetools/lemma-ger-3.6.model";
		taggerModel = quenfoData + "/resources/nlp/matetools/tag-ger-3.6.model";
		
		generalProps = loadPropertiesFile(configFolder.getAbsolutePath() + "/general.properties");
		ieProps = loadPropertiesFile(configFolder.getAbsolutePath() + "/informationextraction.properties");
		classificationProps = loadPropertiesFile(configFolder.getAbsolutePath() + "/classification.properties");
		matchingProps = loadPropertiesFile(configFolder.getAbsolutePath() + "/matching.properties");
	}

	
	public static String getQuenfoData() {
		return quenfoData;
	}

	public static String getSplittedCompounds() {
		return splittedCompounds;
	}

	public static String getPossibleCompounds() {
		return possibleCompounds;
	}

	public static String getRegex() {
		return regex;
	}

	public static String getStopwords() {
		return stopwords;
	}
	
	public static String getLemmatizerModel() {
		return lemmatizerModel;
	}
	
	public static String getTaggerModel() {
		return taggerModel;
	}
	
	
	public static String getStringProperty(String domain, String key) {
		return getProperty(domain, key);
	}
	
	public static int getIntProperty(String domain, String key) {
		String value = getProperty(domain, key);
		return Integer.parseInt(value);
	}
	
	public static boolean getBoolProperty(String domain, String key) {
		String value = getProperty(domain, key);
		return Boolean.parseBoolean(value);
	}
	
	public static int[] getIntArrayProperty(String domain, String key) {
		if (getProperty(domain, key).equals("")) {
			return null;
		}
		String[] nGramString = getProperty(domain, key).split(",");
		int[] nGrams = new int[nGramString.length];
		// TODO JB: leere Strings abfangen ( , , )
		for (int i = 0; i < nGramString.length; i++) {
			nGrams[i] = Integer.parseInt(nGramString[i].replaceAll("\\D", ""));
		}
		
		return nGrams;
	}
	
	public static IEType getSearchType(String domain) {
		String value = getProperty(domain, "search");
		IEType ieType;
		
		switch (value) {
		case "2":
			ieType = IEType.COMPETENCE_IN_2;
			break;
		case "3":
			ieType = IEType.COMPETENCE_IN_3;
			break;
		case "23":
			ieType = IEType.COMPETENCE_IN_23;
			break;
		default:
			System.out.println("default: search in Class 3");
			ieType = IEType.COMPETENCE_IN_3;
			break;
		}
		
		return ieType;
	}
	

	
	/**
	 * get property value for the given key from the given domain
	 * @param domain must be from: general, ie, matching, classification
	 * @param key
	 * @return
	 */
	private static String getProperty(String domain, String key) {
		
		
		String value = "";
		
		switch (domain) {
		case "general":
			value = generalProps.getProperty(key);
			break;
		case "ie":
			value = ieProps.getProperty(key);
			break;
		case "matching":
			value = matchingProps.getProperty(key);
			break;
		case "classification":
			value = classificationProps.getProperty(key);
			break;
		default:
			break;
		}
		
		return value;
	}
	

	private static Properties loadPropertiesFile(String path) throws IOException {

		File propsFile = new File(path);
		if (!propsFile.exists()) {
			System.err.println(
					"Config File " + path + " does not exist." + "\nPlease change configuration and start again.");
			System.exit(0);
		}

		Properties properties = new Properties();
		InputStream is = new FileInputStream(propsFile);
		properties.load(is);
		return properties;
	}
	
	
	
	
	
	

}
