package edu.mit.csail.ammolite.compression;

import java.io.Serializable;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.database.CompressionType;

public class MoleculeStructFactory implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private CompressionType compressionType;
	
	
	public MoleculeStructFactory(CompressionType compressionType){
		
	}
	
	public CompressionType getCompressionType(){
		return compressionType;
	}

	public MoleculeStruct makeMoleculeStruct(IAtomContainer base){
		if( compressionType == CompressionType.RING ){
			return new RingStruct( base );
		} 
		if( compressionType == CompressionType.CYCLIC ){
			return new CyclicStruct( base );
		} 

		return new MoleculeStruct( base );
	}
}
