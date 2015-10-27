package edu.mit.csail.ammolite.utils;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import edu.mit.csail.ammolite.compression.IMolStruct;
import edu.mit.csail.ammolite.compression.MolStruct;

public class MolUtils {
	
    @Deprecated
	public static PubchemID getPubID(IAtomContainer mol){
		return new PubchemID((String) mol.getProperty("PUBCHEM_COMPOUND_CID"));
	}
	
	public static StructID getStructID(IAtomContainer mol){
	    AmmoliteID ammID = getAmmoliteID(mol);
		return new StructID(ammID.toString() + "_STRUCT");
	}
	
	public static AmmoliteID getAmmoliteID(IAtomContainer mol){
	   
	    if(mol.getProperty("AMMOLITE_COMPOUND_CID") != null){
	        return new AmmoliteID((String) mol.getProperty("AMMOLITE_COMPOUND_CID"));
	    } else if(mol.getProperty("PUBCHEM_COMPOUND_CID") != null){
	        return new AmmoliteID((String) mol.getProperty("PUBCHEM_COMPOUND_CID") + "_AMMOLITE");
	    } else {
	        return new AmmoliteID(String.valueOf(mol.hashCode()) + "_AMMOLITE");
	    }
	}
	
	public static ID getUnknownOrID(IAtomContainer mol){
	    ID out;
        try{
            if( mol instanceof IMolStruct){
                out = getStructID(mol);
            } else {
               out = getAmmoliteID(mol);
            }
        } catch(NullPointerException ignore){
            out = new UnknownID();
        }
        return out;
	}
	
	public static int getAtomCountNoHydrogen(IAtomContainer mol){
        return AtomContainerManipulator.removeHydrogens(mol).getAtomCount();
    }

}
