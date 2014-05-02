package edu.mit.csail.ammolite.aggregation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import edu.mit.csail.ammolite.Logger;
import edu.mit.csail.fmcsj.AbstractMCS;
import edu.mit.csail.fmcsj.FMCS;
import edu.mit.csail.fmcsj.MCSFinder;
import edu.mit.csail.fmcsj.SMSD;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;

public class Matrix {
	
	List<List<Double>> distances;
	List<Cluster> cList;
	Map<Cluster, Integer> clusterToInd = new HashMap<Cluster,Integer>();
	
	private static final Double FAIL = -1.0;
	
	public Matrix(List<Cluster> _cList){
		cList = _cList;
		populateDistArray();
	}
	
	public int size(){
		return cList.size();
	}
	
	public List<Cluster> getClusterList(){
		return cList;
	}
	
	
	private void populateDistArray(){
		long startTime = System.currentTimeMillis();
		Logger.debug("Calculating pairwise distances for "+cList.size()+" clusters");
		distances = new ArrayList<List<Double>>(cList.size());
		
		for(int i=0; i<cList.size(); ++i){
			List<Double> myList = new ArrayList<Double>(i);
			for(int j=0; j<i; ++j){
				myList.add(FAIL);
			}
			clusterToInd.put(cList.get(i), i);
			distances.add(myList);
		}
		
		//Logger.debug(distArrayToString());
		long totalMCSTime = 0;
		List<Long> mcsTimes = new ArrayList<Long>();
		List<Double> mcsDists = new ArrayList<Double>();
		for(ClusterDist cd: parDistances()){
			
			int aInd = clusterToInd.get(cd.a);
			int bInd = clusterToInd.get(cd.b);
			totalMCSTime += cd.time;
			mcsTimes.add(cd.time);
			mcsDists.add(cd.d);
			
			//Logger.debug(distArrayToString());
			//Logger.debug("aInd bInd "+aInd+" "+bInd);
			
			if( aInd > bInd){
				
				distances.get(aInd).set(bInd, cd.d);
			} else if (aInd < bInd){
				
				distances.get(bInd).set(aInd, cd.d);
			}

		}
		//Logger.debug(distArrayToString());
		long elapsedTime = System.currentTimeMillis() - startTime;
		Logger.debug("Done calculating pairwise distances. Took "+elapsedTime+" milliseconds, "+totalMCSTime+" in MCS");
//		Logger.debug("Time breakdown:");
//		for(int i=0; i<mcsDists.size(); ++i){
//			Logger.debug(i+" Time "+mcsTimes.get(i)+" Dist "+mcsDists.get(i));
//		}
	}
	
	private String distArrayToString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Distance Array\n");
		int i=0;
		for(List<Double> l: distances){
			sb.append(i);
			sb.append(" (");
			sb.append(l.size());
			sb.append("): ");
			sb.append(l);
			sb.append("\n");
			++i;
		}
		return sb.toString();
	}
	
	private List<ClusterDist> parDistances(){
		int numThreads = Runtime.getRuntime().availableProcessors();
		if( numThreads > 12){
			numThreads -= 4;
		}
		//numThreads = 1;
		Logger.debug("Using "+numThreads+" threads");
		ExecutorService service = Executors.newFixedThreadPool(numThreads);
		List<Future<ClusterDist>> futures = new ArrayList<Future<ClusterDist>>();

		for(int i=0; i<cList.size(); ++i){
			for(int j=0; j<i; ++j){
				final Cluster aClust = cList.get(i);
				final Cluster bClust = cList.get(j);
				final IAtomContainer a = new AtomContainer( aClust.getRep() );
				final IAtomContainer b = new AtomContainer( bClust.getRep() );
				Callable<ClusterDist> callable = new Callable<ClusterDist>(){
					
					public ClusterDist call() throws InterruptedException, ExecutionException{
						AbstractMCS myMCS = new MCSFinder( a, b);
						long runTime;
						runTime = myMCS.timedCalculate();
						int overlap = myMCS.size();
						double coeff = tanimotoCoeff( overlap, a.getAtomCount(), b.getAtomCount());
						return new ClusterDist(aClust,bClust,coeff,runTime);
					}
				};
				futures.add( service.submit( callable));
			}
		}
		
		List<ClusterDist> results = new ArrayList<ClusterDist>();
		for( Future<ClusterDist> future: futures){
			try {
				results.add( future.get());
			} catch (InterruptedException ie) {
				ie.printStackTrace();
				System.exit(1);
			} catch (ExecutionException ee) {
				ee.printStackTrace();
				System.exit(1);
			}

		}
		
		service.shutdown();
		return results;
	}
	
	private double tanimotoCoeff(int overlap, int a, int b){
		return ( (double) overlap) / ( a + b - overlap);
	}
	
	private double getDist(Cluster a, Cluster b){
		int aInd = clusterToInd.get(a);
		int bInd = clusterToInd.get(b);
		return getDist(aInd, bInd);
	}
	
	private double getDist( int aInd, int bInd){
		
		if(bInd == aInd){
			return 0.0;
		} else if( aInd > bInd){
			return distances.get(aInd).get(bInd);
		} else if(aInd < bInd){
			return distances.get(bInd).get(aInd);
		}
		return FAIL;
	}
	
	public Pair<Cluster> getClosest(){
		
		double max = 0.0;
		int aInd = 0;
		int bInd = 0;
		for(int i=0; i<cList.size(); ++i){
			for(int j=0; j<i; ++j){
				double dist = getDist(i,j);
				
				if( dist > max){
					aInd = i;
					bInd = j;
					max = dist;
				}
			}
		}
		
		return new Pair<Cluster>(cList.get(aInd), cList.get(bInd));
		
	}
	
	public void merge(Cluster a, Cluster b, Cluster combined){
		if(a == b){
			return;
		}
		//Logger.debug(distArrayToString());
		List<Double> myList = new ArrayList<Double>();
		for(Cluster c: cList){
			double d = getDist(a, c);
			d += getDist(b, c);
			d /= 2;
			myList.add(d);
		}
		distances.add(myList);
		
		
		int aInd = clusterToInd.get(a);
		int bInd = clusterToInd.get(b);
		
		if(aInd > bInd){
			int temp = bInd;
			bInd = aInd;
			aInd = temp;
		}
		// Delete Columns
		for(List<Double> l: distances){
			if(l.size() > aInd){
				l.remove(aInd);
				if(l.size() + 1 > bInd ){
					l.remove(bInd - 1);
				}
			}
		}
		// Delete Rows
		distances.remove(aInd);
		distances.remove(bInd - 1);
		
		// update cluster list
		cList.remove(aInd);
		cList.remove(bInd - 1);
		cList.add(combined);
		
		clusterToInd.clear();
		int i=0;
		// Reset Index map
		for(Cluster c: cList){
			clusterToInd.put(c, i);
			++i;
		}
	}
	
	
	public void markFailed(Cluster a, Cluster b){
		int aInd = clusterToInd.get(a);
		int bInd = clusterToInd.get(b);
		
		if(aInd > bInd){
			distances.get(aInd).set(bInd, FAIL);
		} else{
			distances.get(bInd).set(aInd, FAIL);
		}
		
	}
	
	public boolean done(){
		for(List<Double> l: distances){
			for(Double d: l){
				if(d > 0){
					return false;
				}
			}
		}
		return true;
	}
	
	public String toString(){
		return distArrayToString();
	}
	
	

}
