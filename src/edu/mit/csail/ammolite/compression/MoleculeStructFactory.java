package edu.mit.csail.ammolite.compression;

import java.io.Serializable;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.database.CompressionType;

public class MoleculeStructFactory implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private CompressionType compressionType;
	
	
	public MoleculeStructFactory(CompressionType _compressionType){
		compressionType = _compressionType;
	}
	
	public CompressionType getCompressionType(){
		return compressionType;
	}

	public IMolStruct makeMoleculeStruct(IAtomContainer base){
		 if( compressionType.equals( CompressionType.CYCLIC )){
			return new CyclicStruct( base );
		} else if( compressionType.equals( CompressionType.BASIC)){
			return new MolStruct( base );
		} else if(compressionType.equals( CompressionType.FULLY_LABELED)){
		    return new LabeledCyclicStruct(base);
		} else if(compressionType.equals( CompressionType.WEIGHTED)){
            return new LabeledWeightedCyclicStruct(base);
        } else if(compressionType.equals( CompressionType.BINARY_LABELED)){
            return new BinaryLabeledCyclicStruct(base);
        } else if(compressionType.equals( CompressionType.CONNECTION_2)){
            return new ConnectionStruct(base,2);
        } else if(compressionType.equals( CompressionType.CONNECTION_3)){
            return new ConnectionStruct(base,3);
        } else if(compressionType.equals( CompressionType.CONNECTION_4)){
            return new ConnectionStruct(base,4);
        } else if(compressionType.equals( CompressionType.CONNECTION_5)){
            return new ConnectionStruct(base,5);
        } else if(compressionType.equals( CompressionType.CONNECTION_6)){
            return new ConnectionStruct(base,6);
        }
		else {
			throw new IllegalArgumentException("Compression type not found");
		}

	}
}
