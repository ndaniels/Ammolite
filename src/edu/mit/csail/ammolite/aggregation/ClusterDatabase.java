package edu.mit.csail.ammolite.aggregation;

import java.io.Serializable;
import java.util.List;

import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;

public class ClusterDatabase implements Serializable {

	private String dbName;
	private List<Cluster> cList;
	
	public ClusterDatabase(String _dbName, List<Cluster> _cList){
		dbName = _dbName;
		cList = _cList;
	}
	
	public IStructDatabase getDatabase(){
		return StructDatabaseDecompressor.decompress(dbName);
	}
	
	public List<Cluster> getClusterList(){
		return cList;
	}

}

