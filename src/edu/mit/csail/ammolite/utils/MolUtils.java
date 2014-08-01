package edu.mit.csail.ammolite.utils;

import org.openscience.cdk.interfaces.IAtomContainer;

public class MolUtils {
	
	public static String getPubID(IAtomContainer mol){
		return (String) mol.getProperty("PUBCHEM_COMPOUND_CID");
	}

}
