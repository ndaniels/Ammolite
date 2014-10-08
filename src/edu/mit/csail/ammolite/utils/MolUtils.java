package edu.mit.csail.ammolite.utils;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.MolStruct;

public class MolUtils {
	
	public static PubchemID getPubID(IAtomContainer mol){
		return new PubchemID((String) mol.getProperty("PUBCHEM_COMPOUND_CID"));
	}
	
	public static StructID getStructID(IAtomContainer mol){
		return new StructID((String) mol.getProperty("PUBCHEM_COMPOUND_CID") + "_STRUCT");
	}
	
	public static ID getUnknownOrID(IAtomContainer mol){
	    ID out;
        try{
            if( mol instanceof MolStruct){
                out = getStructID(mol);
            } else {
               out = getPubID(mol);
            }
        } catch(NullPointerException ignore){
            out = new UnknownID();
        }
        return out;
	}

}
