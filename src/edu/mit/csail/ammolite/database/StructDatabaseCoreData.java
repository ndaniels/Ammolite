package edu.mit.csail.ammolite.database;

import java.io.Serializable;
import java.util.HashMap;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.compression.MolStruct;

public class StructDatabaseCoreData implements Serializable, IDatabaseCoreData  {


	private static final long serialVersionUID = -2500864197376846911L;
	private KeyListMap<Integer, MolStruct> structsByFingerprint;
	private ISDFSet files;
	private CompressionType compressionType;
	private String VERSION = "dev_aug6_2014";
	
	
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


	public KeyListMap<Integer, MolStruct> getFingerprintTable() {
		return structsByFingerprint;
	}

	public ISDFSet getSDFSet() {
		return files;
	}


	public CompressionType getCompressionType() {
		return compressionType;
	}

	@Override
	public String getVersion() {
		return VERSION;
	}

	@Override
	public void setFingerprintTable(KeyListMap<Integer, MolStruct> table) {
		structsByFingerprint = table;
		
	}

	@Override
	public void setSDFSet(ISDFSet set) {
		files = set;
		
	}

	@Override
	public void setCompressionType(CompressionType type) {
		compressionType = type;
		
	}

	@Override
	public void setVersion(String versionID) {
		VERSION = versionID;
		
	}
}

