package fmcs;

import java.util.HashMap;

import org.openscience.cdk.interfaces.IAtom;

public class MCSRules {
	
	public MCSRules(){
		
	}
	
	public int count(IAtom atom){
		return 0;
	}
	
	public boolean allowMismatch(IAtom atom1, IAtom atom2){
		return false;
	}
}
