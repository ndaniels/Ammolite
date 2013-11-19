package speedysearch;

import org.openscience.cdk.interfaces.IAtomContainer;

public class MoleculeStructFactory {
	MoleculeStruct exemplar;
	
	public MoleculeStructFactory(MoleculeStruct _exemplar){
		exemplar = _exemplar;
	}

	public MoleculeStruct makeMoleculeStruct(IAtomContainer base){
		if( exemplar.getClass() == RingStruct.class ){
			return new RingStruct( base );
		} 
		if(exemplar.getClass() == CyclicStruct.class ){
			return new CyclicStruct( base );
		} 
		return new MoleculeStruct( base );
	}
}
