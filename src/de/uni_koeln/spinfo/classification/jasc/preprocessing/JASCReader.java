package de.uni_koeln.spinfo.classification.jasc.preprocessing;

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import jxl.Cell;
import jxl.CellType;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

/**
 * Reads Excel files with job ads
 * @author jhermes
 *
 */
public class JASCReader {

	/** Extracts the job ads from the specified Excel File.
	 * @param jobAdFile Excel file from BIBB
	 * @return Map of all Texts (id/content)
	 * @throws IOException
	 */
	public SortedMap<Integer, String> getJobAds(File jobAdFile) throws IOException {
		SortedMap<Integer, String> toReturn = new TreeMap<Integer, String>();
		Workbook w;
		try {
			WorkbookSettings ws = new WorkbookSettings();
			ws.setEncoding("Cp1252");
			w = Workbook.getWorkbook(jobAdFile, ws);
			
			// Get the first sheet
			Sheet sheet = w.getSheet(0);
			for (int i = 0; i < sheet.getRows(); i++) {
				//TODO read text ids from file
				
				Cell idCell = sheet.getCell(0,i);
				CellType type = idCell.getType();
				int id =0;
				if(type == CellType.NUMBER){
					id = Integer.parseInt(idCell.getContents());
				}
				else{
					System.out.println("No ID Found");
				}
				Cell cell = sheet.getCell(1, i);
				type = cell.getType();
				if (type == CellType.LABEL) {
					toReturn.put(id, cell.getContents().replaceAll("#", "\\_"));
				}
			}
		} catch (BiffException e) {
			e.printStackTrace();
		}
		
		return toReturn;
	}

}
