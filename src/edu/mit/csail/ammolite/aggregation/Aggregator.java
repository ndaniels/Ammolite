package edu.mit.csail.ammolite.aggregation;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.Logger;
import edu.mit.csail.ammolite.compression.MoleculeStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;

public class Aggregator {
	private IStructDatabase db;
	private static double repBound;
	
	public Aggregator(String dbFilename, double _repBound){
		db = StructDatabaseDecompressor.decompress(dbFilename);
		repBound = _repBound;
	}
	
	public long aggregate(String filename){
		long startTime = System.currentTimeMillis();
		List<Cluster> cList = buildInitialClusterList();
		Logger.debug("1");
		int prevNumClusters = cList.size()+1;
		while( cList.size() != prevNumClusters && cList.size() > 10){
			Logger.debug("2*");
			Logger.debug(cList.size()+" clusters");
			prevNumClusters = cList.size();
			Logger.debug("3*");
			Matrix matrix = buildMatrix( cList);
			Logger.debug("4*");
			cList = singleFold( matrix, matrix.size());
		}
		Logger.debug("5");
		Logger.debug(cList.size()+" clusters");
		writeObjectToFile( filename, cList);
		return System.currentTimeMillis()-startTime;
	}
	
	private List<Cluster> singleFold(Matrix matrix, int originalSize){
		while( matrix.size()*2 > originalSize && !matrix.done()){
			//Logger.debug(matrix);
			Pair<Cluster> closest = matrix.getClosest();
			Cluster a = closest.left();
			Cluster b = closest.right();
			Cluster c = null;
			boolean success = false;
			if(a.order() == b.order()){
				//Logger.debug("a.order == b.order");
				c = new Cluster(a, repBound);
				success = c.addCandidate(b);
			} else if( a.order() < b.order()){
				//Logger.debug("a.order < b.order");
				c = b;
				success = b.addCandidate(a);
			} else if( a.order() > b.order()){
				//Logger.debug("a.order > b.order");
				c = a;
				success = a.addCandidate(b);
			}
			if( success){
				matrix.merge(a, b, c);
			} else{
				matrix.markFailed(a, b);
			}
		}
		return matrix.getClusterList();
	}
	
	private List<Cluster> buildInitialClusterList(){
		List<Cluster> clusterList = new ArrayList<Cluster>();
		Iterator<IAtomContainer> dbIter = db.iterator();
		while( dbIter.hasNext()){
			clusterList.add( new Cluster(dbIter.next(), repBound));
		}
		return clusterList;
	}
	
	private Matrix buildMatrix(List<Cluster> cList){
		return new Matrix( cList);
	}
	
	private static void writeObjectToFile(String object_filename, Object o){
		try{
			OutputStream file = new FileOutputStream( object_filename + ".cdb" );
			OutputStream buffer = new BufferedOutputStream( file );
			ObjectOutput output = new ObjectOutputStream( buffer );
			output.writeObject(o);
			output.close();
		}
		catch( IOException ex){
			ex.printStackTrace();
		}
		
	}

}
