/**
 * 
 */
package edu.mit.csail.fmcsj;

import java.util.HashMap;
import java.util.Map;

import org.openscience.cdk.interfaces.IAtom;

/**
 * @author DC
 *
 */
public class MCSMap {

	private MCSList<IAtom> keyList = new MCSList<IAtom>();
	private MCSList<IAtom> valList = new MCSList<IAtom>();
	
	private Map<IAtom,IAtom> keyToVal = new HashMap<IAtom,IAtom>();
	private Map<IAtom,IAtom> valToKey = new HashMap<IAtom,IAtom>();
	
	public int size(){
		return keyList.size();
	}
	
	public void push(IAtom key, IAtom val){
		keyList.push(key);
		valList.push(val);
		keyToVal.put(key, val);
		valToKey.put(val, key);
	}
	
	public void pop(){
		valToKey.remove(valList.pop());
		keyToVal.remove(keyList.pop());
	}
	
	public IAtom getKey(IAtom val){
		return valToKey.get(val);
	}
	
	public IAtom getVal(IAtom key){
		return keyToVal.get(key);
	}
	
	public void clear(){
		keyList.clear();
		valList.clear();
		keyToVal.clear();
		valToKey.clear();
	}
	
	public boolean containsKey(IAtom key){
		return keyToVal.containsKey(key);
	}
	
	public boolean containsVal(IAtom val){
		return valToKey.containsKey(val);
	}
	
	public MCSList<IAtom> getKeyList(){
		return new MCSList<IAtom>(keyList);
	}
	
	public MCSList<IAtom> getValList(){
		return new MCSList<IAtom>(valList);
	}
	
	public MCSMap copy(){
		MCSMap out = new MCSMap();
		for(IAtom key: this.getKeyList()){
			out.push( key, this.getVal(key));
		}
		return out;
	}
	
	
	
}
