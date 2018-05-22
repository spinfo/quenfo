package quenfo.de.uni_koeln.spinfo.categorization.data;

/**
 * @author geduldia
 *
 *Abstrakte Klasse für die Kategorie einer Entität (Tool oder Kompetenz)
 *bestehend aus firstLevelkategorie (= übergeordneter Bereich) und einer secondLevelCategorie (= Hauptkategorie, nach der auch evaluiert wird)
 *
 */
public abstract class Category {
	
	private String secondLevelCategory;
	private String firstLevelCategory;
	
	public Category(String firstLevelCategory, String secondLevelCategory){
		this.firstLevelCategory = firstLevelCategory;
		this.secondLevelCategory = secondLevelCategory;
	}
	
	public String getfirstLevelCategory() {
		return firstLevelCategory;
	}

	public void setFirstLevelCategory(String firstLevelCategory) {
		this.firstLevelCategory = firstLevelCategory;
	}

	public String getsecondLevelCategory() {
		return secondLevelCategory;
	}

	public void setSecondLevelCategory(String secondLevelCategory) {
		this.secondLevelCategory = secondLevelCategory;
	}
	
	@Override
	public String toString(){
		return "firstLevel: "+firstLevelCategory+"\nsecondLevel: "+secondLevelCategory;
	}
	
	@Override
	public boolean equals(Object o){
		Category c = (Category) o;
		if(c.getfirstLevelCategory().equals(this.getfirstLevelCategory())){
			return c.getsecondLevelCategory().equals(this.getsecondLevelCategory());
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		String s = firstLevelCategory+secondLevelCategory;
		return s.hashCode();
	}
	
	
}
