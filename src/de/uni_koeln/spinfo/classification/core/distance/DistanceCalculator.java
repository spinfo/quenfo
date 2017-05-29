package de.uni_koeln.spinfo.classification.core.distance;



/**
 * @author geduldia
 * 
 * a class for distance calculation with different distance-measures
 *
 */
public class DistanceCalculator {
	
	
	
	/**
	 * calculates the distance of the given vectors a & b
	 * @param a vector a
	 * @param b vector b
	 * @return distance of a and b
	 */
	public static double getDistance(double[]a, double[] b, Distance distance){

		
		if(distance == Distance.EUKLID){
			return getEuklideanDistance(a, b);
		}
		if(distance == Distance.COSINUS){
			return getCosinusDistance(a, b);
		}
		if(distance == Distance.MANHATTAN){
			return getManhattanDistance(a,b);
		}
		return 0.0;
	}
	
	
	

	private static double getManhattanDistance(double[] a, double[] b){
		double toReturn = 0.0;
		for(int d = 0; d < a.length; d++){
			toReturn += Math.abs(a[d]-b[d]);
		}
		return toReturn;
	}

	private static double getEuklideanDistance(double[] a, double[] b){
		double toReturn = 0.0;
		for(int d = 0; d < a.length; d++){
			double dif =a[d]-b[d];
			toReturn+=dif*dif;
		}
		toReturn = Math.sqrt(toReturn);
		return toReturn;
		
	}
	
	private static double getCosinusDistance(double[] a, double[] b){
		double toReturn;
		double dotProduct = 0;
		double vecProduct = 0;
		double centProduct = 0;
		for(int d = 0; d < a.length; d++){
			dotProduct+=a[d] * b[d];
			vecProduct+=a[d] *a [d];
			centProduct+=b[d] * b[d];
			
		}
		vecProduct =  Math.sqrt(vecProduct);
		centProduct =  Math.sqrt(centProduct);
		double similarity = dotProduct/(vecProduct*centProduct);
		if(similarity >= 0){
			toReturn = 1 - similarity;
		}
		else{
			toReturn  = -similarity;
		}
		return toReturn;
		
	}

}
