package edu.mit.csail.ammolite.mcs;

import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.MatrixDimensionMismatchException;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import edu.mit.csail.ammolite.utils.MCSUtils;

public class DualMatrix {
	SparseMatrix A;
	SparseMatrix B;
	int a;
	int b;
	private static final double ZERO = 0.00000000001;
	
	public DualMatrix(SparseMatrix major, SparseMatrix minor){
		A= major;
		B= minor;
		a= major.getColumnDimension();
		b= minor.getColumnDimension();
	}
	
	public RealVector postOperate(RealVector R){
		if( R.getDimension() != a*b){
			throw new MatrixDimensionMismatchException(1, R.getDimension(), 1, a*b);
		}
		
		RealMatrix rectR = matrixFromVector(R);
//		System.out.println("rectR");
//		UtilFMCS.prettyPrintMatrix(rectR);
		
		RealMatrix T = B.postMultiply(rectR);
//		System.out.println("T");
//		UtilFMCS.prettyPrintMatrix(T);
		
		RealMatrix newR = A.postMultiply(T.transpose());
//		System.out.println("newR");
//		UtilFMCS.prettyPrintMatrix(newR);
		
		RealVector out = vectorFromMatrix(newR);
		return out;
	}
	
	
	private void checkIndices(int row, int col){
		if( row >= a*b || col >= a*b){
			throw new IndexOutOfBoundsException();
		}
	}
	
	
	public double getEntry(int row, int col){
		checkIndices(row, col);
		
		int rowA = row / b;
		int colA = col / b;
		int rowB = row % b;
		int colB = col % b;
		double entryA = A.getEntry(rowA, colA);
		if(entryA > ZERO){
			double entryB = B.getEntry(rowB,colB);
			if( entryB > ZERO){
				return entryA*entryB;
			}
		}
		return 0.0;
	}
	
	
	private RealMatrix matrixFromVector(RealVector R){
		RealMatrix rectR = new BlockRealMatrix(b,a);
		for(int row=0; row<b; row++){
			for(int col=0; col<a; col++){
				int vec = col * b + row;
				rectR.setEntry(row,col, R.getEntry(vec));
			}
		}
		return rectR;
	}
	
	private RealVector vectorFromMatrix(RealMatrix rectR){
		RealVector R = new OpenMapRealVector(a*b);
		for(int row=0; row<a; row++){
			for(int col=0; col<b; col++){
				int vec = row*b + col;
				double val = rectR.getEntry(row, col);
				R.setEntry(vec, val);
			}
		}
		return R;
	}

	public int getColumnDimension() {
		return a*b;
	}

	public int getRowDimension() {
		return a*b;
	}

}
