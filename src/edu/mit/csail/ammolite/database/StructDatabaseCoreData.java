package edu.mit.csail.ammolite.database;

import java.io.Serializable;
import java.util.HashMap;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.compression.MolStruct;

public class StructDatabaseCoreData implements Serializable {


	private static final long serialVersionUID = -2500864197376846911L;
	public KeyListMap<Integer, MolStruct> structsByFingerprint;
	public ISDFSet files;
	public CompressionType compressionType;
	public String VERSION = "dev_aug5_2014";
	
	
	public StructDatabaseCoreData(	KeyListMap<Integer, MolStruct> _structsByFingerprint, 
									ISDFSet _files, 
									CompressionType _compressionType){
	
		if(_structsByFingerprint == null)
			throw new NullPointerException("Null structure set.");
		structsByFingerprint = _structsByFingerprint;
		if(_files == null)
			throw new NullPointerException("Null file set.");
		files = _files;
		if(_compressionType == null)
			throw new NullPointerException("Null compression type.");
		compressionType = _compressionType;
	}
	
	public StructDatabaseCoreData(	KeyListMap<Integer, MolStruct> _structsByFingerprint, 
			ISDFSet _files, 
			CompressionType _compressionType,
			String _VERSION){

	this(_structsByFingerprint, _files, _compressionType);
	VERSION = _VERSION;
}
}
