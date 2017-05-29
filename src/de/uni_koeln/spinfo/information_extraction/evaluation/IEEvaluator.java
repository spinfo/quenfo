package de.uni_koeln.spinfo.information_extraction.evaluation;

import java.util.List;
import java.util.Map;

import de.uni_koeln.spinfo.information_extraction.data.Context;
import de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import de.uni_koeln.spinfo.information_extraction.data.Token;

/**
 * @author geduldia
 * 
 * A Class to evaluate IE-results
 *
 */
public class IEEvaluator {

	/**
	 * compares the extracted Information Entities with the annotated Entities and returns the EvaluationResult
	 * @param allExtractions
	 * @param trainingdata
	 * @return EvaluationResult
	 */
	public EvaluationResult evaluateIEResults(Map<ExtractionUnit, Map<InformationEntity, List<Context>>> allExtractions,
			Map<ExtractionUnit, List<String>> trainingdata) {
		
		EvaluationResult result = new EvaluationResult();
		
		for (ExtractionUnit ieunit : trainingdata.keySet()) {
		//	System.out.println("\n"+ieunit.getSentence());
			List<String> anchors = trainingdata.get(ieunit);
			for (String string : anchors) {
		//		System.out.println("- "+string);
			}
			if(allExtractions.keySet().contains(ieunit)){
				for (InformationEntity ie : allExtractions.get(ieunit).keySet()) {
					boolean tp = false;
					for (String anchor : anchors) {
						if(ie.getFullExpression().contains(anchor)){
					//		System.out.println("TP: "+ ie);
							tp = true;
							break;
						}
//						if(anchor.contains(ie.getFullExpression())){
//							System.out.println("TP2: "+ie);
//							tp = true;
//							break;
//						}
					}
					if(tp){
						result.addTP(ie, ieunit);
					}
					else{
						if(allExtractions.get(ieunit).get(ie).size() >= 1){
					//		System.out.println("FP2: "+ ie);
							if(allExtractions.get(ieunit).get(ie).size()>0){
				//				System.out.println(allExtractions.get(ieunit).get(ie).get(0).getDescription());
							}
						}
						else{
					//		System.out.println("FP: "+ie);
							if(allExtractions.get(ieunit).get(ie).size()>0){
						//		System.out.println(allExtractions.get(ieunit).get(ie).get(0).getDescription());
							}
					}
						result.addFP(ie, ieunit);
					}
				}
				for (String anchor : anchors) {
					boolean fn = true;
					for (InformationEntity ie : allExtractions.get(ieunit).keySet()) {
						if(ie.getFullExpression().contains(anchor)){
							fn = false;
							break;
						}
					}
					if(fn){
						//System.out.println("FN: "+ anchor);
						result.addFN(anchor, ieunit);
					}
				}
			}
			else{
				for (String string : anchors) {
					//System.out.println("FN: "+ string);
					result.addFN(string, ieunit);
				}
			}
			
		}
		
		return result;	
	}

}

