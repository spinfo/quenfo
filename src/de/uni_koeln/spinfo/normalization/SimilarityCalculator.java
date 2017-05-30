package de.uni_koeln.spinfo.normalization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author geduldia
 * 
 * a class to calc similarities between a set of strings (vocabulary)
 *
 */
public class SimilarityCalculator {

	private List<String> vocabulary;

	private int match = 3;
	private int mismatch = -2;
	private int gap = -1;
	private int[][] similarities;

	// int insert = 1;
	// int delete = 0;
	// int replace = 1;
	// int[][] distances;

	
	public int getMatch() {
		return match;
	}

	public void setMatch(int match) {
		this.match = match;
	}

	public int getMismatch() {
		return mismatch;
	}

	public void setMismatch(int mismatch) {
		this.mismatch = mismatch;
	}

	public int getGap() {
		return gap;
	}

	public void setGap(int gap) {
		this.gap = gap;
	}

	/**
	 * @param vocab a set of all extracted entity-expressions
	 */
	public void setVocabulary(Set<String> vocab) {
		this.vocabulary = new ArrayList<String>(vocab);
	}

	/**
	 * calcs the similarity between each word-pair from the vocabulary and stores it in the similarities-matrix 
	 */
	public int[][] calcSimilarityMatrix() {
		this.similarities = new int[vocabulary.size()][vocabulary.size()];
		for (int i = 0; i < vocabulary.size() - 1; i++) {
			for (int j = i + 1; j < vocabulary.size(); j++) {
				int sim = getSimilarity(vocabulary.get(i), vocabulary.get(j));
				similarities[j][i] = sim;
				similarities[i][j] = sim;
			}
		}
		return similarities;
	}

	/**
	 * @param s a string from the vocabulary
	 * @return a list of all similar strings in the vocabulary (depending on a minimum similarity-value)
	 */
	public List<String> getSimilaritiesForString(String s) {
		List<String> toReturn = new ArrayList<String>();
		if(s.length() <= 3) 
			//ignore strings with length < 4  (too short for similarity calculation)
			return toReturn;
		int i = vocabulary.indexOf(s);
		if (i == -1)
			//s is not in the vocabulary
			return null;
		for (int j = 0; j < vocabulary.size(); j++) {
			int minSim;
			if (j == i)
				continue;
			String other = vocabulary.get(j);
			if(other.length() <= 3) 
				//too short
				continue;
			if (other.length() > 4 * s.length())
				//length difference is too big
				continue;
			if (s.length() > 4 * other.length())
				//length difference is too big
				continue;
			if (s.length() > other.length()) {
				minSim = (int) (other.length() * (double) 3);
			} else {
				minSim = (int) (s.length() * (double) 3);
			}
			if (minSim < 5)
				//minSim must be at least 4
				minSim = 4;
			int sim = similarities[i][j];
			if (sim < minSim)
				continue;
			toReturn.add(vocabulary.get(j));
		}
		return toReturn;
	}

	private int getSimilarity(String s1, String s2) {
		char[] a = s1.toCharArray();
		char[] b = s2.toCharArray();
		int n = a.length;
		int m = b.length;
		if (n == 0)
			return -m;
		if (m == 0)
			return -n;
		int[][] d = new int[n + 1][m + 1];
		for (int i = 0; i <= n; i++) {
			d[i][0] = 0;
		}
		for (int j = 0; j <= m; j++) {
			d[0][j] = 0;
		}
		int maxValue = 0;
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= m; j++) {
				d[i][j] = maximum((d[i - 1][j] + gap), (d[i][j - 1] + gap),
						(d[i - 1][j - 1] + equal(a[i - 1], b[j - 1])));
				if (d[i][j] > maxValue) {
					maxValue = d[i][j];
				}
			}
		}
		return maxValue;
	}
	
	private int maximum(int a, int b, int c) {
		int max = a;
		if (b > max)
			max = b;
		if (c > max)
			max = c;
		return max;
	}

	private int equal(int a, int b) {
		if (a == b)
			return match;
		return mismatch;
	}
	// public void calcDistanceMatrix() {
	// this.distances = new int[vocabulary.size()][vocabulary.size()];
	// for (int i = 0; i < vocabulary.size() - 1; i++) {
	// for (int j = i + 1; j < vocabulary.size(); j++) {
	// int dist = getDistance(vocabulary.get(i), vocabulary.get(j));
	// distances[j][i] = dist;
	// distances[i][j] = dist;
	// }
	// }
	// }

	// public List<String> getDistancesForString(String s) {
	// int maxDist = s.length()/2;
	// List<String> toReturn = new ArrayList<String>();
	// int i = vocabulary.indexOf(s);
	// if (i == -1)
	// return null;
	// for (int j = 0; j < vocabulary.size(); j++) {
	// maxDist = s.length()/2;
	// if (j == i)
	// continue;
	// int dist = distances[i][j];
	// if (dist > maxDist)
	// continue;
	//
	// toReturn.add(vocabulary.get(j));
	// }
	// return toReturn;
	// }

	// private int getDistance(String string1, String string2) {
	// int n = string1.length();
	// int m = string2.length();
	// char[] wordA = string1.toCharArray();
	// char[] wordB = string2.toCharArray();
	// if (n == 0)
	// return m * insert;
	// if (m == 0)
	// return n * delete;
	//
	// int[][] A = new int[n + 1][m + 1];
	// for (int i = 0; i <= n; i++) {
	// A[i][0] = i;
	// }
	// for (int j = 1; j <= m; j++) {
	// A[0][j] = j;
	// }
	// for (int i = 1; i <= n; i++) {
	// int a_i = wordA[i - 1];
	// for (int j = 1; j <= m; j++) {
	// int b_j = wordB[j - 1];
	// if (a_i == b_j) {
	// A[i][j] = A[i - 1][j - 1];
	// } else {
	//
	// A[i][j] = min(A[i - 1][j] + delete, A[i][j - 1] + insert, A[i - 1][j - 1]
	// + replace);
	// }
	// }
	// }
	// return A[n][m];
	// }

	

	

	// private int min(int a, int b, int c) {
	// int min = a;
	// if (b < min)
	// min = b;
	// if (c < min)
	// min = c;
	// return min;
	// }

}
