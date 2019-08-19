package quenfo.de.uni_koeln.spinfo.classification.jasc.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.log4j.Logger;

import quenfo.de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import quenfo.de.uni_koeln.spinfo.classification.core.helpers.EncodingProblemTreatment;
import quenfo.de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.jasc.preprocessing.ClassifyUnitSplitter;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.RegexClassifier;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.data.ZoneClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.helpers.SingleToMultiClassConverter;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.workflow.ZoneJobs;
import quenfo.de.uni_koeln.spinfo.core.data.JobAd;

public class DerbyDBClassifier {

	private Logger log = Logger.getLogger(DerbyDBClassifier.class);

	private String trainingDataFileName;
	private ZoneJobs jobs;

	private int queryLimit, fetchSize, startPos;

	private EntityManager em;

	public DerbyDBClassifier(int queryLimit, int fetchSize, int startPos, String trainingDataFileName, EntityManager em)
			throws IOException {

		this.queryLimit = queryLimit;
		this.fetchSize = fetchSize;
		this.startPos = startPos;

		this.trainingDataFileName = trainingDataFileName;

		this.em = em;

		// set Translations
		Map<Integer, List<Integer>> translations = new HashMap<Integer, List<Integer>>();
		List<Integer> categories = new ArrayList<Integer>();
		categories.add(1);
		categories.add(2);
		translations.put(5, categories);
		categories = new ArrayList<Integer>();
		categories.add(2);
		categories.add(3);
		translations.put(6, categories);
		SingleToMultiClassConverter stmc = new SingleToMultiClassConverter(6, 4, translations);

		jobs = new ZoneJobs(stmc);
	}

	@SuppressWarnings("unchecked")
	public void classify(ExperimentConfiguration config) throws IOException {
		// get trainingdata from file (and db)
		File trainingDataFile = new File(trainingDataFileName);
		List<ClassifyUnit> trainingData = new ArrayList<ClassifyUnit>();
		//TODO JB: persist trainingData
		trainingData.addAll(jobs.getCategorizedParagraphsFromFile(trainingDataFile,
				config.getFeatureConfiguration().isTreatEncoding()));

		if (trainingData.size() == 0) {
			System.out.println(
					"\nthere are no training paragraphs in the specified training-DB. \nPlease check configuration and try again");
			System.exit(0);
		}
		log.info("training paragraphs: " + trainingData.size());
		log.info("...classifying...");

		trainingData = jobs.initializeClassifyUnits(trainingData);
		trainingData = jobs.setFeatures(trainingData, config.getFeatureConfiguration(), true);
		trainingData = jobs.setFeatureVectors(trainingData, config.getFeatureQuantifier(), null);

		// build model
		Model model = jobs.getNewModelForClassifier(trainingData, config);
		if (config.getModelFileName().contains("/myModels/")) {
			jobs.exportModel(config.getModelFile(), model);
		}

		if (queryLimit < 0)
			queryLimit = Integer.MAX_VALUE;

		Query query = em.createQuery("SELECT j from JobAd j"); //TODO query JobAds

		List<JobAd> jobAds;

		boolean goOn = true;
		boolean askAgain = true;

		while ((startPos < queryLimit) && goOn) {
			query.setFirstResult(startPos);
			query.setMaxResults(fetchSize);

			jobAds = query.getResultList();

			if (jobAds.isEmpty())
				break;

			// TODO process batch
			em.getTransaction().begin();
			for (JobAd job : jobAds) {
				List<ClassifyUnit> result = processJobAd(job, config, model);
				for (ClassifyUnit cu : result)
					em.persist(cu);
			}
			em.getTransaction().commit();

			if (askAgain) {

				System.out.println(
						"\n\n" + "continue (c),\n" + "don't interrupt again (d),\n" + "or stop (s) classifying?");

				boolean answered = false;
				while (!answered) {
					BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
					String answer = in.readLine();

					if (answer.toLowerCase().trim().equals("c")) {
						goOn = true;
						answered = true;
						log.info("...classifying...");
					} else if (answer.toLowerCase().trim().equals("d")) {
						goOn = true;
						askAgain = false;
						answered = true;
						log.info("...classifying...");
					} else if (answer.toLowerCase().trim().equals("s")) {
						goOn = false;
						answered = true;
					} else {
						System.out.println("C: invalid answer! please try again...");
						System.out.println();
					}
				}
			}

			startPos += jobAds.size();
		}

	}

	private List<ClassifyUnit> processJobAd(JobAd job, ExperimentConfiguration config, Model model) throws IOException {
		// 1. Split into paragraphs and create a ClassifyUnit per paragraph
		Set<String> paragraphs = ClassifyUnitSplitter.splitIntoParagraphs(job.getContent());
		// TODO unsplitted?

		// if treat enc
		if (config.getFeatureConfiguration().isTreatEncoding()) {
			paragraphs = EncodingProblemTreatment.normalizeEncoding(paragraphs);
		}
		List<ClassifyUnit> classifyUnits = new ArrayList<ClassifyUnit>();
		for (String string : paragraphs) {
//						paraCount++;
			classifyUnits.add(new JASCClassifyUnit(string, job.getJahrgang(), job.getZeilenNr()));
		}
		// prepare ClassifyUnits
		classifyUnits = jobs.initializeClassifyUnits(classifyUnits);
		classifyUnits = jobs.setFeatures(classifyUnits, config.getFeatureConfiguration(), false);
		classifyUnits = jobs.setFeatureVectors(classifyUnits, config.getFeatureQuantifier(), model.getFUOrder());

		// 2. Classify
		RegexClassifier regexClassifier = new RegexClassifier("classification/data/regex.txt");
		Map<ClassifyUnit, boolean[]> preClassified = new HashMap<ClassifyUnit, boolean[]>();
		for (ClassifyUnit cu : classifyUnits) {
			boolean[] classes = regexClassifier.classify(cu, model);
			preClassified.put(cu, classes);
		}
		Map<ClassifyUnit, boolean[]> classified = jobs.classify(classifyUnits, config, model);
		classified = jobs.mergeResults(classified, preClassified);
		classified = jobs.translateClasses(classified);

		List<ClassifyUnit> results = new ArrayList<ClassifyUnit>();
		for (ClassifyUnit cu : classified.keySet()) {
			((ZoneClassifyUnit) cu).setClassIDs(classified.get(cu));
//						 System.out.println();
//						 System.out.println(cu.getContent());
//						 System.out.print("-----> CLASS: ");
			boolean[] ids = ((ZoneClassifyUnit) cu).getClassIDs();
			boolean b = false;
			for (int i = 0; i < ids.length; i++) {
				if (ids[i]) {
//					if (b) {
////									 System.out.print("& " + (i + 1));
//					} else {
////									 System.out.println((i + 1));
//					}
					b = true;
				}
			}

			results.add(cu);
		}
		
		return results;
	}

}
