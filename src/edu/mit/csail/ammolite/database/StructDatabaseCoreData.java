package edu.mit.csail.ammolite.database;

import java.io.Serializable;
import java.util.HashMap;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.compression.MolStruct;

public class StructDatabaseCoreData implements Serializable {


	private static final long serialVersionUID = -2500864197376846911L;
	public KeyListMap<Integer, MolStruct> structsByHash;
	public SDFSet files;
	public CompressionType compressionType;
	
	
	public StructDatabaseCoreData(	KeyListMap<Integer, MolStruct> _structsByHash, 
									SDFSet _files, 
									CompressionType _compressionType){
	
		if(_structsByHash == null)
			throw new NullPointerException("Null structure set.");
		structsByHash = _structsByHash;
		if(_files == null)
			throw new NullPointerException("Null file set.");
		files = _files;
		if(_compressionType == null)
			throw new NullPointerException("Null compression type.");
		compressionType = _compressionType;
	}
}
