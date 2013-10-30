/**
 * 
 */
package fmcs;

import java.util.HashMap;

import org.openscience.cdk.interfaces.IAtom;

/**
 * @author DC
 *
 */
public class MCSMap extends HashMap<IAtom, IAtom> {

	private MCSList<IAtom> keyList = new MCSList<IAtom>();
	private MCSList<IAtom> valList = new MCSList<IAtom>();
	
	public void push(IAtom key, IAtom val){
		this.put(key, val);
		keyList.push(key);
		valList.push(val);
	}
	
	public void pop(){
		IAtom key = keyList.pop();
		this.remove(key);
		valList.pop();
	}
	
	public IAtom getKey(IAtom val){
		return keyList.get( valList.indexOf(val));
	}
	
	public IAtom getVal(IAtom key){
		return this.get(key);
	}
	
}
