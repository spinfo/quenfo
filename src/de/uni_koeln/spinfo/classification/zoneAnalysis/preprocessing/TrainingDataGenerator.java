package de.uni_koeln.spinfo.classification.zoneAnalysis.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.jasc.data.JASCClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;

/**
 * Class to annotate ClassifyUnits manually with (one or more) classIDs
 * 
 * @author jhermes, geduldig
 * 
 */
public class TrainingDataGenerator {

	private File tdFile;
	private List<ClassifyUnit> classifiedData;
	private int numberOfSingleClasses = 0;
	int deletions;
//	private int numberOfMulticlasses = 4;

	/**
	 * Instanciates a new TrainingDataGenerator corresponding to the specified
	 * file.
	 * 
	 * @param trainingDataFile
	 *            File for trained data
	 */
	public TrainingDataGenerator(File trainingDataFile) {
		this.tdFile = trainingDataFile;
		classifiedData = new ArrayList<ClassifyUnit>();
	}

	


	/**
	 * @param trainingDataFile
	 * @param categories number of categories
	 * @param classes number of classes
	 * @param translations translations from classes to categories
	 */
	public TrainingDataGenerator(File trainingDataFile, int categories, int classes, Map<Integer, List<Integer>> translations) {
		this.tdFile = trainingDataFile;
		classifiedData = new ArrayList<ClassifyUnit>();
		this.numberOfSingleClasses = classes;
		JASCClassifyUnit.setNumberOfCategories(categories, classes, translations);
	}

	/**
	 * Returns trained (manually annotated) data from training data file.
	 * 
	 * @return List of manually annotated ClassifyUnits
	 * @throws IOException
	 */
	public List<ClassifyUnit> getTrainingData() throws IOException {
		if (classifiedData.isEmpty()) {
			classifiedData = new ArrayList<ClassifyUnit>();
			BufferedReader in = new BufferedReader(new FileReader(tdFile));
			String line = in.readLine();
			if(line != null && line.startsWith("deletions:")){
				deletions = Integer.parseInt(line.split(":")[1]);
				line = in.readLine();
			}	
			StringBuffer content = new StringBuffer();
			int parentID = 0;
			int secondParentID = 0;
			UUID paragraphID = null;
			int classID = 0;
			while (line != null) {
				String[] splits = line.split("\t");
				if (splits.length == 3 || splits.length == 2) {

					if (/** classes.length **/
					classID != 0) {
						ZoneClassifyUnit utc = null;
						if(splits.length == 3){
							utc = new JASCClassifyUnit(content.toString(), parentID,secondParentID, paragraphID);
						}
						else{
							utc = new ZoneClassifyUnit(content.toString(),paragraphID);
						}
					
						utc.setActualClassID(classID);
						classifiedData.add(utc);
					}
					paragraphID = UUID.fromString(splits[0]);
					if(splits.length == 3){
						String[] parentIDs = splits[1].split("-");
						parentID = Integer.parseInt(parentIDs[0]);
						if(parentIDs.length == 2){
							secondParentID = Integer.parseInt(parentIDs[1]);
						}
						classID = Integer.parseInt(splits[2]);
					}
					else{
						classID = Integer.parseInt(splits[1]);
					}
					content = new StringBuffer();
					

				} else {
					content.append(line + "\n");
				}
				line = in.readLine();

			}
			if (/** classes.length **/
			classID != 0) {
				JASCClassifyUnit utc = new JASCClassifyUnit(content.toString(),
						parentID, secondParentID, paragraphID);
				utc.setActualClassID(classID);
				classifiedData.add(utc);
			}
			in.close();
		}
		return classifiedData;
	}

	private int getSingleClassAnnotation(String answer) throws Exception {
		int category = 0;
		try {
			category = Integer.parseInt(answer);
			if (category == 0 || category > numberOfSingleClasses) {
				throw new Exception();
			}

			return category;
		} catch (Exception e) {
			throw new Exception();
		}

	}

	/**
	 * @param dataList
	 * @throws IOException
	 */
	public void annotate(List<ClassifyUnit> dataList) throws IOException {
		
		getTrainingData();
		System.out.println("Training Data Size: " + classifiedData.size());
		
		
		int start = classifiedData.size()+deletions;
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		// Anzahl der Kategorien festlegen

		System.out
				.println("Geben Sie die Zahl der unterschiedlichen Klassen an");

		while (numberOfSingleClasses == 0) {
			String answer = in.readLine();
			try {
				numberOfSingleClasses = Integer.parseInt(answer);
				if (numberOfSingleClasses == 0) {
					System.out.println("invalid number. try again...");
					continue;
				}
				if (numberOfSingleClasses < 0) {
					numberOfSingleClasses = 0;
					System.out.println("invalid number. try again...");
					continue;
				}

			} catch (ClassCastException e) {
				continue;
			}
		}
		// annotate...
		for (int i = start; i < dataList.size(); i++) {
			JASCClassifyUnit currentCU = (JASCClassifyUnit) dataList.get(i);
			int classID = 0;
			System.out.println(currentCU.getContent());
			String answer = in.readLine();
			if (answer.equals("stop")) {
				break;
			}
			if (answer.equals("b")) {
				classifiedData.remove(classifiedData.size() - 1);
				i--;
				i--;
				continue;
			}
			if(answer.equals("d")){
				deletions++;
				continue;
			}
			// single class annotation
			try {
				classID = getSingleClassAnnotation(answer);
			} catch (Exception e) {
				System.out.println("invalid category. please try again...");
				i--;
				continue;
			}
			currentCU.setActualClassID(classID);
			classifiedData.add(currentCU);
		}
		writeTrainingDataFile(classifiedData);
	}


	/**
	 * @param newTrainingDataFile
	 * @param badDataFile
	 * @throws IOException
	 */
	public void filterUglyTrainingData(File newTrainingDataFile, File badDataFile) throws IOException {
		getTrainingData();
		List<ClassifyUnit> goodData = new ArrayList<ClassifyUnit>();
		List<ClassifyUnit> badData = new ArrayList<ClassifyUnit>();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		for (int i = 0; i < classifiedData.size(); i++) {
			ClassifyUnit cu = classifiedData.get(i);
			System.out.println(cu.getContent());
			System.out.println(((JASCClassifyUnit) cu).getActualClassID());
			String answer = in.readLine();
			if (answer.equals("y")) {
				goodData.add(cu);
			} else {
				if (answer.equals("n")) {
					badData.add(cu);

				} else {
					if (answer.equals("b")) {
						goodData.remove(goodData.size() - 1);
						i--;
						i--;
						continue;
					}
					if (answer.equals("stop")) {
						break;
					}
					System.out.println("Try again");
					i--;
					continue;
				}
			}
		}
		writeTrainingDataFile(newTrainingDataFile, goodData);
		writeTrainingDataFile(badDataFile, badData);
	}
	
	public void writeTrainingDataFile(List<ClassifyUnit> toWrite) throws IOException{
		writeTrainingDataFile(tdFile, toWrite);
	}

	/**
	 * @param trainingDataFile
	 * @param toWrite
	 * @throws IOException
	 */
	public void writeTrainingDataFile(File trainingDataFile, List<ClassifyUnit> toWrite) throws IOException{
		if(! trainingDataFile.exists() ){
			trainingDataFile.createNewFile();
		}
		while(true){
			try{
				PrintWriter out = new PrintWriter(new FileWriter(trainingDataFile));
				out.write("deletions:"+deletions+"\n");
				for (ClassifyUnit unitToClassify : toWrite) {
					out.print(unitToClassify.getID() + "\t");
					if(unitToClassify instanceof JASCClassifyUnit){
						out.print(((JASCClassifyUnit) unitToClassify).getParentID() +"-"+((JASCClassifyUnit) unitToClassify).getSecondParentID()+"\t");
					}
					out.print(( (ZoneClassifyUnit) unitToClassify).getActualClassID()+"\n");
					out.println(unitToClassify.getContent().trim().replaceAll("\t", " "));
					out.println();
				}
				out.flush();
				out.close();
				break;
			}
			catch(IOException e){
				e.printStackTrace();
				System.out.println("Auf die Datei kann nicht geschrieben werden, weil ein anderer Prozess darauf zugreift.");
				System.out.println("Bitte Prozess schließen und zur Bestätigung ENTER drücken.");		
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				try {
					in.readLine();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}
