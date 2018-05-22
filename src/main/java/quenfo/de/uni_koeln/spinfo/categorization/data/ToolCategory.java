package quenfo.de.uni_koeln.spinfo.categorization.data;

/**
 * @author geduldia
 *
 *Kategorie für Tools. Richtet sich nach dem Kategorienbaum vom BIBB.
 * firstLevelCategory:  z.B. 1 = Werkzeuge oder 2 =  Maschinen
 * secondLevelCategory: z.B. ' A = angetriebene Handwerkzeuge' B = 'feinmechanische oder Spezial-Handwerkzeuge und Instrumente'
 * 
 * In Gegensatz zur CompetenceCategorie gibt es keine feinere Gliederung mehr als die SecondLevelCategory

 * shortkey: z.B. 1A oder 2B
 */
public class ToolCategory extends Category{
	
	private String shortKey;
	
	public ToolCategory(String firstLevelCategory, String secondLevelCategory, String shortKey) {
		super(firstLevelCategory, secondLevelCategory);
		this.shortKey = shortKey;
	}
	
	@Override
	public boolean equals(Object o){
		if(((Category)o).getfirstLevelCategory().equals(this.getfirstLevelCategory())){
			if(((ToolCategory)o).getShortKey().equals(this.shortKey)){
				if(((Category)o).getsecondLevelCategory().equals(this.getsecondLevelCategory()));
				return true;
			}
		}
		return false;
	}



	public String getShortKey() {
		return shortKey;
	}

	public void setShortKey(String shortKey) {
		this.shortKey = shortKey;
	}
}
