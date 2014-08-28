package edu.mit.csail.ammolite.utils;

import java.io.Serializable;

public class ID implements Serializable{
	protected String id;
	
	public ID(String _id){
		id = _id;
	}
	
	@Deprecated
	public String asString(){
		return toString();
	}
	
	public String toString(){
		return id;
	}
	
	public boolean equals(Object that){
		if(that instanceof ID ){
			ID iThat = (ID) that;
			return id.equals(iThat.toString());
		} else if(that instanceof String){
			String sThat = (String) that;
			return id.equals(sThat);
		}
		
		return false;
		
		
	}
	
	@Override
	public int hashCode(){
		return toString().hashCode();
	}

}
