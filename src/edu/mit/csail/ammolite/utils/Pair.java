package edu.mit.csail.ammolite.utils;

public class Pair<T> {
	private T left;
	private T right;
	
	public Pair(T l, T r){
		left = l;
		right = r;
	}
	
	public T left(){ 
		return left;
	}
	
	public T right(){
		return right;
	}

}
