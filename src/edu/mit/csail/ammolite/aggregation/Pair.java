package edu.mit.csail.ammolite.aggregation;

public class Pair<T> {
	private T left;
	private T right;
	
	public Pair(T a, T b){
		left = a;
		right = b;
	}
	
	public T left(){
		return left;
	}
	
	public T right(){
		return right;
	}
	

}
