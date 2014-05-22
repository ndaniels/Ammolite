package edu.mit.csail.ammolite.mcs;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SparseRealVector;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.MolDrawer;
import edu.mit.csail.ammolite.compression.MoleculeStruct;
import edu.mit.csail.ammolite.utils.UtilFMCS;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.SimpleEdge;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.SparseUndirectedGraph;

public class IsoRank{
	
	Graph g1;
	Graph g2;
	Map<Integer, Integer> map;
	Graph mcs;
	
	final static double ZERO_THRESH = 0.000000000001;
	
	
	public IsoRank( MoleculeStruct m1, MoleculeStruct m2){ 
		this( m1.getGraph(), m2.getGraph());
	}
	
	public IsoRank(Graph<Edge> _g1, Graph<Edge> _g2){
		g1 = _g1;
		g2 = _g2;
	}
	
	public void calculate(){
		RealMatrix A = buildA();
		RealVector R = iterativelyBuildR(A);
		map = buildMapping(R);
//		mcs = buildGraph( map);
//		if(Math.random() < 2.0){
//			System.out.println("g1:");
//			System.out.println(g1);
//			System.out.println("g2:");
//			System.out.println(g2);
//			System.out.println("map:");
//			System.out.println(map);
//			System.out.println("mcs:");
//			System.out.println(mcs);
//		}
	}
	
	private Map<Integer, Integer> buildMapping(RealVector R){

		Map<Integer, Integer> mapping = new HashMap<Integer, Integer>();

		while(true){

			int maxDex = R.getMaxIndex();

			if(R.getEntry(maxDex) > ZERO_THRESH){
				int i = maxDex / g2.order();
				int j = maxDex - i*g2.order();
				mapping.put(i, j);
				R.setEntry(maxDex, 0.0);
				for(int nullifier=i*g2.order(); nullifier<(i+1)*g2.order(); nullifier++){
					R.setEntry(nullifier, 0.0);
				}
				for(int nullifier=j; nullifier<R.getDimension(); nullifier+=g2.order()){
					R.setEntry(nullifier, 0.0);
				}
			} else {
				break;
			}
		}
		return mapping;
	}
	
	private Graph buildGraph(Map<Integer, Integer> mapping){
		Graph<Edge> mcs = new SparseUndirectedGraph();
//		for(int i: mapping.keySet()){
//			mcs.add(i);
//		}
//		for(int i:mcs.vertices()){
//			for(int j: g1.getNeighbors(i)){
//				System.out.print("a");
//				int u = mapping.get(i);
//				System.out.print("b");
//				int v = mapping.get(j);
//				System.out.print("c");
//				if(g1.contains(i, j) && g2.contains(u, v)){
//					mcs.add(new SimpleEdge(i,j));
//				}
//				System.out.print("d");
//			}
//
//		}
		return mcs;
		
	}

	private RealVector iterativelyBuildR(RealMatrix A){
		
		RealVector R = new OpenMapRealVector(A.getColumnDimension());
		
		R.mapAddToSelf(1.0);

		boolean converged = false;
		long startTime = System.currentTimeMillis();
		long runTime = 2000;
		
		while(!converged && !timedOut(startTime, runTime)){
			RealVector newR  = A.operate(R);
			newR.unitize();
			double del = newR.getL1Distance(R);
			if( del < ZERO_THRESH)
				converged = true;
			R = newR;
		}
		
		return R;
	}
	
	private RealMatrix buildA(){
		int n = g1.order() * g2.order();
		RealMatrix A = new BlockRealMatrix( n,n);
		
		for(int i=0; i<g1.order(); ++i){
			for(int j=0; j<g2.order(); ++j){
				for(int u=0; u<i; ++u){
					for(int v=0; v<j; ++v){
						if( g1.contains(i, u) && g2.contains(j,v)){
							double N = 1.0 / g1.getNeighbors(u).size() * g2.getNeighbors(v).size();
							A.addToEntry(i*j+j, u*v+v, N);
							A.addToEntry(u*v+v, i*j+j, N);
						}
					}
				}
			}
		}
		
		return A;
	}
	
	
	private boolean timedOut(long startTime, long runTime){
		long currentTime = System.currentTimeMillis();
		return currentTime - startTime > runTime;
	}

	public int size() {
		return map.size();
	}

}
