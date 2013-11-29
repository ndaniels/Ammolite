package speedysearch;

import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

public class StructDatabase {
	
	public StructDatabase(){
		
	}
	
	public List<MoleculeStruct> getStructsByHash(int hash){
		return null;
	}
	
	public IAtomContainer getMolecule(String pubchemID){
		return null;
	}
	
	public Iterator<MoleculeStruct> iterator(){
		return null;
	}
	
	
}
