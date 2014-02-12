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

	public void push(final T el){
		
		this.add(el);

	}
	
	public T front(){
		return this.get(0);
	}
	
	public T back(){
		return this.get(this.size()-1);
	}
	
	public void pop(){
		this.remove( this.size() - 1);
	
	}
	
	@Override
	public boolean equals(Object that){
		if(!(that instanceof MCSList<?>)){
			return false;
		}
		MCSList<?> mThat = (MCSList<?>) that;
		if( mThat.size() != this.size()){
			return false;
		}
		for(T el: this){
			if( !mThat.contains(el)){
				return false;
			}
		}
		return true;
	}

}
