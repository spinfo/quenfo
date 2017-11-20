package de.uni_koeln.spinfo.grouping.weka;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class ArffFileCreator {

	public static void createArffFile(Map<String, double[]> vectors, File file) throws IOException{
		// create Arff-file
				PrintWriter out = new PrintWriter(new FileWriter(new File("clustering/vectors.arff")));
				out.write("@RELATION comps\n\n");
				for (int d = 0; d < vectors.values().iterator().next().length; d++) {
					out.write("@ATTRIBUTE dim" + d + " NUMERIC\n");
				}
				out.write("\n");
				out.write("@DATA\n");
				for (String comp : vectors.keySet()) {
					double[] vec = vectors.get(comp);
					for (int dim = 0; dim < vec.length; dim++) {
						out.write(", " + vec[dim]);
					}
					out.write("\n");
				}
				out.close();
	}

}
