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

import edu.mit.csail.ammolite.Logger;
import edu.mit.csail.fmcsj.MCS;

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
		for(ClusterDist cd: parDistances()){
			
			int aInd = clusterToInd.get(cd.a);
			int bInd = clusterToInd.get(cd.b);
			totalMCSTime += cd.time;
			
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
		numThreads = 4;
		Logger.debug("Using "+numThreads+" threads");
		ExecutorService service = Executors.newFixedThreadPool(numThreads);
		List<Future<ClusterDist>> futures = new ArrayList<Future<ClusterDist>>();

		for(int i=0; i<cList.size(); ++i){
			for(int j=0; j<i; ++j){
				final Cluster a = cList.get(i);
				final Cluster b = cList.get(j);
				Callable<ClusterDist> callable = new Callable<ClusterDist>(){
					
					public ClusterDist call() throws Exception {
						MCS myMCS = new MCS( a.getRep(), b.getRep());
						long time = myMCS.calculate();
						int overlap = myMCS.size();
						double coeff = tanimotoCoeff( overlap, a.getRep().getAtomCount(), b.getRep().getAtomCount());
						return new ClusterDist(a,b,coeff,time);
					}
				};
				futures.add( service.submit( callable));
			}
		}
		
		List<ClusterDist> results = new ArrayList<ClusterDist>();
		for( Future<ClusterDist> future: futures){
			try {
				results.add( future.get());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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