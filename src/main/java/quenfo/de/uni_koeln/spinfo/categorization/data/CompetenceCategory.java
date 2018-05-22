package quenfo.de.uni_koeln.spinfo.categorization.data;


/**
 * @author geduldia
 * 
 * Kategorie für Komptenzen. Richtet sich nach dem Kategorienbaum der AMS
 * 
 * - firstLevelCategorie (z.B. 'Zertifikate und Abschlüsse')
 * - secondLevelategory (z.B. 'Kunst Kultur und Medien' - )
 * - thirdLevelCategore (z.B. 'Logistik-Kenntnisse' - noch feinere Unterteilung von AMS. Zur Evaluation und Kategorisierung wird aber die SecondLevelCategory verwendet)
 * 
 */
public class CompetenceCategory extends Category{
	
	private String thirdLevelCategory;

	public CompetenceCategory(String firstLevelCat, String secondLevelCat){
		super(firstLevelCat, secondLevelCat);
	}
	

	public CompetenceCategory(String firstLevelCat, String secondLevelCat, String thirdLevelCategory){
		super(firstLevelCat, secondLevelCat);
		this.thirdLevelCategory = thirdLevelCategory;
	}

	
	public String getThirdLevelCategory() {
		return thirdLevelCategory;
	}


	public void setThirdLevelCategory(String thirdLevelCategory) {
		this.thirdLevelCategory = thirdLevelCategory;
	}
	
	@Override
	public String toString(){
		return super.toString()+"\nthirdLevel: " + thirdLevelCategory;
	}
	
	@Override
	public boolean equals(Object o){
		return ((Category) o).getsecondLevelCategory().equals(this.getsecondLevelCategory());
	}
}
