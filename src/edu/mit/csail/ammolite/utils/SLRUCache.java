package edu.mit.csail.ammolite.utils;

import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.ArrayList;


public class SLRUCache<K,V> {

private static final float   hashTableLoadFactor = 0.75f;
Runtime runtime = Runtime.getRuntime();

private LinkedHashMap<K,V>   map;
private long allowedMemory;


public SLRUCache (double d) {
   allowedMemory = (long) (runtime.totalMemory() * d);
   int hashTableCapacity = 1000;
   map = new LinkedHashMap<K,V>(hashTableCapacity, hashTableLoadFactor, true) {
      // (an anonymous inner class)
      private static final long serialVersionUID = 1;
      @Override 
      protected boolean removeEldestEntry (Map.Entry<K,V> eldest) {
         return SLRUCache.this.allowedMemory < SLRUCache.this.runtime.freeMemory();
         }
      }; 
 }

public synchronized boolean containsKey(K key){
	return map.containsKey(key);
}

public synchronized V get (K key) {
   return map.get(key); }


public synchronized void put (K key, V value) {
   map.put (key, value); }


public synchronized void clear() {
   map.clear(); }


public synchronized int usedEntries() {
   return map.size(); }


public synchronized Collection<Map.Entry<K,V>> getAll() {
   return new ArrayList<Map.Entry<K,V>>(map.entrySet()); }

}