package speedysearch;

import java.io.Serializable;

public class FilePair implements Serializable{
	private String filename;
	private long location;
	
	public FilePair(String name, long loc){
		filename = name;
		location = loc;
	}
	
	public String name(){
		return filename;
	}
	
	public long location(){
		return location;
	}
}
