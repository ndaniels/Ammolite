/**
 * 
 */
package edu.mit.csail.fmcsj;

import java.util.HashMap;
import java.util.Map;


/**
 * @author DC
 *
 */
public class GraphMCSMap {

	private GraphMCSList keyList = new GraphMCSList();
	private GraphMCSList valList = new GraphMCSList();
	
	public int size(){
		return keyList.size();
	}
	
	public void push(Integer key, Integer val){
		keyList.push(key);
		valList.push(val);
	}
	
	public void pop(){
		valList.pop();
		keyList.pop();
	}
	
	public Integer getKey(Integer val){
		for(int i=0; i<size(); ++i){
			if(val == valList.get(i)){ // This is supposed to be memory address.
				return keyList.get(i);
			}
		}
		return null;
	}
	
	public Integer getVal(Integer key){
		for(int i=0; i<size(); ++i){
			if(key == keyList.get(i)){ // This is supposed to be memory address.
				return valList.get(i);
			}
		}
		return null;
	}
	
	public void clear(){
		keyList.clear();
		valList.clear();
	}
	
	public boolean containsKey(Integer key){
		for(int i=0; i<size(); ++i){
			if(key == keyList.get(i)){ // This is supposed to be memory address.
				return true;
			}
		}
		return false;
	}
	
	public boolean containsVal(Integer val){
		for(int i=0; i<size(); ++i){
			if(val == valList.get(i)){ // This is supposed to be memory address.
				return true;
			}
		}
		return false;
	}
	
	public GraphMCSList getKeyList(){
		return new GraphMCSList(keyList);
	}
	
	public GraphMCSList getValList(){
		return new GraphMCSList(valList);
	}
	
	public GraphMCSMap copy(){
		GraphMCSMap out = new GraphMCSMap();
		for(Integer key: this.getKeyList()){
			out.push( key, this.getVal(key));
		}
		return out;
	}
	

	
	
	
}