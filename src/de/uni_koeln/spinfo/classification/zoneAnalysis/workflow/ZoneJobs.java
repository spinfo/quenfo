package de.uni_koeln.spinfo.classification.zoneAnalysis.workflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.core.featureEngineering.FeatureUnitTokenizer;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureReduction.MutualInformationFilter;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureReduction.Normalizer;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureReduction.Stemmer;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureReduction.StopwordFilter;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureSelection.LetterNGrammGenerator;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureSelection.SuffixTreeFeatureGenerator;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbstractFeatureQuantifier;
import de.uni_koeln.spinfo.classification.core.helpers.ClassifyUnitFilter;
import de.uni_koeln.spinfo.classification.core.helpers.EncodingProblemTreatment;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.preprocessing.ClassifyUnitSplitter;
import de.uni_koeln.spinfo.classification.jasc.preprocessing.JASCReader;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.ZoneAbstractClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.svm.SVMClassifier;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.helpers.SingleToMultiClassConverter;
import de.uni_koeln.spinfo.classification.zoneAnalysis.preprocessing.TrainingDataGenerator;

public class ZoneJobs {

	public ZoneJobs() throws IOException {
		System.out.println("ZoneJobs: Achtung - keine Translations gesetzt");
		sw_filter = new StopwordFilter(new File("classification/data/stopwords.txt"));
		normalizer = new Normalizer();
		stemmer = new Stemmer();
		tokenizer = new FeatureUnitTokenizer();
		suffixTreeBuilder = new SuffixTreeFeatureGenerator();
	}

	public ZoneJobs(SingleToMultiClassConverter stmc) throws IOException {
		if (stmc == null) {
			System.out.println("ZoneJobs: Achtung - keine Translations gesetzt");
		}
		this.stmc = stmc;
		sw_filter = new StopwordFilter(new File("classification/data/stopwords.txt"));
		normalizer = new Normalizer();
		stemmer = new Stemmer();
		tokenizer = new FeatureUnitTokenizer();
		suffixTreeBuilder = new SuffixTreeFeatureGenerator();

	}

	protected SuffixTreeFeatureGenerator suffixTreeBuilder;
	protected StopwordFilter sw_filter;
	protected Normalizer normalizer;
	protected Stemmer stemmer;
	protected FeatureUnitTokenizer tokenizer;
	private SingleToMultiClassConverter stmc;
	MutualInformationFilter mi_filter = new MutualInformationFilter();

	/**
	 * A method to read job advertisements from the specified file and split
	 * them into paragraphs
	 * 
	 * @param dataFile
	 *            A file containing job advertisements
	 * @return A list of paragraphs from the job ads contained in the data file
	 * @throws IOException
	 */
	public List<JASCClassifyUnit> getParagraphsFromJobAdFile(File dataFile) throws IOException {

		/* Read the source file */
		JASCReader reader = new JASCReader();
		SortedMap<Integer, String> texts = reader.getJobAds(dataFile);
		// System.out.println(texts);

		List<String> textsAsString = new ArrayList<String>(texts.values());
		Collections.reverse(textsAsString);

		/* Split to atomic classification units (here: paragraphs) */
		List<JASCClassifyUnit> paragraphs = new ArrayList<JASCClassifyUnit>();

		Set<Integer> textIDs = texts.keySet();
		for (Integer textID : textIDs) {
			List<String> splitAtEmptyLine = ClassifyUnitSplitter.splitIntoParagraphs(texts.get(textID));
			for (String content : splitAtEmptyLine) {
				JASCClassifyUnit unitToClassify = new JASCClassifyUnit(content.replaceAll("\\/", "\\_"), textID);
				paragraphs.add(unitToClassify);
			}
		}
		return paragraphs;
	}



	/**
	 * A method to get pre-categorized paragraphs from the specified file
	 * 
	 * @param trainingDataFile
	 *            file with pre-categorized paragraphs
	 * @return A list of pre-categorized paragraphs
	 * @throws IOException
	 */
	public List<ClassifyUnit> getCategorizedParagraphsFromFile(File trainingDataFile, boolean treatEncoding)
			throws IOException {
		TrainingDataGenerator tdg = new TrainingDataGenerator(trainingDataFile, stmc.getNumberOfCategories(),
				stmc.getNumberOfClasses(), stmc.getTranslations());
		List<ClassifyUnit> paragraphs = tdg.getTrainingData();
		
		if(treatEncoding){
			for (ClassifyUnit classifyUnit : paragraphs) {
				String content = classifyUnit.getContent();
				classifyUnit.setContent(EncodingProblemTreatment.normalizeEncoding(content));
			}
		}
		
		return paragraphs;
	}

	public List<ClassifyUnit> getCategorizedParagraphsFromDB(Connection trainingConnection, boolean treatEncoding )
			throws ClassNotFoundException, SQLException {
		
		List<ClassifyUnit> toReturn = new ArrayList<ClassifyUnit>();
		Statement stmt = trainingConnection.createStatement();
		String sql = "SELECT Jahrgang, ZEILENNR, Text, ClassONE, ClassTWO, ClassTHREE, ClassFOUR  FROM trainingData";
		ResultSet result = stmt.executeQuery(sql);
		ClassifyUnit cu;
		while (result.next()) {
			String content = result.getString(3);
			int parentID = result.getInt(1);
			int secondParentID = result.getInt(2);
			boolean[] classIDs = new boolean[stmc.getNumberOfCategories()];
			for (int i = 0; i < stmc.getNumberOfCategories(); i++) {
				classIDs[i] = parseIntToBool(result.getInt(4 + i));
			}
			ZoneClassifyUnit.setNumberOfCategories(stmc.getNumberOfCategories(), stmc.getNumberOfClasses(),
					stmc.getTranslations());
			if(treatEncoding){
				cu = new JASCClassifyUnit(EncodingProblemTreatment.normalizeEncoding(content), parentID, secondParentID);
			}
			else{
				cu = new JASCClassifyUnit(content, parentID, secondParentID);
			}	
			((ZoneClassifyUnit) cu).setClassIDs(classIDs);
			toReturn.add(cu);
		}
		return toReturn;
	}



	private boolean parseIntToBool(int toParse) {
		if (toParse == 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Initializes the feature units (as tokens) of each classify unit.
	 * 
	 * @param paragraphs
	 *            Classify units to initialize
	 * @return Classify units with initialized feature units (tokens)
	 */
	public List<ClassifyUnit> initializeClassifyUnits(List<ClassifyUnit> paragraphs) {
		List<ClassifyUnit> toProcess = new ArrayList<ClassifyUnit>();
		for (ClassifyUnit paragraph : paragraphs) {
			// System.out.println(((ZoneClassifyUnit)
			// paragraph).getActualClassID());
			ZoneClassifyUnit newParagraph = new ZoneClassifyUnit(paragraph.getContent(), paragraph.getID());
		
			newParagraph.setClassIDs(((ZoneClassifyUnit) paragraph).getClassIDs());
			newParagraph.setActualClassID(((ZoneClassifyUnit) paragraph).getActualClassID());
			List<String> tokens = tokenizer.tokenize(newParagraph.getContent());
			if(tokens == null){
				continue;
			}
			newParagraph.setFeatureUnits(tokens);
			
			
			toProcess.add(newParagraph);
		}
		return toProcess;
	}

	/**
	 * Sets the feature units of each classify unit by following the
	 * instructions within the specified feature unit configuration.
	 * 
	 * @param paragraphs
	 *            Classify units with initialized features
	 * @param fuc
	 *            feature unit configuration
	 * @return Classify units with features
	 * @throws IOException
	 */
	public List<ClassifyUnit> setFeatures(List<ClassifyUnit> paragraphs, FeatureUnitConfiguration fuc,
			boolean trainingPhase) throws IOException {

		for (ClassifyUnit cu : paragraphs) {
			// if(fuc.isTreatEncoding()){
			// List<String> normalizeEncoding =
			// EncodingProblemTreatment.normalizeEncoding(cu.getFeatureUnits());
			// cu.setFeatureUnits(normalizeEncoding);
			// }
			if (fuc.isNormalize()) {
				normalizer.normalize(cu.getFeatureUnits());
			}
			if (fuc.isFilterStopwords()) {
				// System.out.println("Stopwords filtered");
				List<String> filtered = sw_filter.filterStopwords(cu.getFeatureUnits());
				cu.setFeatureUnits(filtered);
			}
			if (fuc.isStem()) {
				// System.out.println("Stemmed");
				List<String> stems = stemmer.getStems(cu.getFeatureUnits());
				cu.setFeatureUnits(stems);
			}
			int[] ngrams2 = fuc.getNgrams();
			if (ngrams2 != null) {
				// System.out.println(" GramsCont: " + continuusNgrams);
				// useNGrams(cu, nGramLength, continuusNgrams);
				List<String> ngrams = new ArrayList<String>();
				for (int i : ngrams2) {
					ngrams.addAll(LetterNGrammGenerator.getNGramms(cu.getFeatureUnits(), i, fuc.isContinuusNGrams()));
				}
				cu.setFeatureUnits(ngrams);
			}

		}
		if (fuc.getMiScore() != 0) {

			if (trainingPhase) {
				mi_filter.initialize(fuc, paragraphs);
			}
			mi_filter.filter(paragraphs, fuc.getMiScore());
		}
		if (fuc.isSuffixTree()) {
			paragraphs = suffixTreeBuilder.getSuffixTreeFreatures(paragraphs);
		}
		List<ClassifyUnit> filtered = ClassifyUnitFilter.filterByFUs(paragraphs, 1);
		return filtered;
	}

	/**
	 * Initializes the feature vectors of the classify units.
	 * 
	 * @param paragraphs
	 *            Classify units with features
	 * @param fq
	 *            The feature quantifier that should be used.
	 * @param featureUnitOrder
	 *            Pre-determined oder of pre-determined features. If null, a new
	 *            fuo will be generated using the specified features.
	 * @return Classify units with feature vectors
	 */
	public List<ClassifyUnit> setFeatureVectors(List<ClassifyUnit> paragraphs, AbstractFeatureQuantifier fq,
			List<String> featureUnitOrder) {
		if (fq != null) {
			// initialisieren mit trainingdata...
			fq.setFeatureValues(paragraphs, featureUnitOrder);
		}
		return paragraphs;
	}

	/**
	 * @param cus
	 *            the classify units
	 * @param expConfig
	 *            the experiment configuration
	 * @return a model for the specified experiment configuration
	 * @throws IOException
	 */
	public Model getModelForClassifier(List<ClassifyUnit> cus, ExperimentConfiguration expConfig) throws IOException {
		File modelFile = expConfig.getModelFile();
		// modelFile.createNewFile();
		Model model;
		if (expConfig.getClassifier() instanceof SVMClassifier) {
			SVMClassifier svmC = (SVMClassifier) expConfig.getClassifier();
			svmC.buildModel(expConfig, cus);
			return null;
		}

		if (!modelFile.exists()) {
			// build model...
			model = expConfig.getClassifier().buildModel(cus, expConfig.getFeatureConfiguration(),
					expConfig.getFeatureQuantifier(), expConfig.getDataFile());
			// store model
			// exportModel(expConfig.getModelFile(), model);
			return model;
		} else {
			System.out.println("read model..");
			// read model...
			FileInputStream fis = new FileInputStream(modelFile);
			ObjectInputStream in = new ObjectInputStream(fis);
			try {
				Object o = in.readObject();
				model = (Model) o;
				in.close();
				return model;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				in.close();
				return null;
			}
		}
	}

	/**
	 * @param cus
	 *            the classify units
	 * @param expConfig
	 *            the experiment configuration
	 * @return a model for the specified experiment configuration
	 * @throws IOException
	 */
	public Model getNewModelForClassifier(List<ClassifyUnit> cus, ExperimentConfiguration expConfig)
			throws IOException {
		Model model;
		if (expConfig.getClassifier() instanceof SVMClassifier) {
			SVMClassifier svmC = (SVMClassifier) expConfig.getClassifier();
			svmC.buildModel(expConfig, cus);
			return null;
		}
		// build model...
		model = expConfig.getClassifier().buildModel(cus, expConfig.getFeatureConfiguration(),
				expConfig.getFeatureQuantifier(), expConfig.getDataFile());
		// store model
		// exportModel(expConfig.getModelFile(), model);
		return model;

	}

	public void exportModel(File modelFile, Model model) throws IOException {
		if (modelFile.exists()) {
			return;
		} else {
			modelFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(modelFile);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			out.writeObject(model);
			out.flush();
			out.close();
		}
	}

	

	/**
	 * Classifies the specified classify units with the specified classifier,
	 * based on the specified model
	 * 
	 * @param paragraphs
	 *            Units to classify
	 * @param classifier
	 *            Classifier to use
	 * @param model
	 *            Model to train the classifier
	 * @return A map of all classify units as key and the guessed categories as
	 *         values
	 */

	public Map<ClassifyUnit, boolean[]> classify(List<ClassifyUnit> paragraphs, ExperimentConfiguration expConfig,
			Model model) {
		ZoneAbstractClassifier classifier = (ZoneAbstractClassifier) expConfig.getClassifier();
		if (classifier instanceof SVMClassifier) {
			try {
				Map<ClassifyUnit, boolean[]> classified = ((SVMClassifier) classifier).predict(paragraphs, expConfig, stmc);
				// TODO....
				return classified;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Map<ClassifyUnit, boolean[]> classified = new HashMap<ClassifyUnit, boolean[]>();
		for (ClassifyUnit cu : paragraphs) {
			boolean[] classes = ((ZoneAbstractClassifier) classifier).classify((ZoneClassifyUnit) cu, model);
			classified.put(cu, classes);
		}
		return classified;
	}

	/**
	 * Merges the results of the second map into the first map. For every
	 * classify unit each class will be matched that matches in the first OR in
	 * the second map.
	 * 
	 * @param all
	 *            A map containg all classify units
	 * @param other
	 *            A map that should not contain more or other classify units
	 *            than the
	 * @return A map of all classify units as key and the guessed categories as
	 *         values
	 */
	public Map<ClassifyUnit, boolean[]> mergeResults(Map<ClassifyUnit, boolean[]> all,
			Map<ClassifyUnit, boolean[]> other) {
		Map<ClassifyUnit, boolean[]> toReturn = new HashMap<ClassifyUnit, boolean[]>();
		Set<ClassifyUnit> allUnits = all.keySet();
		for (ClassifyUnit classifyUnit : allUnits) {

			boolean[] otherCats = other.get(classifyUnit);
			boolean[] allCats = all.get(classifyUnit);
			boolean[] toReturnCats = new boolean[allCats.length];

			for (int i = 0; i < allCats.length; i++) {
				if (allCats[i]) {
					toReturnCats[i] = true;
				}
				if (otherCats != null && otherCats[i]) {

					toReturnCats[i] = true;
				}
			}
			toReturn.put(classifyUnit, toReturnCats);
			// for (int i = 0; i < toReturnCats.length; i++) {
			// if(toReturnCats[i] != allCats[i]){
			// System.out.println(classifyUnit.getContent());
			// System.out.println(((ZoneClassifyUnit)
			// classifyUnit).getActualClassID());
			// System.out.println("difference");
			// System.out.println("all");
			// for (int j = 0; j < toReturnCats.length; j++) {
			// System.out.print(allCats[j]+" ");
			// }
			// System.out.println();
			// System.out.println("pre");
			// for (int j = 0; j < toReturnCats.length; j++) {
			// System.out.print(otherCats[j]+" ");
			// }
			// System.out.println();
			// System.out.println("merged");
			// for (int j = 0; j < toReturnCats.length; j++) {
			// System.out.print(toReturnCats[j]+" ");
			// }
			// System.out.println();
			// }
			// }
		}
		return toReturn;
	}

	/**
	 * Translates the boolean arrays of the specified map (key and value) into
	 * possibly-multiple-true-values-boolean arrays (Short: It translates
	 * single-value-6-classes to multiple-value-4-classes)
	 * 
	 * @param untranslated
	 *            A map with boolean arrays each containing no more than one
	 *            true value (6 single-value classes)
	 * @return A map with boolean arrays each possibly containing more than one
	 *         true value (4 multiple-value classes)
	 */
	public Map<ClassifyUnit, boolean[]> translateClasses(Map<ClassifyUnit, boolean[]> untranslated) {
		Map<ClassifyUnit, boolean[]> translated = new HashMap<ClassifyUnit, boolean[]>();
		Set<ClassifyUnit> keySet = untranslated.keySet();
		for (ClassifyUnit classifyUnit : keySet) {
			boolean[] classIDs = ((ZoneClassifyUnit) classifyUnit).getClassIDs();
			int singleClassID = -1;
			for (int i = 0; i < classIDs.length; i++) {
				if (classIDs[i]) {
					singleClassID = i + 1;
				}
			}
			boolean[] multiClasses = stmc.getMultiClasses(singleClassID);
			((ZoneClassifyUnit) classifyUnit).setClassIDs(multiClasses);
			boolean[] newClassIDs = untranslated.get(classifyUnit);
			singleClassID = -1;
			for (int i = 0; i < newClassIDs.length; i++) {
				if (newClassIDs[i]) {
					singleClassID = i + 1;
				}
			}
			boolean[] newMultiClasses = stmc.getMultiClasses(singleClassID);
			translated.put(classifyUnit, newMultiClasses);
		}
		return translated;
	}


	public SingleToMultiClassConverter getStmc() {
		return stmc;
	}

}
