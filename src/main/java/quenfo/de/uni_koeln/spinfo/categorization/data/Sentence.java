package quenfo.de.uni_koeln.spinfo.categorization.data;

import quenfo.de.uni_koeln.spinfo.categorization.workflow.Cat_Jobs;

public class Sentence {
	
	String id;
	String lemmata;
	String trimmed;
	
	public String getTrimmed() {
		return trimmed;
	}

	public void setTrimmed(String trimmed) {
		this.trimmed = trimmed;
	}

	public Sentence(String id){
		this.id = id;
	}
	
	public Sentence(String id, String lemmata){
		this.id = id;
		this.lemmata = Cat_Jobs.normalizeSentence(lemmata);
	}
	
	
	@Override
	public boolean equals(Object o){
		Sentence s = (Sentence) o;
		return s.id.equals(this.id);
	}

	public String getLemmata() {
		return lemmata;
	}

	public void setLemmata(String lemmata) {
		this.lemmata = Cat_Jobs.normalizeSentence(lemmata);
	}

	public String getId() {
		return id;
	}
	
	@Override
	public int hashCode(){
		return id.hashCode();
	}
	
	
	

}
