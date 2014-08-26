package edu.mit.csail.ammolite.utils;

public class ID {
	protected String id;
	
	public ID(String _id){
		id = _id;
	}
	
	public String asString(){
		return id;
	}
	
	public boolean equals(Object that){
		if(that instanceof ID ){
			ID iThat = (ID) that;
			return id.equals(iThat.asString());
		} else if(that instanceof String){
			String sThat = (String) that;
			return id.equals(sThat);
		}
		
		return false;
		
		
	}

}
