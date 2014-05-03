package edu.mit.csail.ammolite.mcs;

import java.util.Iterator;
import java.util.NoSuchElementException;

import edu.mit.csail.fmcsj.MCSList;

public class MCSIterator<T> implements Iterator<T> {
	private MCSList<T> list;
	private int i;
	
	public MCSIterator(MCSList<T> _list){
		list = _list;
		i=0;
	}
	
	public boolean hasNext(){
		return i<list.size();
	}
	
	public T next(){
		if(i>=list.size()){
			throw new NoSuchElementException();
		}
		T el = list.get(i);
		++i;
		return el;
	}
	
	public void remove(){
		if(i>0 && i<=list.size()){
			list.remove(list.get(i));
		} else {
			throw new IllegalStateException();
		}
	}
}
