package fmcs;

import java.util.ArrayList;
import java.util.List;

public class MCSList<T> extends ArrayList<T> {

	public MCSList( List<T> l){
		super(l);
	}
	public MCSList(){
		super();
	}
	
	private static final long serialVersionUID = 1L;

	public void push(T el){
		this.add(el);
	}
	
	public T pop(){
		return this.remove( this.size() - 1);
	}

}
