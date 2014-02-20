package edu.mit.csail.ammolite.compression;

import java.io.Serializable;

import org.openscience.cdk.interfaces.IAtomContainer;

public class MoleculeStructFactory implements Serializable{

	private static final long serialVersionUID = 1L;
	
	public MoleculeStruct exemplar;
	
	
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
		if( exemplar.getClass() == FragStruct.class){
			return new FragStruct( base );
		}
		return new MoleculeStruct( base );
	}
}
