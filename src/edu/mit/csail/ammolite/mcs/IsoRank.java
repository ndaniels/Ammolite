package edu.mit.csail.ammolite.mcs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealVector;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.ucla.sspace.graph.SimpleEdge;
import edu.ucla.sspace.graph.SparseUndirectedGraph;

public class IsoRank{
	
	SparseUndirectedGraph big;
	SparseUndirectedGraph lil;
	int n1;
	int n2;
	SparseUndirectedGraph mcs;
	double mapping_threshold;
	Map<Integer,Integer> map;

	final static int MAX_ITERATIONS = 20;

	public IsoRank( MolStruct m1, MolStruct m2){ 
		this( m1.getGraph(), m2.getGraph());
	}


	public IsoRank(SparseUndirectedGraph g1, SparseUndirectedGraph g2){
		if( g1.order() > g2.order() ){
			big = rebaseGraph(g1);
			lil = rebaseGraph(g2);
		} else {
			lil = rebaseGraph(g1);
			big = rebaseGraph(g2);
		}
		n1 = big.order();
		n2 = lil.order();
		mapping_threshold = 14.0 / (n1*n2);
	}
	
	public void calculate(){
		SparseMatrix A = buildSparseA();
		RealVector R = iterativelyBuildRMaxIter(A);
		map = buildMapping(R);
	}
	
	public SparseUndirectedGraph buildMCS(Map<Integer,Integer> map){
		SparseUndirectedGraph g = new SparseUndirectedGraph();
		
		Set<Integer> vertsInG1 = map.keySet();
		
		
		for(int v: vertsInG1){
			g.add(v);
		}
		
		for(int v: vertsInG1){
			for(int n: vertsInG1){
				if(lil.contains(v,n)){
					g.add(new SimpleEdge(v,n));
				}
			}
		}	
		
		return g;
	}
	
	public SparseUndirectedGraph reduceToSingleComponent(SparseUndirectedGraph gIn){
		List<HashSet<Integer>> components = new ArrayList<HashSet<Integer>>();
		boolean added;
		
		for(int v: gIn.vertices()){
			added = false;
			for(int n: gIn.getNeighbors(v)){
				for(HashSet<Integer> component: components){
					if(component.contains(n)){
						component.add(v);
						added = true;
						break;
					}
				} 
				if( added) 
					break;
			}
			if( !added){
				HashSet<Integer> newComponent = new HashSet<Integer>();
				newComponent.add(v);
				components.add(newComponent);
			}
		}
			
		HashSet<Integer> maxComp = new HashSet<Integer>();
		for(HashSet<Integer> comp: components){
			if(comp.size() > maxComp.size()){
				maxComp = comp;
			}
		}
		
		SparseUndirectedGraph gOut = new SparseUndirectedGraph();
		for(int vertex: maxComp){
			gOut.add(vertex);
		}
		for(int v: maxComp){
			for(int n: maxComp){
				if(gIn.contains(v, n)){
					gOut.add(new SimpleEdge(v,n));
				}
			}
		}
		return gOut;
	}
	
	private SparseUndirectedGraph rebaseGraph(SparseUndirectedGraph graph){
		SparseUndirectedGraph newGraph = new SparseUndirectedGraph();
		
		int outer = 0;
		for(int i: graph.vertices()){
			newGraph.add(outer);
			int inner = 0;
			for(int j: graph.vertices()){
				newGraph.add(inner);
				if(graph.contains(i, j))
					newGraph.add(new SimpleEdge(outer,inner));
				inner++;
			}
			outer++;
		}
		return graph;
	}
	

	
	private Map<Integer, Integer> buildMapping(RealVector R){
		Map<Integer, Integer> mapping = new HashMap<Integer, Integer>();

		while(true){

			int maxDex = R.getMaxIndex();

			if(R.getEntry(maxDex) > mapping_threshold){
				int lilVert = maxDex / n1;
				int bigVert = maxDex - lilVert*n1;
				mapping.put(lilVert, bigVert);
				R.setEntry(maxDex, 0.0);
				for(int nullifier=lilVert*n1; nullifier<(lilVert+1)*n1; nullifier++){
					R.setEntry(nullifier, 0.0);
				}
				for(int nullifier=bigVert; nullifier<R.getDimension(); nullifier+=n1){
					R.setEntry(nullifier, 0.0);
				}
			} else {
				break;
			}
		}
		return mapping;
	}
	
	

	
	private RealVector iterativelyBuildRMaxIter(SparseMatrix A){
		
		RealVector R = new OpenMapRealVector(A.getColumnDimension());
		
		R.mapAddToSelf(1.0);
		R.unitize();

		int iter = 0;
		
		while(iter < MAX_ITERATIONS){
			R  = A.postOperate(R);
			try{
				R.unitize();
			}
			catch(MathArithmeticException mae){
				break;
			}
			iter++;
		}
		
		return R;
	}
	
	
	private SparseMatrix buildSparseA(){

		SparseMatrix A = new SparseMatrix( n1*n2,n1*n2);
		
		int[] lilVertices = lil.vertices().toPrimitiveArray();
		int[] bigVertices = big.vertices().toPrimitiveArray();
		
		List<int[]> lilVertsToNeighbours = new ArrayList<int[]>(lilVertices.length);
		for(int i=0; i<lilVertices.length; i++){
			lilVertsToNeighbours.add(null);
		}
		for(int lilVertex: lilVertices){
			lilVertsToNeighbours.set(lilVertex, lil.getNeighbors(lilVertex).toPrimitiveArray());
		}
		
		List<int[]> bigVertsToNeighbours = new ArrayList<int[]>(bigVertices.length);
		for(int i=0; i<bigVertices.length; i++){
			bigVertsToNeighbours.add(null);
		}
		for(int bigVertex: bigVertices){
			bigVertsToNeighbours.set(bigVertex, big.getNeighbors(bigVertex).toPrimitiveArray());
		}
		
		for(int lilVertex: lilVertices){
			int numLilNeighbours = lilVertsToNeighbours.get(lilVertex).length;
			for(int lilNeighbour: lilVertsToNeighbours.get(lilVertex)){
				for(int bigVertex: bigVertices){
					int numBigNeighbours = bigVertsToNeighbours.get(bigVertex).length;
					for(int bigNeighbour: bigVertsToNeighbours.get(bigVertex)){
						//double val = 1.0 / (numLilNeighbours*numBigNeighbours);
						double val = 1.0;
						A.add(lilVertex*n1 +  bigVertex, lilNeighbour*n1 + bigNeighbour, val);
					}
				}
			}
		}

		return A;
	}

	public int size() {
		return map.size();
	}

}
