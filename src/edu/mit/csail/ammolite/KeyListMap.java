package edu.mit.csail.ammolite;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.SDFUtils;

public class KeyListMap<K,V> extends HashMap<K, List<V>> implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6317801244680182657L;

	public KeyListMap(int initialSize){
		super(initialSize);
	}
	
	public V add(K key, V val){
		if(this.containsKey(key)){
			this.get(key).add(val);
		} else {
			this.put(key, new LinkedList<V>());
			this.get(key).add(val);
		}
		return val;
	}
	
	public Iterator<V> valueIterator(){
	    return new ValIterator<V>(this.values().iterator());
	}
	
	private class ValIterator<V> implements Iterator {
	    
	    Iterator<V> currentIterator;
	    Iterator<List<V>> listIterator;
	    
	    public ValIterator(Iterator<List<V>> listIterator){
	        this.listIterator = listIterator;
	        this.loadNextIterator();
	    }
	    

	    private boolean loadNextIterator(){
	        if( listIterator.hasNext()){
	            List<V> nextList = listIterator.next();
	            currentIterator = nextList.iterator();
	            return true;
	        }
	        return false;
	    }
	    
	    @Override
	    public boolean hasNext() {
            if( currentIterator.hasNext()){
                return currentIterator.hasNext();
            } else {
                boolean isNextIterator = loadNextIterator();
                if( isNextIterator){
                    return this.hasNext();
                } else {
                    return false;
                }
            }
	            
	     
	    }

	    @Override
	    public V next() {
	        if( this.hasNext()){
	            V nextOne;
	            nextOne = currentIterator.next();
	            return nextOne;
	        }
	        return null;
	    }
	    
	}
	
}
