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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;
import edu.mit.csail.ammolite.utils.Logger;
import edu.mit.csail.ammolite.utils.Pair;
import edu.mit.csail.ammolite.utils.ParallelUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public class Aggregator {
	private String dbFilename;
	private IStructDatabase db;
	private static double repBound;
	
	public Aggregator(String _dbFilename, double _repBound){
		dbFilename = _dbFilename;
		db = StructDatabaseDecompressor.decompress(dbFilename);
		repBound = _repBound;
	}
	
	public long aggregate(String filename){
		return aggregate(filename, false);
	}
	
	public long aggregate(String filename, boolean useLinearizedClustering){
		long startTime = System.currentTimeMillis();
		
		List<Cluster> cList = buildInitialClusterList();
		boolean converged = false;
		int prevNumClusters = -1;
		
		while(!converged && cList.size() > 0){
			Logger.debug(cList.size()+" clusters");
			prevNumClusters = cList.size();

			if( !useLinearizedClustering ){
				Matrix matrix = buildMatrix( cList);
				cList = singleFold( matrix);
				
			} else {
				long lineStartTime = System.currentTimeMillis();
				cList = linearFold( cList);
				long lineTime = System.currentTimeMillis() - lineStartTime;
				Logger.debug("Did parallel pseudolinear clustering in "+lineTime);
			}
			converged = (cList.size() == prevNumClusters);
		}
		
		if(converged){
			Logger.debug("converged with "+cList.size()+" clusters");
		} else {
			Logger.debug("ended with "+cList.size()+" clusters");
		}
		
		ClusterDatabaseCoreData cDB = new ClusterDatabaseCoreData(dbFilename, cList, repBound);
		writeObjectToFile( filename, cDB);
		
		return System.currentTimeMillis()-startTime;
	}
	
	private List<Cluster> linearFold(List<Cluster> cList){
		
		List<Cluster> newCList = new ArrayList<Cluster>();
		for(Cluster originalCluster: cList){
			boolean added = false;
			for(int i=0; i<newCList.size(); ++i){
				Cluster comparisonCluster = newCList.get(i);
				if( comparisonCluster.order() == originalCluster.order()){
					Cluster newCluster = new Cluster( comparisonCluster, repBound);
					added = newCluster.addCandidate(originalCluster);
					if(added){
						newCList.remove(i);
						newCList.add(newCluster);
						break;
					}
					
				} else if ( comparisonCluster.order() < originalCluster.order()){
					added = originalCluster.addCandidate(comparisonCluster);
					if( added){
						newCList.remove(i);
						newCList.add(originalCluster);
						break;
					}
					
				} else if ( comparisonCluster.order() > originalCluster.order()){
					added = comparisonCluster.addCandidate(originalCluster);
					if( added){
						break;
					}
				}
			}
			if(!added){
				newCList.add(originalCluster);
			}
		}
		return newCList;
	}
	
	private List<Cluster> parallelLinearFold(List<Cluster> cList){
		
		
		class Out {
			public boolean added = false;
			public Cluster clusterToAdd = null;
			public int indexOfClusterToRemove;
			
		}
		
		List<Cluster> newCList = new ArrayList<Cluster>();
		for(int j=0; j<cList.size(); ++j){
			
			List<Callable<Out>> callList = new ArrayList<Callable<Out>>(newCList.size());
			
			final Cluster originalCluster = cList.get(j);
			
			for(int i=0; i<newCList.size(); ++i){
				
				final Cluster comparisonCluster = newCList.get(i);
				final int index = i;
				
				Callable<Out> callable = new Callable<Out>(){
					
					public Out call() throws InterruptedException, ExecutionException{
						Out out = new Out();
						if( comparisonCluster.order() >= originalCluster.order()){
							Cluster newCluster = new Cluster( comparisonCluster, repBound);
							out.added = newCluster.addCandidate(originalCluster);
							out.clusterToAdd = newCluster;
							out.indexOfClusterToRemove = index;
							
						} else if ( comparisonCluster.order() < originalCluster.order()){
							Cluster newCluster = new Cluster( originalCluster, repBound);
							out.added = newCluster.addCandidate(comparisonCluster);
							out.clusterToAdd = newCluster;
							out.indexOfClusterToRemove = index;	
						}
						if( out.added){
							return out;
						}
						return null;
					}
				};
				callList.add(callable);
			}
			Out result =  ParallelUtils.parallelSingleExecution(callList);
			
			if(result != null){
				newCList.remove(result.indexOfClusterToRemove);
				newCList.add(result.clusterToAdd);
			} else {
				newCList.add(originalCluster);
			}
			
		}
		return newCList;
	}
	
	private List<Cluster> singleFold(Matrix matrix){
		int originalSize = matrix.size();
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
				success = c.addCandidate(a);
			} else if( a.order() > b.order()){
				//Logger.debug("a.order > b.order");
				c = a;
				success = c.addCandidate(b);
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
		Iterator<MolStruct> dbIter = db.iterator();
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
			OutputStream file = new FileOutputStream( object_filename + ".clusters.adb" );
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
