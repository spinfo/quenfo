package quenfo.de.uni_koeln.spinfo.information_extraction.workflow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.Pattern;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.TextToken;

@Deprecated
public class InformationEntityExport {
	
	public static void exportIEsToTXT(Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extracts) {
		
		File f = new File("information_extraction/data/competences/export_competences.txt");
		StringBuilder sb = new StringBuilder();
		for(Map.Entry<ExtractionUnit, Map<InformationEntity, List<Pattern>>> eu : extracts.entrySet()) {
			for(Map.Entry<InformationEntity, List<Pattern>> ieMap : eu.getValue().entrySet()) {
				//List<TextToken> ieTT = ieMap.getKey().getOriginialEntity();
				StringBuilder tokens = new StringBuilder();
				StringBuilder lemmas = new StringBuilder();
				StringBuilder pos = new StringBuilder();

//				for(TextToken tt : ieTT) {
//					tokens.append(tt.getToken() + " ");
//					lemmas.append(tt.getLemma() + " ");
//					pos.append(tt.getPosTag() + " ");
//				}
				sb.append(tokens.toString().trim()
						+ "|" + lemmas.toString().trim()
							+ "|" + pos.toString() + "\n");
				
//				System.out.println(ieTT);
			}
		}
		
		try {
			FileWriter fw = new FileWriter(f);
			fw.write(sb.toString());
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
