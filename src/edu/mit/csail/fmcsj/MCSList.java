package edu.mit.csail.fmcsj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtom;

public class MCSList<T> implements Iterable<T>{
	private ArrayList<T> myList;
	private Map<T,Integer> els;
	
	public MCSList( List<T> l){
		myList = new ArrayList<T>(l);
		els = new HashMap<T,Integer>();
		for(int i=0; i<myList.size(); ++i){
			els.put(myList.get(i),i);
		}

	}
	
	public MCSList(){
		myList = new ArrayList<T>();
		els = new HashMap<T,Integer>();
	}


	public MCSList(MCSList<T> l) {
		myList = new ArrayList<T>();
		els = new HashMap<T,Integer>();
		for(T el: l){
			this.push(el);
		}
	}
	
	public void push(T el){
		if(els.containsKey(el)){
			int count = els.get(el);
			++count;
			els.put(el,count);
		} else {
			els.put(el, 1);
		}
		myList.add(el);
	}
	
	public T pop(){
		T el = myList.get(myList.size() - 1);
		myList.remove( myList.size() - 1);
		int count = els.get(el);
		--count;
		if(count == 0){
			els.remove(el);
		} else {
			els.put(el, count);
		}
		return el;
	}
	
	public T peek(){
		return myList.get(myList.size() - 1);
	}
	
	public int size(){
		return myList.size();
	}
	
	public boolean contains(T el){
		return els.containsKey(el);
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
		Map<T,Integer> myMap    = this.getElementsByCount();
		Map<T,Integer> theirMap = mThat.getElementsByCount();
		
		for(T key: myMap.keySet()){
			boolean hasKey = theirMap.containsKey(key);
			if( hasKey ){
				boolean countsDoNotMatch = !myMap.get(key).equals(theirMap.get(key));
				if( countsDoNotMatch){
					return false;
				}
			} else {
				return false;
			}
		}
		
		return true;
	}
	
	public Map<T,Integer> getElementsByCount(){
		return els;
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
		return new MCSIterator<T>( this);
	}
	
	public void remove(T el) {
		myList.remove(el);
		int count = els.get(el);
		--count;
		if(count == 0){
			els.remove(el);
		} else {
			els.put(el, count);
		}
	}
	
	public boolean isEmpty() {
		return this.size() == 0;
	}
	
	private void check(){
		if(!(myList.size() == els.size())){
			throw new IllegalStateException("List sizes mismatched");
		} 
		
		for(int i=0; i<size(); ++i){
			T el = myList.get(i);
			if(!els.get(el).equals(i)){
				throw new IllegalStateException("Map points to wrong index");
			}
		}
	}

}
