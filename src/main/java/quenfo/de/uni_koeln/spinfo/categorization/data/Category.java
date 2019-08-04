package quenfo.de.uni_koeln.spinfo.categorization.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author geduldia
 *
 *Abstrakte Klasse für die Kategorie einer Entität (Tool oder Kompetenz)
 *bestehend aus firstLevelkategorie (= übergeordneter Bereich) und einer secondLevelCategorie (= Hauptkategorie, nach der auch evaluiert wird)
 *
 */
@Data
@EqualsAndHashCode(of = {"firstLevelCategory", "secondLevelCategory"})
@AllArgsConstructor
public abstract class Category {
	
	private String secondLevelCategory;
	private String firstLevelCategory;
	
//	public Category(String firstLevelCategory, String secondLevelCategory){
//		this.firstLevelCategory = firstLevelCategory;
//		this.secondLevelCategory = secondLevelCategory;
//	}
//	
//	public String getFirstLevelCategory() {
//		return firstLevelCategory;
//	}
//
//	public void setFirstLevelCategory(String firstLevelCategory) {
//		this.firstLevelCategory = firstLevelCategory;
//	}
//
//	public String getSecondLevelCategory() {
//		return secondLevelCategory;
//	}
//
//	public void setSecondLevelCategory(String secondLevelCategory) {
//		this.secondLevelCategory = secondLevelCategory;
//	}
//	
//	@Override
//	public String toString(){
//		return "firstLevel: "+firstLevelCategory+"\nsecondLevel: "+secondLevelCategory;
//	}
//	
//	@Override
//	public boolean equals(Object o){
//		Category c = (Category) o;
//		if(c.getFirstLevelCategory().equals(this.getFirstLevelCategory())){
//			return c.getSecondLevelCategory().equals(this.getSecondLevelCategory());
//		}
//		return false;
//	}
//	
//	@Override
//	public int hashCode(){
//		String s = firstLevelCategory+secondLevelCategory;
//		return s.hashCode();
//	}
	
	
}
