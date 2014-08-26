package edu.mit.csail.ammolite.utils;

import org.openscience.cdk.interfaces.IAtomContainer;

public class MolUtils {
	
	public static PubchemID getPubID(IAtomContainer mol){
		return new PubchemID((String) mol.getProperty("PUBCHEM_COMPOUND_CID"));
	}
	
	public static StructID getStructID(IAtomContainer mol){
		return new StructID((String) mol.getProperty("PUBCHEM_COMPOUND_CID") + "_STRUCT");
	}

}
