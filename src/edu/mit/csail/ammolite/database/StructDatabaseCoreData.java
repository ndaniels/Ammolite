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
	
		structsByHash = _structsByHash;
		files = _files;
		compressionType = _compressionType;
	}
}
