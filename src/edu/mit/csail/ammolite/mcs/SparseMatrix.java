package edu.mit.csail.ammolite.mcs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.MatrixDimensionMismatchException;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import edu.mit.csail.ammolite.KeyListMap;

public class SparseMatrix {
	
	private List<Triple> els = new ArrayList<Triple>();
	private KeyListMap<Integer,Triple> elsByRow = new KeyListMap<Integer,Triple>(10);
	private int numRows;
	private int numCols;
	private Map<Integer, Double> rowSums = new HashMap<Integer, Double>();
	
	class Triple {
		public int row;
		public int col;
		public double val;
		
		public Triple(int _row, int _col, double _val){
			row = _row;
			col = _col;
			val = _val;
		}
	};
	
	public SparseMatrix(int _numRows, int _numCols){
		numRows = _numRows;
		numCols = _numCols;
	}
	
	public double getEntry(int row, int col){
		checkIndices(row, col);
		if( elsByRow.containsKey(row)){
			for(Triple t: elsByRow.get(row)){
				if(t.col == col){
					return t.val;
				}
			}
		}
		return 0.0;
	}
	
	private void checkIndices(int row, int col){
		if( row >= numRows || col >= numCols){
			throw new IndexOutOfBoundsException();
		}
	}
	
	public void add(int row, int col, double val){
		checkIndices(row, col);
		if(rowSums.containsKey(row)){
			double currentSum = rowSums.get(row);
			rowSums.put(row, val + currentSum);
		} else {
			rowSums.put(row, val);
		}
		Triple entry = new Triple(row, col, val);
		els.add(entry);
		elsByRow.add(entry.row, entry);
	}
	
	public RealVector postOperate(RealVector v){
		if( v.getDimension() != numCols){
			throw new MatrixDimensionMismatchException(1, v.getDimension(), 1, numCols);
		}
		
		RealVector out = new OpenMapRealVector(v.getDimension());
		
		for(int rowIndex: elsByRow.keySet()){
			double val = 0.0;
			for(Triple t: elsByRow.get(rowIndex)){
				val += t.val * v.getEntry(t.col);
			}
			out.setEntry(rowIndex, val);
		}
		
		return out;
		
	}
	
	public RealMatrix postMultiply(RealMatrix M){
		if( M.getRowDimension() != numCols){
			throw new MatrixDimensionMismatchException(1, M.getRowDimension(), 1, numCols);
		}
		RealMatrix out = new BlockRealMatrix(this.getRowDimension(), M.getColumnDimension());
		for(int rowIndex: elsByRow.keySet()){
			for(int colIndex=0; colIndex<M.getColumnDimension(); colIndex++){
				double val = 0.0;
				for(Triple t: elsByRow.get(rowIndex)){
					val += t.val * M.getEntry(t.col, colIndex);
				}
				out.setEntry(rowIndex, colIndex, val);
			}
		}
		return out;
	}
	
	public RealMatrix getMatrix(){
		RealMatrix out = new BlockRealMatrix(numRows, numCols);
		for(Triple trip: els){
			out.setEntry(trip.row, trip.col, trip.val);
		}
		return out;
		
	}
	
	public RealMatrix getFullyNormalizedMatrix(){
		double normalizer = 0.0;
		for( double d: rowSums.values()){
			normalizer += d;
		}
		RealMatrix out = new BlockRealMatrix(numRows, numCols);
		for(Triple trip: els){
			out.setEntry(trip.row, trip.col, trip.val / normalizer);
		}
		return out;
	}
	
	public RealMatrix getNormOut(){
		RealMatrix out = new BlockRealMatrix(numRows, numCols);
		for(Triple trip: els){
			double denominator = rowSums.get(trip.row);
			out.setEntry(trip.row, trip.col, trip.val / denominator);
		}
		return out;
	}
	
	public int getColumnDimension(){
		return numCols;
	}
	
	public int getRowDimension(){
		return numRows;
	}
	

}
