package quenfo.de.uni_koeln.spinfo.disambiguation.workflow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.FeatureUnitTokenizer;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_reduction.Normalizer;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_reduction.Stemmer;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_reduction.StopwordFilter;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_weighting.AbstractFeatureQuantifier;
import quenfo.de.uni_koeln.spinfo.classification.core.feature_engineering.feature_weighting.LogLikeliHoodFeatureQuantifier;

public class WSDJobs {

	private StopwordFilter sw_filter;
	private Normalizer normalizer;
	private Stemmer stemmer;
	private FeatureUnitTokenizer tokenizer;

	private Map<String, Integer> docFreq = new HashMap<String, Integer>();

	public WSDJobs() throws IOException {
		this.sw_filter = new StopwordFilter(new File("classification/data/stopwords.txt"));
		this.normalizer = new Normalizer();
		this.stemmer = new Stemmer();
		this.tokenizer = new FeatureUnitTokenizer();
	}

	public List<ClassifyUnit> preprocessJobAds(List<ClassifyUnit> jobAds) throws IOException {

		for (ClassifyUnit cu : jobAds) {
			List<String> tokens = selectFeatures(cu.getContent());
			cu.setFeatureUnits(tokens);

			Set<String> tokenSet = new HashSet<String>(tokens);

			for (String t : tokenSet) {
				Integer freq = 0;
				if (docFreq.containsKey(t))
					freq = docFreq.get(t);
				docFreq.put(t, freq + 1);
			}
		}
		
		
		System.out.println(docFreq.size() + " Merkmale im Korpus");
		
		List<String> featureUnitOrder = new ArrayList<String>();
		
		for(Map.Entry<String, Integer> e : docFreq.entrySet()) {
			if(e.getValue() < 5)
				continue;
			if(e.getValue() > 9000)
				continue;
			featureUnitOrder.add(e.getKey());
		}
		System.out.println(featureUnitOrder.size() + " Merkmale im Korpus");

//		Stream<Map.Entry<String,Integer>> sorted =
//			    docFreq.entrySet().stream()
//			       .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()));
//		
//		sorted.limit(100).forEach(System.out::println);

		jobAds = setFeatureVectors(jobAds, new LogLikeliHoodFeatureQuantifier(), featureUnitOrder);

		return jobAds;
	}

	public List<String> selectFeatures(String content) {
		List<String> tokens = tokenizer.tokenize(content);
		if (tokens == null)
			return null;

		normalizer.normalize(tokens);

		tokens = sw_filter.filterStopwords(tokens);

		tokens = stemmer.getStems(tokens);
		return tokens;
	}

	public List<ClassifyUnit> setFeatureVectors(List<ClassifyUnit> jobAds, AbstractFeatureQuantifier fq, List<String> featureUnitOrder) {

		fq.setFeatureValues(jobAds, featureUnitOrder);

		return jobAds;
	}

}
