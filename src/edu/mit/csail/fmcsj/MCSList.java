package edu.mit.csail.fmcsj;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtom;

public class MCSList<T> implements Iterable<T>{
	private ArrayList<T> myList;
	private Set<T> els;
	
	public MCSList( List<T> l){
		myList = new ArrayList<T>(l);
		els = new HashSet<T>(l);
	}
	
	public MCSList(){
		super();
	}


	public MCSList(MCSList<T> l) {
		for(T el: l){
			this.push(el);
		}
	}
	
	public void push(final T el){
		
		myList.add(el);
		els.add(el);

	}
	
	
	public T pop(){
		T el = myList.remove( myList.size() - 1);
		els.remove(el);
		return el;
	
	}
	
	public int size(){
		return myList.size();
	}
	
	public boolean contains(T el){
		return els.contains(el);
	}
	
	@Override
	public boolean equals(Object that){
		if(!(that instanceof MCSList<?>)){
			return false;
		}
		MCSList<T> mThat = (MCSList<T>) that;
		if( mThat.size() != this.size()){
			return false;
		}
		for(T el: myList){
			if( !mThat.contains(el)){
				return false;
			}
		}
		return true;
	}

	public T get(int j) {
		return myList.get(j);
	}
	
	public void clear() {
		els.clear();
		myList.clear();
		
	}
	
	@Override
	public Iterator<T> iterator() {
		return myList.iterator();
	}
	
	public void remove(T el) {
		myList.remove(el);
		els.remove(el);
		
	}
	public boolean isEmpty() {
		return this.size() == 0;
	}

}
