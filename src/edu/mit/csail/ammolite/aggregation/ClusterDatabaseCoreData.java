package edu.mit.csail.ammolite.aggregation;

import java.io.Serializable;
import java.util.List;

import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;

public class ClusterDatabaseCoreData implements Serializable {

	private String dbName;
	private List<Cluster> cList;
	private IStructDatabase db = null;
	private double repBound;
	
	public ClusterDatabaseCoreData(String _dbName, List<Cluster> _cList, double _repBound){
		dbName = _dbName;
		cList = _cList;
		repBound = _repBound;
	}
	
	public double getRepBound(){
		return repBound;
	}
	
	public IStructDatabase getDatabase(){
		if( db == null){
			db = StructDatabaseDecompressor.decompress(dbName);
		}
		return db;
	}
	
	public List<Cluster> getClusterList(){
		return cList;
	}
}
