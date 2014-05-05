package edu.mit.csail.ammolite.aggregation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;

public class ClusterDatabase{

	private List<Cluster> cList;
	private IStructDatabase db;
	private double repBound;
	
	public ClusterDatabase(ClusterDatabaseCoreData data){
		cList = data.getClusterList();
		db = data.getDatabase();
		repBound = data.getRepBound();
	}
	
	public IStructDatabase getDatabase(){
		return db;
	}
	
	public List<Cluster> getClusterList(){
		return cList;
	}
	
	public String info(){
		StringBuilder sb = new StringBuilder();
		sb.append("Ammolite Cluster Database Info\n");
		sb.append(db.info());
		sb.append("Compressed with boundary: ");
		sb.append(repBound);
		sb.append("\n Cluster structure: \n");
		for(Cluster c: cList){
			sb.append(clusterToASCIIForest(c));
		}
		sb.append("\n");
		return sb.toString();
	}
	
	private String clusterToASCIIForest(Cluster c){
		return clusterToASCIIForest(c, 0);
	}
	
	private String clusterToASCIIForest(Cluster c, int depth){
		if( depth >= 8){
			return "(...)";
		}
		StringBuilder nameBuilder = new StringBuilder();
		nameBuilder.append("(");
		nameBuilder.append(c.getRep().getAtomCount());
		nameBuilder.append(", ");
		nameBuilder.append(c.order());
		if( c.getMembers().size() == 0){
			nameBuilder.append(", ");
			nameBuilder.append(c.getRep().getID());
		} else {
			nameBuilder.append(", ");
			nameBuilder.append(c.getMembers().size());
		}
		nameBuilder.append(")");
		String name = nameBuilder.toString();
		
		StringBuilder bufferBuilder = new StringBuilder();
		bufferBuilder.append("-");
		bufferBuilder.append(c.order());
		for(int i=2; i<name.length(); ++i){
			bufferBuilder.append("-");
		}
		String buffer = bufferBuilder.toString();
		
		List<String> kids = new ArrayList<String>();
		for(Cluster k: c.getMembers()){
			kids.add(clusterToASCIIForest(k));
		}
		
		StringBuilder out = new StringBuilder();
		out.append(name);
		out.append("\n");
		for(String kid: kids){
			for(String line: kid.split("\n")){
				out.append(buffer);
				out.append(line);
				out.append("\n");
			}
		}
		return out.toString();
	}

}

