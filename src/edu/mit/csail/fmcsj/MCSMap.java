/**
 * 
 */
package edu.mit.csail.fmcsj;

import java.util.HashMap;

import org.openscience.cdk.interfaces.IAtom;

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
		keyList.pop();
		valList.pop();
	}
	
	public IAtom getKey(IAtom val){
		int j =  valList.indexOf(val);
		if(j == -1){
			return null;
		}
		return keyList.get( j );
	}
	
	public IAtom getVal(IAtom key){
		int j =  keyList.indexOf(key);
		if(j == -1){
			return null;
		}
		return valList.get( j );
	}
	
	public void clear(){
		keyList.clear();
		valList.clear();
	}
	
	public boolean containsKey(IAtom key){
		return keyList.contains(key);
	}
	
	public boolean containsVal(IAtom val){
		return valList.contains(val);
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
