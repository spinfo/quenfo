package quenfo.de.uni_koeln.spinfo.categorization.workflow;

public class SimilarityCalculator {

	private int gap = -1;
	// TODO JB: mismatch und match werden nicht aufgerufen?
//	private int mismatch = -1;
//	private int match = 3;
//
//	private int[][] d;

	public SimilarityCalculator() {

	}

	/**
	 * @param match
	 * @param mismatch
	 * @param gap
	 */
	public SimilarityCalculator(int match, int mismatch, int gap) {
//		this.match = match;
//		this.mismatch = mismatch;
		this.gap = gap;
	}

	/**calculates the smith-waterman-similarity of the given strings --> locale alignment
	 * @param s1
	 * @param s2
	 * @return smith-waterman-similarity
	 */
	public int smithWatermanSimilarity(String s1, String s2) {
		int maxValue = 0;
		char[] a = s1.toCharArray();
		char[] b = s2.toCharArray();

		int n = a.length;
		int m = b.length;

		int[][] d = new int[n + 1][m + 1];

		d[0][0] = 0;
		for (int i = 0; i <= n; i++) {
			d[i][0] = 0;
		}
		for (int j = 0; j <= m; j++) {
			d[0][j] = 0;
		}

		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= m; j++) {
				d[i][j] = max2(d[i - 1][j - 1] + compare(a[i - 1], b[j - 1]), d[i][j - 1] + gap, d[i - 1][j] + gap);
				if(d[i][j] > maxValue){
					maxValue = d[i][j];
				}
			}
		}
		return maxValue;
	}

	/**
	 * calculates the needleman-wunsch-similarity of the given strings
	 * @param s1
	 * @param s2
	 * @return needleman-wunsch-similarity
	 */
	public int needlemanWunschSimilarity(String s1, String s2) {

		char[] a = s1.toCharArray();
		char[] b = s2.toCharArray();

		int n = a.length;
		int m = b.length;

		int[][] d = new int[n + 1][m + 1];

		d[0][0] = 0;
		for (int i = 0; i <= n; i++) {
			d[i][0] = -i;
		}
		for (int j = 0; j <= m; j++) {
			d[0][j] = -j;
		}

		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= m; j++) {
				d[i][j] = max(d[i - 1][j - 1] + compare(a[i - 1], b[j - 1]), d[i][j - 1] + gap, d[i - 1][j] + gap);
			}
		}
		return d[n][m];

	}

	private int max(int a, int b, int c) {
		int max = 0;
		if (a > max)
			max = a;
		if (b > max)
			max = b;
		if (c > max)
			max = c;
		return max;
	}

	private int max2(int a, int b, int c) {
		int max = 0;
		if (a > max)
			max = a;
		if (b > max)
			max = b;
		if (c > max)
			max = c;
		if (max > 0)
			return max;
		return 0;
	}

	private int compare(char a, char b) {
		int match = 3;
		int mismatch = 0;
		if (a == b)
			return match;
		return mismatch;
	}

}
