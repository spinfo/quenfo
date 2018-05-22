package quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.svm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import quenfo.de.uni_koeln.spinfo.classification.core.classifier.model.Model;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import quenfo.de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_weighting.AbstractFeatureQuantifier;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.classifier.ZoneAbstractClassifier;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.data.ZoneClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.zone_analysis.helpers.SingleToMultiClassConverter;

public class SVMClassifier extends ZoneAbstractClassifier {
	

	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Map<ClassifyUnit, boolean[]> predict(List<ClassifyUnit> cus, ExperimentConfiguration expConfig, SingleToMultiClassConverter stmc) throws IOException {
		
		//create svmTestDataFile
		String testFileName = "classification/svm/testFile";
		File dir = new File("classification/svm");
		if(!dir.exists()){
			dir.mkdirs();
		}
		File testFile = new File(testFileName);
		if (!testFile.exists()) {
			testFile.createNewFile();
		}
		SVMTrainingDataGenerator tdg = new SVMTrainingDataGenerator();
		tdg.writeData(cus, testFile);	
		
		String outputFileName ="classification/svm/predictions";
		String modelFileName = expConfig.getModelFileName();
		String[] argv = new String[3];
		argv[0] = testFileName;
		argv[1] = modelFileName;
		argv[2] = outputFileName;
		
		//write outputFile
		svm_predict.main(argv);
		
		Map<ClassifyUnit, boolean[]> classified = new HashMap<ClassifyUnit, boolean[]>();
		
		//read and interpret outputFile
		BufferedReader in = new BufferedReader(new FileReader(new File(outputFileName)));
		int numberOfClasses = ((ZoneClassifyUnit) cus.get(0)).getClassIDs().length;
		String line = in.readLine();
		int i = 0;
		while(line != null){
			int label = Integer.parseInt(line.split("\\.")[0]);
			boolean[] classes = stmc.getMultiClasses(label);
//			boolean[] classes = new boolean[numberOfClasses];
//			classes[label-1]  = true;
			classified.put(cus.get(i), classes);
			i++;
			line = in.readLine();
		}
		in.close();
		return classified;
	}

	
	public void buildModel(ExperimentConfiguration expConfiguration, List<ClassifyUnit> cus) throws IOException{
		
//		File modelFile = new File(expConfiguration.getModelFileName());
//		if(modelFile.exists()){
//			return ;
//		}
		File dir = new File("classification/svm");
		if(!dir.exists()){
			dir.mkdirs();
		}
		File dataFile = new File("classification/svm/trainingData");
		if(!dataFile.exists()){
			dataFile.createNewFile();
		}

			SVMTrainingDataGenerator tdg = new SVMTrainingDataGenerator();
			try {
				tdg.writeData(cus, dataFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		String[] argv = new String[3];
		argv[0] = "-q";
		argv[1] = dataFile.getPath();
		argv[2] = expConfiguration.getModelFileName();
		try {
			svm_train.main(argv);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
	@Override
	public boolean[] classify(ClassifyUnit cu, Model model) {

		return null;
	}

	@Override
	public Model buildModel(List<ClassifyUnit> cus,
			FeatureUnitConfiguration fuc, AbstractFeatureQuantifier fq,
			File dataFile) {
		return null;
	}

}
