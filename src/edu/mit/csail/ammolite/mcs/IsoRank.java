package edu.mit.csail.ammolite.mcs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SparseRealVector;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.ReactionRenderer;

import edu.mit.csail.ammolite.MolDrawer;
import edu.mit.csail.ammolite.compression.MoleculeStruct;
import edu.mit.csail.ammolite.utils.UtilFMCS;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.SimpleEdge;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.SparseUndirectedGraph;

public class IsoRank{
	
	SparseUndirectedGraph big;
	SparseUndirectedGraph lil;
	int n1;
	int n2;
	SparseUndirectedGraph mcs;
	double mapping_threshold;
	Map<Integer,Integer> map;
	
	final static double ZERO_THRESH = 0.0000001;
	final static double CONVERGENCE_THRESHOLD = .5;
	final static int MAX_ITERATIONS = 20;

	public IsoRank( MoleculeStruct m1, MoleculeStruct m2, double baseThresh){ 
		this( m1.getGraph(), m2.getGraph(), baseThresh);
	}
	
	public IsoRank( MoleculeStruct m1, MoleculeStruct m2){ 
		this( m1.getGraph(), m2.getGraph());
	}
	
	public IsoRank(SparseUndirectedGraph g1, SparseUndirectedGraph g2){
		this( g1, g2, 1.0);
	}
	
	public IsoRank(SparseUndirectedGraph g1, SparseUndirectedGraph g2, double base_threshold){
		if( g1.order() > g2.order() ){
			big = rebaseGraph(g1);
			lil = rebaseGraph(g2);
		} else {
			lil = rebaseGraph(g1);
			big = rebaseGraph(g2);
		}
		n1 = big.order();
		n2 = lil.order();
		
		mapping_threshold = base_threshold / (n1*n2);
	}
	
	public void calculate(){
		// RealMatrix A = buildNormOutA();
		SparseMatrix A = buildSparseA();
		//DualMatrix A = buildDualA();
		RealVector R = iterativelyBuildRMaxIter(A);
		map = buildMapping(R);
		
//		map = buildMapping(iterativelyBuildR(buildSparseA()));
		//mcs = buildMCS( map);
//		SparseUndirectedGraph multiMCS = buildMCS(map);
//		mcs = reduceToSingleComponent(multiMCS);

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
	
	private RealMatrix reshapeR(RealVector R, int rows, int cols){
		RealMatrix out = new BlockRealMatrix(rows, cols);

		for(int i=0; i<R.getDimension(); i++){
			int row = i / cols;
			int col = i - row*cols;
			out.setEntry(row, col, R.getEntry(i));
		}
		return out;
	}
	
	class Indices{
		public int row;
		public int col;
		public Indices(int r, int c){
			row = r;
			col = c;
		}
	}
	
	private Indices findMax(RealMatrix m){
		double max = 0.0;
		int rM = 0;
		int cM = 0;
		for(int r=0; r<m.getRowDimension(); r++){
			for(int c=0; c<m.getColumnDimension(); c++){
				double d = m.getEntry(r, c);
				if(d > max){
					max = d;
					rM = r;
					cM = c;
				}
			}
		}
		return new Indices(rM, cM);
		
	}
	
	private Map<Integer, Integer> buildMapping(RealMatrix R){
		Map<Integer, Integer> mapping = new HashMap<Integer, Integer>();
		double[] rZeros = new double[R.getColumnDimension()];
		double[] cZeros = new double[R.getRowDimension()];
		System.out.println("MAJOR_DELIMITER");
		while(true){
			System.out.println("MINOR_DELIMITER");
			System.out.println(R);
			Indices mI = findMax(R);
			int rM = mI.row;
			int cM = mI.col;
			if(R.getEntry(rM, cM) > mapping_threshold){
				mapping.put(rM, cM);
				R.setRow(rM, rZeros);
				R.setColumn(cM, cZeros);
			} else {
				break;
			}
		}
		return mapping;
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
	
	
	private RealVector standardBuildR(RealMatrix A){
		EigenDecomposition eD = new EigenDecomposition(A);
		return eD.getEigenvector(0);
	}

	private RealVector iterativelyBuildR(DualMatrix A){
		
		RealVector R = new OpenMapRealVector(A.getColumnDimension());
		
		R.mapAddToSelf(1.0);
		R.unitize();

		boolean converged = false;
		long startTime = System.currentTimeMillis();
		long runTime = 2000;
		
		while(!converged && !timedOut(startTime, runTime)){
			RealVector newR  = A.postOperate(R);
			try{
				newR.unitize();
			}
			catch(MathArithmeticException mae){
				R = newR;
				break;
			}
			double del = newR.getL1Distance(R);

			if( del < CONVERGENCE_THRESHOLD)
				converged = true;
			R = newR;
		}
		
		return R;
	}
	
	private RealVector iterativelyBuildRMaxIter(DualMatrix A){
		
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
	
	private RealVector iterativelyBuildRMaxIter(RealMatrix A){
		
		RealVector R = new OpenMapRealVector(A.getColumnDimension());
		
		R.mapAddToSelf(1.0);
		R.unitize();

		int iter = 0;
		
		while(iter < MAX_ITERATIONS){
			R  = A.operate(R);
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

	private RealVector iterativelyBuildR(RealMatrix A){
		
		RealVector R = new OpenMapRealVector(A.getColumnDimension());
		
		R.mapAddToSelf(1.0);
		R.unitize();

		boolean converged = false;
		long startTime = System.currentTimeMillis();
		long runTime = 2000;
		
		while(!converged && !timedOut(startTime, runTime)){
			System.out.println("BEGIN_BLOCK");
			RealVector newR  = A.operate(R);
			try{
				newR.unitize();
			}
			catch(MathArithmeticException mae){
				R = newR;
				System.out.println("Math Arithmetic Exception");
				break;
			}
			double del = newR.getL1Distance(R);

			System.out.println(del);
			System.out.println("END_BLOCK");
			if( del < ZERO_THRESH)
				converged = true;
			R = newR;
		}
		
		return R;
	}
	
	private RealVector iterativelyBuildR(SparseMatrix A){
		//System.out.println("BEGIN_BLOCK");
		RealVector R = new OpenMapRealVector(A.getColumnDimension());
		
		R.mapAddToSelf(1.0);
		R.unitize();

		boolean converged = false;
		long startTime = System.currentTimeMillis();
		long runTime = 2000;
		
		while(!converged && !timedOut(startTime, runTime)){
			RealVector newR  = A.postOperate(R);
			try{
				newR.unitize();
			}
			catch(MathArithmeticException mae){
				R = newR;
				//System.out.println("Math Arithmetic Exception");
				break;
			}
			double del = newR.getL1Distance(R);
			//System.out.println(del);
			if( del < CONVERGENCE_THRESHOLD)
				converged = true;
			R = newR;
		}
		//System.out.println("END_BLOCK");
		
		return R;
	}
	

	
	private RealMatrix adjacencyMatrix(Graph g){
		RealMatrix adj = new BlockRealMatrix(g.order(),g.order());
		for(int i: g.vertices()){
			for(int j: g.getNeighbors(i)){
				adj.addToEntry(i, j, 1.0);
			}
		}
		return adj;
	}
	
	private RealMatrix normout(RealMatrix in){
		RealMatrix out = new BlockRealMatrix(in.getRowDimension(), in.getColumnDimension());
		
		for(int row=0; row<in.getRowDimension(); row++){
			double rowSum = 0.0;
			for(double d: in.getRow(row)){
				rowSum += d;
			}
			int col = 0;
			for(double d: in.getRow(row)){
				out.setEntry(row, col, d/rowSum);
				col++;
			}
		}
		
		return out;
	}

	
	private RealMatrix buildA(){
		
		RealMatrix bigAdj = adjacencyMatrix(big);
		RealMatrix littleAdj = adjacencyMatrix(lil);

		RealMatrix A = new OpenMapRealMatrix( n1*n2,n1*n2);
		
		for(int bigRow=0; bigRow<n1; bigRow++){
			for(int bigCol=0; bigCol<n1; bigCol++){
				double bigEntry = bigAdj.getEntry(bigRow, bigCol);
				if(bigEntry > ZERO_THRESH){
					for(int littleRow=0; littleRow<n2; littleRow++){
						for(int littleCol=0; littleCol<n2; littleCol++){
							double littleEntry = littleAdj.getEntry(littleRow, littleCol);
							if(littleEntry > ZERO_THRESH){
								int row = bigRow*n2 + littleRow;
								int col = bigCol*n2 + littleCol;
								double val = 1.0 / (n1*n2);
								A.addToEntry(row, col, val);
							}
						}
					}
				}
				
			}
		}
		
//		System.out.println("MAJOR_DELIMITER");
//		System.out.println(iterativelyBuildR(A));
//		System.out.println("MINOR_DELIMITER");
//		System.out.println(iterativelyBuildR(normout(A)));
		
		return A;
	}
	
	private RealMatrix buildRealA(){

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
			for(int lilNeighbour: lilVertsToNeighbours.get(lilVertex)){
				for(int bigVertex: bigVertices){
					for(int bigNeighbour: bigVertsToNeighbours.get(bigVertex)){
						double val = 1.0 / (n1*n2);
						A.add(lilVertex*n1 +  bigVertex, lilNeighbour*n1 + bigNeighbour, val);
					}
				}
			}
		}

		return A.getMatrix();
	}
	
	private SparseMatrix buildSparseAdj(SparseUndirectedGraph g){
		SparseMatrix out = new SparseMatrix(g.order(), g.order());
		for(int v: g.vertices()){
			for(int n: g.getNeighbors(v)){
				out.add(v, n, 1.0);
			}
		}
		return out;
	}
	
	private DualMatrix buildDualA(){
		SparseMatrix adjLil = buildSparseAdj(lil);
		SparseMatrix adjBig = buildSparseAdj(big);
		DualMatrix A = new DualMatrix(adjLil, adjBig);
		return A;
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
	
	private RealMatrix rowNormalizeMatrix(RealMatrix A){
		int n = A.getRowDimension();
		RealMatrix diag = new BlockRealMatrix(n,n);
		for(int rowNum=0; rowNum<n; rowNum++){
			double sum = 0.0;
			for(double d: A.getRow(rowNum)){
				sum += d;
			}
			double inv = 1.0 / sum;
			diag.addToEntry(rowNum, rowNum, inv);
		}
		
		return diag.multiply(A);
		
	}
	
	
	private boolean timedOut(long startTime, long runTime){
		long currentTime = System.currentTimeMillis();
		return currentTime - startTime > runTime;
	}

	public int size() {
		return map.size();
	}

}
