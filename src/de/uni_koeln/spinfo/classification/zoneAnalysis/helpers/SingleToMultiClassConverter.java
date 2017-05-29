package de.uni_koeln.spinfo.classification.zoneAnalysis.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SingleToMultiClassConverter {
	
	private int classesCount;
	private int categoriesCount;
	private Map<Integer, List<Integer>> translations;
	
	public int getNumberOfClasses(){
		return classesCount;
	}
	
	public int getNumberOfCategories(){
		return categoriesCount;
	}
	
	public SingleToMultiClassConverter(int classesCount, int categoriesCount, Map<Integer, List<Integer>> translations) {
		super();
		this.classesCount = classesCount;
		this.categoriesCount = categoriesCount;
		this.translations = translations;
		validate();
	}
	
	private void validate(){
		if(classesCount == categoriesCount)
			return;
		if(classesCount < categoriesCount){
			reportTranslationError(categoriesCount);
		}
		for(int i=categoriesCount+1; i<=classesCount; i++){
			List<Integer> list = translations.get(i);
			if(list==null){
				reportTranslationError(i);
				return;
			}
			for (Integer classID : list) {
				if(classID<1||classID>categoriesCount){
					reportTranslationError(i);
				}
			}
		}
	}
	
	private void reportTranslationError(int classID){
		System.err.println("Your class/category translation is not well defiened. Please check your code. Error in class " + classID);
	}
	
	public int getSingleClass(boolean[] classes){
		List<Integer> classList = new ArrayList<Integer>();
		for (int i = 0; i < classes.length;i++) {
			if(classes[i]){
				classList.add(i+1);
			}
		}
		if(classList.size()==1){
			return classList.get(0);
		}
		if(classList.size()==2){
			for (int c : translations.keySet()) {
				if(translations.get(c).equals(classList)){
					return c;
				}
			}
		}
		return -1;
	}

	public boolean[] getMultiClasses(int singleClassID){
		
		if(singleClassID==7){
			singleClassID=6;
		}
		if(singleClassID > classesCount){
			reportTranslationError(classesCount);
		}
		boolean[] toReturn = new boolean[categoriesCount];
		
		if(singleClassID==-1){
			toReturn[toReturn.length-1]= true;
			return toReturn;
		}
		
		if(singleClassID<=toReturn.length){
			toReturn[singleClassID-1] = true;
		}
		else{			
			List<Integer> classesList = translations.get(singleClassID);
			for (Integer actualClass : classesList) {
				toReturn[actualClass-1] = true;
			}
		}
		
		return toReturn;
	}
	
	public Map<Integer, List<Integer>> getTranslations(){
		return translations;
	}
	

}
