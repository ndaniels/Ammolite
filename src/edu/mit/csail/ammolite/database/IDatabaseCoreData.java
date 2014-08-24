package edu.mit.csail.ammolite.database;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.compression.MolStruct;

public interface IDatabaseCoreData {
	
	public KeyListMap<Integer, MolStruct> getFingerprintTable();
	public ISDFSet getSDFSet();
	public CompressionType getCompressionType();
	public String getVersion();
	public String getName();
	
	public void setFingerprintTable(KeyListMap<Integer, MolStruct> table);
	public void setSDFSet(ISDFSet set);
	public void setCompressionType(CompressionType type);
	public void setVersion(String versionID);
	public void setName(String name);

}
