package edu.mit.csail.ammolite;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class KeyListMap<K,V> extends HashMap<K, List<V>> implements Serializable {
	
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
	
}
