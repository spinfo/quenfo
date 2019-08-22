package quenfo.de.uni_koeln.spinfo.classification.jasc.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import quenfo.de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import quenfo.de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import quenfo.de.uni_koeln.spinfo.classification.core.helpers.EncodingProblemTreatment;
import quenfo.de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.jasc.preprocessing.ClassifyUnitSplitter;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.RegexClassifier;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.model.ZoneKNNModel;
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

		Query count = em.createQuery("SELECT COUNT(m) FROM Model m WHERE m.configHash = :configHash");
		count.setParameter("configHash", config.hashCode());	
		long existingConfig = (long) count.getSingleResult();
		
		Model model;
		if (existingConfig == 0) {
			log.info("Create Model ... ");			
			model = createModel(config);
			
		} else {
			log.info("Load Model ... ");
			
			Query q = em.createQuery("SELECT m FROM Model m WHERE m.configHash = :configHash");
			q.setParameter("configHash", config.hashCode());
			List<Model> models = q.getResultList();
			
			model = models.get(0);
		}

		if (queryLimit < 0) // unbegrenztes QueryLimit
			queryLimit = Integer.MAX_VALUE;

		if (queryLimit < fetchSize) // QueryLimit kleiner als Fetch
			fetchSize = queryLimit;

		Query query = em.createQuery("SELECT j from JobAd j");

		List<JobAd> jobAds;

		boolean goOn = true;
		boolean askAgain = true;

		log.info("...classifying...");

		while ((startPos < queryLimit) && goOn) {
			query.setFirstResult(startPos);
			query.setMaxResults(fetchSize);

			jobAds = query.getResultList();

			if (jobAds.isEmpty())
				break;

			// log.info("Process " + jobAds.size() + " JobAds ...");

			em.getTransaction().begin();
			List<JASCClassifyUnit> result;
			for (JobAd job : jobAds) {
				result = processJobAd(job, config, model);
				for (JASCClassifyUnit cu : result)
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



	private List<JASCClassifyUnit> processJobAd(JobAd job, ExperimentConfiguration config, Model model)
			throws IOException {
		// 1. Split into paragraphs and create a ClassifyUnit per paragraph
		Set<String> paragraphs = ClassifyUnitSplitter.splitIntoParagraphs(job.getContent());
		// TODO unsplitted?

		// if treat enc
		if (config.getFeatureConfiguration().isTreatEncoding()) {
			paragraphs = EncodingProblemTreatment.normalizeEncoding(paragraphs);
		}
		List<ClassifyUnit> classifyUnits = new ArrayList<ClassifyUnit>();
		for (String string : paragraphs) {
//			paraCount++;
			//TODO JB: ZoneCU oder JASCCU??
//			classifyUnits.add(new ZoneClassifyUnit(string, job.getJahrgang(), job.getZeilenNr(), job.getJpaID()));
			classifyUnits.add(new JASCClassifyUnit(string, job.getJahrgang(), job.getZeilenNr(), job.getJpaID()));
		}

		// prepare ClassifyUnits
		classifyUnits = jobs.initializeClassifyUnits(classifyUnits);
		classifyUnits = jobs.setFeatures(classifyUnits, config.getFeatureConfiguration(), false);
		classifyUnits = jobs.setFeatureVectors(classifyUnits, config.getFeatureQuantifier(), model.getFUOrder());

		// 2. Classify
		RegexClassifier regexClassifier = new RegexClassifier("classification/data/regex.txt");
		Map<ClassifyUnit, boolean[]> preClassified = new HashMap<ClassifyUnit, boolean[]>();
		for (ClassifyUnit cu : classifyUnits) {
//			System.out.println(cu.getClass());
			boolean[] classes = regexClassifier.classify(cu, model);
			preClassified.put(cu, classes);
		}
		Map<ClassifyUnit, boolean[]> classified = jobs.classify(classifyUnits, config, model);
		classified = jobs.mergeResults(classified, preClassified);
		/*
		 * Hier ist ActualID noch richtig (-1) & [false, false, false, true]
		 */
		classified = jobs.translateClasses(classified);

		/*
		 * Hier ist ActualID falsch (4)
		 */

		List<JASCClassifyUnit> results = new ArrayList<JASCClassifyUnit>();
		//JASCClassifyUnit jcu;
		for (ClassifyUnit cu : classified.keySet()) {
			JASCClassifyUnit jcu = (JASCClassifyUnit) cu;

			boolean[] classes = classified.get(cu);
			jcu.setClassIDsAndActualClassID(classes);

			results.add(jcu);
		}

		return results;
	}
	
	
	private Model createModel(ExperimentConfiguration config) throws IOException {
		
		
		List<ClassifyUnit> trainingData = new ArrayList<ClassifyUnit>();

		// get trainingdata from file (and db)
		File trainingDataFile = new File(trainingDataFileName);

		// TODO JB: persist trainingData
		trainingData.addAll(jobs.getCategorizedParagraphsFromFile(trainingDataFile,
				config.getFeatureConfiguration().isTreatEncoding()));

		if (trainingData.size() == 0) {
			System.out.println(
					"\nthere are no training paragraphs in the specified training-DB. \nPlease check configuration and try again");
			System.exit(0);
		}
		log.info("training paragraphs: " + trainingData.size());

		trainingData = jobs.initializeClassifyUnits(trainingData);
		trainingData = jobs.setFeatures(trainingData, config.getFeatureConfiguration(), true);
		trainingData = jobs.setFeatureVectors(trainingData, config.getFeatureQuantifier(), null);

		// build model
		Model model = jobs.getNewModelForClassifier(trainingData, config);
		if (config.getModelFileName().contains("/myModels/")) {
			jobs.exportModel(config.getModelFile(), model);
		}
		em.getTransaction().begin();
		em.persist(model);
		em.getTransaction().commit();
		return model;
	}

}
