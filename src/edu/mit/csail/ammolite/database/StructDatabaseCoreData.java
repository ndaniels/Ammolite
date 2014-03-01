package edu.mit.csail.ammolite.database;

import java.io.Serializable;
import java.util.HashMap;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.compression.MoleculeStruct;

public class StructDatabaseCoreData implements Serializable {


	private static final long serialVersionUID = -2500864197376846911L;
	public KeyListMap<Integer, MoleculeStruct> structsByHash;
	public HashMap<String, FilePair> fileLocsByID;
	public CompressionType compressionType;
	
	
	public StructDatabaseCoreData(	KeyListMap<Integer, MoleculeStruct> _structsByHash, 
		HashMap<String, FilePair> _fileLocsByID, 
		CompressionType _compressionType){
	
		structsByHash = _structsByHash;
		fileLocsByID = _fileLocsByID;
		compressionType = _compressionType;
	}
}
