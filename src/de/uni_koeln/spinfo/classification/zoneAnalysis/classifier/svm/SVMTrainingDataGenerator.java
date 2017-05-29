package de.uni_koeln.spinfo.classification.zoneAnalysis.classifier.svm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.zoneAnalysis.data.ZoneClassifyUnit;

public class SVMTrainingDataGenerator {

	public void writeData(List<ClassifyUnit> cus, File outputFile) throws IOException {
		
		PrintWriter out = new PrintWriter(new FileWriter(outputFile));
		
			
			
				for (ClassifyUnit cu : cus) {
					int label = ((ZoneClassifyUnit) cu).getActualClassID();
					out.print(label+" ");
					double[] vec = cu.getFeatureVector();
					for(int d = 1; d <=vec.length; d++){
						out.print(d + ":" + vec[d - 1]+" ");
						
					}
					out.print("\n");
				}
				out.flush();
				out.close();
					
		
	}
}
