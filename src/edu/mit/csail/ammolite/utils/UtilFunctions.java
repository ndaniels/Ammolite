package edu.mit.csail.ammolite.utils;

public class UtilFunctions {

	public static double tanimotoCoeff(int overlap, int a, int b){
		return ( (double) overlap) / ( a + b - overlap);
	}
	
	public static double overlapCoeff(int overlap, int a, int b){
		if( a < b){
			return ( 1.0*overlap) / a;
		}
		return ( 1.0*overlap) / b;
	}
}
