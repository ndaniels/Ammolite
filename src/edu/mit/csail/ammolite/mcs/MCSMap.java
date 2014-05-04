/**
 * 
 */
package edu.mit.csail.ammolite.mcs;

import java.util.HashMap;
import java.util.Map;

import org.openscience.cdk.interfaces.IAtom;

import edu.mit.csail.ammolite.mcs.MCSList;
import edu.mit.csail.ammolite.mcs.MCSMap;

/**
 * @author DC
 *
 */
public class MCSMap {

	private MCSList<IAtom> keyList = new MCSList<IAtom>();
	private MCSList<IAtom> valList = new MCSList<IAtom>();
	
	public int size(){
		return keyList.size();
	}
	
	public void push(IAtom key, IAtom val){
		keyList.push(key);
		valList.push(val);
	}
	
	public void pop(){
		valList.pop();
		keyList.pop();
	}
	
	public IAtom getKey(IAtom val){
		for(int i=0; i<size(); ++i){
			if(val == valList.get(i)){ // This is supposed to be memory address.
				return keyList.get(i);
			}
		}
		return null;
	}
	
	public IAtom getVal(IAtom key){
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
	
	public boolean containsKey(IAtom key){
		for(int i=0; i<size(); ++i){
			if(key == keyList.get(i)){ // This is supposed to be memory address.
				return true;
			}
		}
		return false;
	}
	
	public boolean containsVal(IAtom val){
		for(int i=0; i<size(); ++i){
			if(val == valList.get(i)){ // This is supposed to be memory address.
				return true;
			}
		}
		return false;
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
