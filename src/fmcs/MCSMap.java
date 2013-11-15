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
	
	public void checkInvariants() throws RuntimeException {
		boolean a = keyList.size() == valList.size();
		boolean b = keyList.size() == this.size();
		if(!a && !b){
			throw new RuntimeException("Key Size != Val Size != Total Size");
		} else if (!a){
			throw new RuntimeException("Key Size != Val Size");
			
		} else if( !b){
			throw new RuntimeException("KeySize != Total Size , "+keyList.size()+" != "+this.size());
		}
	}
	public void push(IAtom key, IAtom val){
		this.checkInvariants();
		if(this.containsKey(key)){
			if(this.getVal(key).equals(val)){
				throw new RuntimeException(" double dupe");
			} else{
				throw new RuntimeException("single dupe");
			}
		}
		this.put(key, val);
		keyList.push(key);
		valList.push(val);
	}
	
	public void pop(){
		this.checkInvariants();
		IAtom key = keyList.pop();
		this.remove(key);
		valList.pop();
	}
	
	public IAtom getKey(IAtom val){
		this.checkInvariants();
		int j =  valList.indexOf(val);
		if(j == -1){
			return null;
		}
		IAtom o = keyList.get( j );
		return o;
	}
	
	public IAtom getVal(IAtom key){
		this.checkInvariants();
		return this.get(key);
	}
	
	@Override
	public void clear(){
		super.clear();
		keyList.clear();
		valList.clear();
	}
	
	public MCSMap deepCopy(){
		this.checkInvariants();
		MCSMap out = new MCSMap();
		for(IAtom k: keyList){
			IAtom v = this.get(k);
			out.push(k, v);
		}
		out.checkInvariants();
		return out;
	}
	
	
	
}
