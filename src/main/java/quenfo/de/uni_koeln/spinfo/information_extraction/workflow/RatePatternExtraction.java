package quenfo.de.uni_koeln.spinfo.information_extraction.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import quenfo.de.uni_koeln.spinfo.information_extraction.data.ExtractionUnit;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.InformationEntity;
import quenfo.de.uni_koeln.spinfo.information_extraction.data.Pattern;

public class RatePatternExtraction {
    
    /**
	 * Evaluation of the used pattern in one iteration and the extracted
	 * competences.
	 * 
	 * @author Christine Schaefer
	 *
	 */


	/**
	 * Computes the confidence of the used patterns: Conf(P) = P.pos / (P.pos +
	 * P.neg)
	 * 
	 * @param knownEntites
	 * @param extractions
	 * 
	 * @return pattern-confidence
	 * 
	 */
	@SuppressWarnings("unlikely-arg-type")
	public Map<String, Double> evaluatePattern(Map<String, Set<InformationEntity>> competences,
			Map<String, Set<List<String>>> noCompetences,
			Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> extractions) {

		Map<String, Double> confP = new HashMap<String, Double>();
		List<Pattern> usedPattern = new ArrayList<Pattern>();

		List<InformationEntity> validatedCompetences = new ArrayList<InformationEntity>();
		List<String> knownExtractionFails = new ArrayList<String>();

		for (ExtractionUnit extractionUnit : extractions.keySet()) {
			Map<InformationEntity, List<Pattern>> ieP = extractions.get(extractionUnit);

			for (InformationEntity ie : ieP.keySet()) {
				List<Pattern> iePattern = ieP.get(ie);
				for (Pattern pattern : iePattern) {
					if (!usedPattern.contains(pattern))
						usedPattern.add(pattern);
				}
			}
		}

		for (String s : competences.keySet()) {
			Set<InformationEntity> knownExtractions = competences.get(s);
			for (InformationEntity e : knownExtractions) {
				if (!(validatedCompetences.contains(e))) {
					validatedCompetences.add(e);
				}
			}
		}

		for (String s : noCompetences.keySet()) {
			Set<List<String>> negExamples = noCompetences.get(s);
			for (List<String> ls : negExamples) {
				if (!(knownExtractionFails.contains(ls))) {
					knownExtractionFails.addAll(ls);
				}
			}
		}

		// Iteration über jedes genutzte Muster, das in der Extraktionsmap aufgelistet
		// ist
		for (Pattern p : usedPattern) {

			int tp = 0;
			int fp = 0;

			// Liste mit den Extraktionen des aktuellen Musters
			List<InformationEntity> extractionsOfPattern = new ArrayList<InformationEntity>();

			for (ExtractionUnit extractionUnit : extractions.keySet()) {
				Map<InformationEntity, List<Pattern>> ieP = extractions.get(extractionUnit);
				for (InformationEntity ie : ieP.keySet()) {
					if (ieP.get(ie).contains(p)) {
						extractionsOfPattern.add(ie);
					}
				}
			}

			// Vergleich der Extraktionen mit den validierten Kompetenzen
			for (InformationEntity ie : extractionsOfPattern) {
				if (validatedCompetences.contains(ie)) {
					tp++;
				}
				if (knownExtractionFails.contains(ie.toString())) {
					fp++;
				}
			}

			// Hinzufügen des Musters mit ermittelten Confidence-Wert
			// eigentlich ist hier die Map nicht mehr notwendig, da direkt der Conf-Wert des
			// Patterns mit setConf gesetzt wird
			confP.put(p.getDescription(), p.setConf(tp, fp));
			p.setConf(tp, fp);

			// TP und FP für neues Pattern wieder null setzen
			tp = 0;
			fp = 0;

		}
		return confP;
	}

	/**
	 * Computes the confidence of the extraction: 1 - (Produkt(1 - Conf(p)))
	 * 
	 * @param allextractions
	 * 
	 * @return extraction confidence
	 */
	public Map<InformationEntity, Double> evaluateSeed(
			Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> allextractions) {
		// Conf(seed) = 1 - <Produkt>(1-Conf(P))

		Map<InformationEntity, Double> confS = new HashMap<InformationEntity, Double>();

		for (ExtractionUnit extractionUnit : allextractions.keySet()) {
			Map<InformationEntity, List<Pattern>> extraction = allextractions.get(extractionUnit);

			for (InformationEntity ie : extraction.keySet()) {
				List<Pattern> pattern = extraction.get(ie);

				List<Pattern> usedPattern = new ArrayList<Pattern>();
				for (Pattern p : pattern) {
					if (!usedPattern.contains(p))
						usedPattern.add(p);
				}

				ie.setConf(usedPattern);
				confS.put(ie, ie.setConf(usedPattern));
			}
		}
		return confS;
	}

	/**
	 * Select extractions with confidence >= 0.9
	 * 
	 * @param allExtractions
	 * @return selected extractions
	 */
	public Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> selectBestEntities(
			Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> allExtractions) {
		Map<ExtractionUnit, Map<InformationEntity, List<Pattern>>> toReturn = new HashMap<ExtractionUnit, Map<InformationEntity, List<Pattern>>>();
		for (ExtractionUnit extractionUnit : allExtractions.keySet()) {
			Map<InformationEntity, List<Pattern>> ies = allExtractions.get(extractionUnit);
			Map<InformationEntity, List<Pattern>> filterdIes = new HashMap<InformationEntity, List<Pattern>>();
			for (InformationEntity ie : ies.keySet()) {
				if (ie.getConf() >= 0.9) {
					filterdIes.put(ie, ies.get(ie));
				}
			}
			if (!filterdIes.isEmpty()) {
				toReturn.put(extractionUnit, filterdIes);
			}
		}
		return toReturn;
	}
}
