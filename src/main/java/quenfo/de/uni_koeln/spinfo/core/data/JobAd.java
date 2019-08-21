package quenfo.de.uni_koeln.spinfo.core.data;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(of = {"zeilenNr"}) //TODO JB: equals auf jpaID?
@ToString(of = {"zeilenNr"})
public class JobAd {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long jpaID;
	
	@Lob
	private String content;
	private int zeilenNr;
	private int jahrgang;
	
	/**
	 * speichert mit welchen Konfigurationen vorverarbeitet wurde
	 */
	private String preprocessingConfig;
	
	private List<String> featureUnits;
	
	private double[] featureVector;
	
	//private Vector sparseVector;
	
	
	public JobAd() {
		
	}
	
	public JobAd(String content, int zeilenNr, int jahrgang) {
		this.content = content;
		this.zeilenNr = zeilenNr;
		this.jahrgang = jahrgang;
	}
	

}
