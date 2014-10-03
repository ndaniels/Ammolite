package edu.mit.csail.ammolite.utils;

public class Pair<A, B> {
	private A left;
	private B right;
	
	public Pair(A l, B r){
		left = l;
		right = r;
	}
	
	public A left(){ 
		return left;
	}
	
	public B right(){
		return right;
	}

}
