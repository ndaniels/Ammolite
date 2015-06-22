package edu.mit.csail.ammolite.utils;

import java.io.Serializable;

public class ID implements Serializable{
	protected String id;
	
	public ID(String _id){
		if(_id == null){
			throw new NullPointerException("ID string cannot be initialized as null");
		}
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
		String myID = toString();
		if(myID != null){
			return myID.hashCode();
		}
		throw new NullPointerException("Null ID String");
	}

}
