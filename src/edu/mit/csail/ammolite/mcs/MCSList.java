package edu.mit.csail.ammolite.mcs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtom;

import edu.mit.csail.ammolite.mcs.MCSIterator;
import edu.mit.csail.ammolite.mcs.MCSList;

public class MCSList<T> implements Iterable<T>{
	private List<T> myList;
	private Map<T,Integer> atomsToCounts = new HashMap<T,Integer>();
	int mySize = 0;
	
	public MCSList( List<T> l){
		myList = new ArrayList<T>(l.size()); 
		mySize = myList.size();
		for(T el: l){
			this.push(el);
		}

	}
	
	public MCSList(){
		myList = new ArrayList<T>();
	}


	public MCSList(MCSList<T> l) {
		myList = new ArrayList<T>();
		for(T el: l){
			this.push(el);
		}
	}
	
	public void push(T el){
		if(atomsToCounts.containsKey(el)){
			int count = atomsToCounts.get(el);
			++count;
			atomsToCounts.put(el,count);
		} else {
			atomsToCounts.put(el, 1);
		}
		myList.add(el);
		mySize++;
	}
	
	public T pop(){
		T el = myList.get(myList.size() - 1);
		myList.remove( myList.size() - 1);
		mySize--;
		int count = atomsToCounts.get(el);
		--count;
		if(count == 0){
			atomsToCounts.remove(el);
		} else {
			atomsToCounts.put(el, count);
		}
		return el;
	}
	
	public T peek(){
		return myList.get(myList.size() - 1);
	}
	
	public int size(){
		return mySize;
	}
	
	public boolean contains(T el){
		return atomsToCounts.containsKey(el);
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
		return atomsToCounts;
	}

	public T get(int j) {
		return myList.get(j);
	}
	
	public void clear() {
		atomsToCounts.clear();
		myList.clear();
		mySize = 0;
	}
	
	@Override
	public Iterator<T> iterator() {
		return new MCSIterator<T>( this);
	}
	
	public void remove(T el) {
		myList.remove(el);
		mySize--;
		int count = atomsToCounts.get(el);
		--count;
		if(count == 0){
			atomsToCounts.remove(el);
		} else {
			atomsToCounts.put(el, count);
		}
	}
	
	public boolean isEmpty() {
		return this.size() == 0;
	}
	
	
	private void check(){
		if(!(myList.size() == atomsToCounts.size())){
			throw new IllegalStateException("List sizes mismatched");
		} 
		
		for(int i=0; i<size(); ++i){
			T el = myList.get(i);
			if(!atomsToCounts.get(el).equals(i)){
				throw new IllegalStateException("Map points to wrong index");
			}
		}
	}

}
