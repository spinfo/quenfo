package de.uni_koeln.spinfo.grouping;

public class SimilarityCalculator {
	
	private int gap = -1;
	private int mismatch = 0;
	private int match = 3;
	
	int[][] d;
	
	
	public int smithWatermanSimilarity(String s1, String s2){
		
		char[] a = s1.toCharArray();
		char[] b = s2.toCharArray();
		
		int n = a.length;
		int m = b.length;
		
		int[][] d = new int[n+1][m+1];
		
		d[0][0] = 0;
		for (int i = 0; i <= n; i++) {
			d[i][0] = -i;
		}
		for (int j = 0; j <= m; j++) {
			d[0][j] = -j;
		}
		
		for (int i = 1; i <= n; i++) {
			for(int j = 1; j <= m; j++){
				d[i][j] = max( 
						d[i-1][j-1] + compare(a[i-1], 
								b[j-1]), d[i][j-1] + gap, 
								d[i-1][j]+gap);
			}
		}
		return d[n][m];

	}
	
	private int max(int a, int b, int c){
		int max = 0;
		if(a>max) max = a;
		if(b>max) max = b;
		if(c>max) max = c;
		return max;
	}
	
	private int compare(char a, char b){
		int match = 3;
		int mismatch = 0;
		if(a==b) return match;
		return mismatch;
	}

}
