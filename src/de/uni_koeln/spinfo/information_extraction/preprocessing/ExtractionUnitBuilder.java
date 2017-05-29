package de.uni_koeln.spinfo.information_extraction.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;

/**
 * @author geduldia
 * 
 * A class to seperate ClassifyUnits (~paragraphs) into ExtractionUnits (~sentences)
 *
 */
public class ExtractionUnitBuilder {
	
	private static boolean init;
	private static Set<String> regexes;
	private static Map<String,String> matches ;
	private static PrintWriter out;
	
	
	/**
	 * transforms a single classifyUnit (≈ paragraph) in a list of
	 * extractionUnits (≈ sentences, list-elements )
	 * @param cu
	 * @return
	 * @throws IOException
	 */
	public static List<ExtractionUnit> initializeIEUnits(ClassifyUnit cu) throws IOException {
		List<ClassifyUnit> list = new ArrayList<ClassifyUnit>();
		list.add(cu);
		return initializeIEUnits(list);
	}
	
	public static List<ExtractionUnit> initializeIEUnits(List<ClassifyUnit> classifyUnits) throws IOException{
		return initializeIEUnits(classifyUnits, true, true, true);
	}
	
	/**
	 *  
	 *  transforms a list of classifyUnits (≈ paragraphs) in a list of
	 * extractionUnits (≈ sentences)
	 * @param classifyUnits
	 * @return extractionUnits
	 * @throws IOException
	 */
	public static List<ExtractionUnit> initializeIEUnits(List<ClassifyUnit> classifyUnits, boolean lemmata, boolean morphTags, boolean posTags) throws IOException {
		if(!init){
			init();
		}
		List<ExtractionUnit> extractionUnits = new ArrayList<ExtractionUnit>();
		IETokenizer tokenizer = new IETokenizer();
		for (ClassifyUnit cu : classifyUnits) {
			List<String> sentences = tokenizer.splitIntoSentences(cu.getContent());
			for (String sentence : sentences) {
				if(init){
					sentence = deleteRegexes(sentence);
				}
				sentence = correctSentence(sentence);
				if (sentence.length() > 1) {
					ExtractionUnit extractionUnit = new ExtractionUnit();
					extractionUnit.setSentence(sentence);
					extractionUnit.setJobAdID(((JASCClassifyUnit) cu).getParentID());
					extractionUnit.setSecondJobAdID(((JASCClassifyUnit) cu).getSecondParentID());
					extractionUnit.setClassifyUnitID(cu.getID());
					extractionUnit.setJobAdID(((JASCClassifyUnit) cu).getParentID());
					extractionUnits.add(extractionUnit);
				}
			}
		}
		if(init){
			for (String match : matches.keySet()) {
				out.write(match + ":\t\t\t"+matches.get(match)+"\n");	
			}
			out.flush();
			out.close();
		}
	
		MateTagger.setLexicalData(extractionUnits, lemmata, morphTags, posTags);
		return extractionUnits;
	}
	
	private static void init() throws IOException{
		File regexFile = new File("information_extraction/data/regexes.txt");
		if(!regexFile.exists()){
			return;
		}
		regexes = new HashSet<String>();
		matches = new HashMap<String,String>();
		BufferedReader in = new BufferedReader(new FileReader(regexFile));
		String line = in.readLine();
		while (line != null) {
			if(!line.equals("")){
				regexes.add(line.trim());
			}
			line = in.readLine();
		}
		in.close();
		out = new PrintWriter(new FileWriter(new File("information_extraction/data/deletedRegexes.txt")));
		init = true;
	}

	private static String deleteRegexes(String sentence) {
		Pattern pattern;
		Matcher matcher;
		for (String regex : regexes) {
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(sentence);
			while (matcher.find()) {
				matches.put(matcher.group(), regex);
				sentence = sentence.replace(matcher.group(), "");
			}
		}
		return sentence;
	}
	
	private static String correctSentence(String sentence) {
		String regex1 = "\\ \\,\\w";
		String regex2 = "[A-Z|a-z](\\,|\\;)[A-Z|a-z]";
		
		Pattern p = Pattern.compile(regex1);
		Matcher m = p.matcher(sentence);
		boolean b = false;
		while(m.find()){
			sentence = sentence.replace(m.group(), ", "+m.group().substring(2));
		}
		p = Pattern.compile(regex2);
		m = p.matcher(sentence);
		while(m.find()){
			sentence = sentence.replace(m.group(), m.group().substring(0, 2)+" "+m.group().substring(2));
		}
		if(sentence.contains(" & ")){
			sentence.replace(" & ", " und ");
		}
		return sentence;
	}
	
	
	


	
}
