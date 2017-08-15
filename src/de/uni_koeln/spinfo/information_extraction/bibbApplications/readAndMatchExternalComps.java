package de.uni_koeln.spinfo.information_extraction.bibbApplications;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import de.uni_koeln.spinfo.information_extraction.preprocessing.IETokenizer;
import is2.data.SentenceData09;
import is2.lemmatizer.Lemmatizer;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

public class readAndMatchExternalComps {

	public static File externalComps = new File(
			"information_extraction/data/competences/Schlagwortliste_Kompetenzen.xls");

	public static File output = new File("information_extraction/data/competences/extComps.txt");

	public static void main(String[] args) throws IOException {
		// read ext. competences from file
		Set<String> comps = new HashSet<String>();
		Workbook w;
		try {
			WorkbookSettings ws = new WorkbookSettings();
			ws.setEncoding("Cp1252");
			w = Workbook.getWorkbook(externalComps, ws);

			// Get the first sheet
			Sheet sheet = w.getSheet(0);
			for (int i = 0; i < sheet.getColumns(); i++) {
				for (int j = 0; j < sheet.getColumn(i).length; j++) {
					Cell cell = sheet.getCell(i, j);
					if(cell.getContents().length() < 1){
						continue;
					}
					comps.add(cell.getContents());
				}
			}
			System.out.println(comps.size());
		} catch (BiffException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Tokenize/Lemmatize comps
		//create output File
				PrintWriter out = new PrintWriter(new FileWriter(output));
		IETokenizer tokenizer = new IETokenizer();
		is2.tools.Tool lemmatizer = new Lemmatizer(
					"information_extraction/data/sentencedata_models/ger-tagger+lemmatizer+morphology+graph-based-3.6/lemma-ger-3.6.model");
		for (String string : comps) {
			SentenceData09 sd = new SentenceData09();
			sd.init(tokenizer.tokenizeSentence("<root> "+string));
			lemmatizer.apply(sd);
			StringBuffer sb = new StringBuffer();
			for (String lemma : sd.plemmas) {
				sb.append(lemma+" ");
			}
			String lemmas = sb.toString().substring(13,sb.toString().length()-1);
			if( lemmas.length() < 3 || lemmas.startsWith("--")){
				System.out.println(lemmas+"          "+ string);
			}
			else{
				out.write(lemmas+"\n");
			}
			
			
			
		}
		
		
		
	}
}
