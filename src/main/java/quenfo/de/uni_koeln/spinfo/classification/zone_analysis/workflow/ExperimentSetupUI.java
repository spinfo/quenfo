package quenfo.de.uni_koeln.spinfo.classification.zone_analysis.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import quenfo.de.uni_koeln.spinfo.classification.core.classifier.AbstractClassifier;
import quenfo.de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import quenfo.de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import quenfo.de.uni_koeln.spinfo.classification.core.distance.Distance;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_weighting.AbsoluteFrequencyFeatureQuantifier;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_weighting.AbstractFeatureQuantifier;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_weighting.LogLikeliHoodFeatureQuantifier;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_weighting.RelativeFrequencyFeatureQuantifier;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_weighting.TFIDFFeatureQuantifier;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.ZoneAbstractClassifier;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.ZoneKNNClassifier;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.ZoneNaiveBayesClassifier;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.ZoneRocchioClassifier;

public class ExperimentSetupUI {
	
	String modelFileName = null;

	List<ZoneAbstractClassifier> classifiers = new ArrayList<ZoneAbstractClassifier>();
	List<AbstractFeatureQuantifier> quantifiers = new ArrayList<AbstractFeatureQuantifier>();
	List<Distance> distances = new ArrayList<Distance>();
	
	public ExperimentSetupUI() {
		classifiers.add(new ZoneKNNClassifier());
		classifiers.add(new ZoneNaiveBayesClassifier());
		classifiers.add(new ZoneRocchioClassifier());

		quantifiers.add(new LogLikeliHoodFeatureQuantifier());
		quantifiers.add(new AbsoluteFrequencyFeatureQuantifier());
		quantifiers.add(new TFIDFFeatureQuantifier());
		quantifiers.add(new RelativeFrequencyFeatureQuantifier());

		distances.add(Distance.COSINUS);
		distances.add(Distance.EUKLID);
		distances.add(Distance.MANHATTAN);
	}

	public ExperimentConfiguration getExperimentConfiguration(
			String dataFileName) throws IOException, ClassNotFoundException {	
		String outputFolderName = dataFileName.substring(0,dataFileName.indexOf("/"))+"/output";	
		Model model = null;
		AbstractClassifier classifier = null;
		AbstractFeatureQuantifier fq = null;
		FeatureUnitConfiguration fuc = null;
	//	String modelFileName = null;
		BufferedReader in = new BufferedReader(new InputStreamReader(
				System.in));
		while (true) {
			System.out.println("____________________________");
			System.out.println("create new model?");
			System.out.println();
			System.out.println("press 'y' to create a new model");
			System.out.println("press 'n' to use an existing model");	
			String answer = in.readLine();
			if (answer.toLowerCase().trim().equals("y")) {
				// create new model
				classifier = getClassifier(in);
				if (!(classifier instanceof ZoneNaiveBayesClassifier)) {
					fq = getFQ(in);
					System.out.println("you chose the "
							+ fq.getClass().getSimpleName());
				}
				fuc = getFeatureConfig(in);
				System.out.println("model created...");
				while (true) {
					System.out.println("save model?");
					System.out.println("press 'y' or 'n'");
					String save = in.readLine();

					if (save.toLowerCase().trim().equals("y")) {
						modelFileName = getModelFileName(in);
						break;
					}
					if(save.toLowerCase().trim().equals("n")){
						System.out.println("don't save model...");
						break;
					}
					 else {
						System.out
								.println("invalid answer! please try again...");
						continue;
					}

				}
				break;
			}
			if (answer.toLowerCase().equals("n")) {
				model = chooseModel(in, outputFolderName);
				if(model == null){
					getExperimentConfiguration(dataFileName);
				}
				System.out.println(model);
				classifier = model.getClassifier();
				fq = model.getFQ();
				fuc = model.getFuc();
				break;
			}
			System.out.println("invalid answer! please try again...");
			System.out.println();
			getExperimentConfiguration(dataFileName);
		}
		// get ExperimnentConfig
		if(!(classifier instanceof ZoneNaiveBayesClassifier) /*&& !(classifier instanceof SVMClassifier)*/){
			Distance dist = getDistance(in);
			classifier.setDistance(dist);
		}
		
		if (classifier instanceof ZoneKNNClassifier) {
			int k = getK(in);
			((ZoneKNNClassifier) classifier).setK(k);
		}
		ExperimentConfiguration expConfig = new ExperimentConfiguration(fuc,
				fq, classifier, new File(dataFileName), outputFolderName);
		if (modelFileName != null) {
			expConfig.setModelFileName(modelFileName);
		}
		return expConfig;
	}

	private String getModelFileName(BufferedReader in) throws IOException {
		System.out.println("enter a name for the model");
		System.out.println("e.g. 'myNewModel'");
		String answer = in.readLine();
		return answer;
		
		

	}

	private int getK(BufferedReader in) throws IOException {
		while (true) {
			System.out.println("choose a k-Value greater than 0");
			System.out.println("e.g. '4' or '6'");
			String answer = in.readLine();
			int k = -1;
			try {
				k = Integer.parseInt(answer);
			} catch (Exception e) {
				System.out.println("invalid answer! please try again...");
				continue;
			}
			if (k > 0) {
				return k;
			}
		}
	}

	private Model chooseModel(BufferedReader in, String outputFolder) throws IOException,
			ClassNotFoundException {
		

		File modelsFolder = new File(outputFolder+"/myModels");
		if(!modelsFolder.exists()){
			System.out.println("there are no existing models. Please create new model");
			return null;
		}
		File[] modelFiles = modelsFolder.listFiles();
		if(modelFiles.length == 0){
			System.out.println("there are no existing models. Please create new model");
			return null;
		}
		System.out.println("chose an existing model...");
		for (int i = 0; i < modelFiles.length; i++) {
			System.out.println("press '" + (i + 1) + "' for:   "
					+ modelFiles[i].getName().replace(".model", ""));
		}

		String answer = in.readLine();
		int modelID = -1;
		File modelFile = null;
		try {
			modelID = Integer.parseInt(answer) - 1;
			System.out.println("You picked model "
					+ modelFiles[modelID].getName());
		} catch (NumberFormatException e) {
			System.out.println("invalid answer! Please try again...");
			chooseModel(in, outputFolder);
		}
		try {
			modelFile = modelFiles[modelID];
		} catch (IndexOutOfBoundsException e) {
			System.out.println("invalid answer! Please try again...");
			chooseModel(in, outputFolder);
		}
		Model model = new Model();
		FileInputStream fis = new FileInputStream(modelFile);
		ObjectInputStream ois = new ObjectInputStream(fis);
		Object o = ois.readObject();
		model = (Model) o;
		ois.close();
		modelFileName = modelFile.getName().replace(".model", "");
		return model;
	}

	private Distance getDistance(BufferedReader in) throws IOException {
		Distance toReturn = null;
		while (true) {
			System.out.println("choose a Distance-measure...");
			System.out.println("press 1 for cosine-similarity (recommended)");
			System.out.println("press 2 for euklidean-distance ");
			System.out.println("press 3 for manhattan-distance");

			String answer = in.readLine();
			int distID = -1;
			try {
				distID = Integer.parseInt(answer) - 1;
			} catch (NumberFormatException e) {
				System.out.println("invalid answer! please try again...");
				continue;
			}
			try {
				toReturn = distances.get(distID);
				break;
			} catch (IndexOutOfBoundsException e) {
				System.out.println("invalid answer! please try again...");
				continue;
			}
		}
		return toReturn;
	}

	private FeatureUnitConfiguration getFeatureConfig(BufferedReader in)
			throws IOException {
		boolean treatEncoding = false;
		while (true) {
			System.out.println("treat encoding?");
			System.out.println("press 'y' or 'n'");
			String answer = in.readLine();
			if (answer.toLowerCase().equals("y")) {
				treatEncoding = true;
				break;
			}
			if (answer.toLowerCase().equals("n")) {
				break;
			} else {
				System.out.println("invalid answer! Try again...");
			}
		}

		boolean norm = false;
		while (true) {
			System.out.println("normalize words?");
			System.out.println("press 'y' or 'n'");
			String answer = in.readLine();
			if (answer.toLowerCase().trim().equals("y")) {
				norm = true;
				break;
			}
			if (answer.toLowerCase().trim().equals("n")) {
				break;
			} else {
				System.out.println("invalid answer! please try again...");
			}
		}
		boolean stem = false;
		while (true) {
			System.out.println("stem words?");
			System.out.println("(press 'y' or 'n')");
			String answer = in.readLine();
			if (answer.toLowerCase().trim().equals("y")) {
				stem = true;
				break;
			}
			if (answer.toLowerCase().trim().equals("n")) {
				break;
			} else {
				System.out.println("invalid answer! please try again...");
			}
		}
		boolean stopwords = false;
		while (true) {
			System.out.println("ignore stopwords?");
			System.out.println("(press 'y' or 'n')");
			String answer = in.readLine();
			if (answer.toLowerCase().trim().equals("y")) {
				stopwords = true;
				break;
			}
			if (answer.toLowerCase().trim().equals("n")) {
				break;
			} else {
				System.out.println("invalid answer! please try again...");
			}
		}
		int mi = 0;
//		while (true) {
//			System.out.println("please enter MI-score");
//			System.out.println("e.g. '0', '20', '40',..");
//			String answer = in.readLine();
//			try {
//				mi = Integer.parseInt(answer);
//			} catch (NumberFormatException e) {
//				System.out.println("invalid answer! please try again...");
//				continue;
//			}
//			if (mi < 0) {
//				System.out.println("MI-Score must be 0 or greater");
//				continue;
//			} else {
//				break;
//			}
//		}
		boolean suffixTree = false;
		while(true){
			System.out.println("use suffixtree-document-model?");
			System.out.println("(press 'y' or 'n')");
			String answer = in.readLine();
			if(answer.toLowerCase().trim().equals("y")){
				suffixTree = true;
				System.out.println("use suffixtree-document-model...");
				break;
			}
			if(answer.toLowerCase().trim().equals("n")){
				break;
			}
			else {
				System.out.println("invalid answer! please try again...");
			}
		}

		int[] nGrams = null;
		while (true) {
			System.out.println("use nGrams instead of words?");
			System.out.println("(press 'y' or 'n')");
			String answer = in.readLine();

			// no ngrams...
			if (answer.toLowerCase().trim().equals("n")) {
				System.out.println("no nGrams...");
				break;
			}

			// use nGrams
			if (answer.toLowerCase().trim().equals("y")) {
				System.out.println("use nGrams...");

				while (true) {
					System.out
							.println("please enter all NGrams seperated by '-'");
					System.out.println("e.g. '2-3-4' or '3'");

					answer = in.readLine();
					String[] split = answer.split("-");
					nGrams = new int[split.length];
					for (int i = 0; i < split.length; i++) {

						try {
							nGrams[i] = Integer.parseInt(split[i]);
						} catch (NumberFormatException e) {
							System.out
									.println("invalid answer! please try again...");
							nGrams = null;
							break;
						}
					}
					if (nGrams != null) {
						boolean contNgrams = false;
						FeatureUnitConfiguration fuc = new FeatureUnitConfiguration(
								norm, stem, stopwords, nGrams, contNgrams, mi, suffixTree);
						return fuc;
					}
				}
			} else {
				System.out.println("invalid answer! please try again...");
				continue;
			}

		}
		boolean contNgrams = false;
		System.out.println("");
		FeatureUnitConfiguration fuc = new FeatureUnitConfiguration(norm, stem,
				stopwords, nGrams, contNgrams, mi, false);
		fuc.setTreatEncoding(treatEncoding);
		return fuc;
	}

	private ZoneAbstractClassifier getClassifier(BufferedReader in)
			throws IOException {
		ZoneAbstractClassifier toReturn = null;
		while (true) {
			System.out.println("choose a classifier...");
			System.out.println();
			System.out.println("press '1' for KNNClassifier");
			System.out.println("press '2' for BayesClassifier");
			System.out.println("press '3' for RocchioClassifier");
			System.out.println("press '4' for SVM-Classifier");
			String answer = in.readLine();
			int classifierID = -1;
			try {
				classifierID = Integer.parseInt(answer) - 1;
			} catch (NumberFormatException e) {
				System.out.println("invalid answer! please try again...");
				continue;
			}

			try {
				toReturn = classifiers.get(classifierID);
				break;
			} catch (IndexOutOfBoundsException e) {
				System.out.println("invalid answer! please try again...");
				continue;
			}
		}
		System.out.println("you chose the "
				+ toReturn.getClass().getSimpleName());

		return toReturn;
	}

	private AbstractFeatureQuantifier getFQ(BufferedReader in)
			throws IOException {
		AbstractFeatureQuantifier toReturn = null;
		while (true) {
			System.out.println("choose a FeatureQuantifier...");
			System.out.println("press 1 for LogLikeliHoodQuantifier");
			System.out.println("press 2 for AbsoluteFrequencyQuantifier");
			System.out.println("press 3 for TFIDFQuantifier");
			System.out.println("press 4 for RelativeFrequencyQuantifier");
			String answer = in.readLine();
			int quantifierID = -1;
			try {
				quantifierID = Integer.parseInt(answer) - 1;
			} catch (NumberFormatException e) {
				System.out.println("invalid answer! please try again...");
				continue;
			}
			try {
				toReturn = quantifiers.get(quantifierID);
				break;
			} catch (IndexOutOfBoundsException e) {
				System.out.println("invalid answer! please try again...");
				continue;
			}
		}
		return toReturn;
	}

}
