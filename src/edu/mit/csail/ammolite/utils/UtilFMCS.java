 package edu.mit.csail.ammolite.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import scala.reflect.internal.Trees.This;
import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.compression.CyclicStruct;
import edu.mit.csail.ammolite.compression.FragStruct;
import edu.mit.csail.ammolite.compression.MoleculeStruct;
import edu.mit.csail.ammolite.mcs.*;



public class UtilFMCS {
	
	/**
	 * prints out tanimoto and overlap coefficients between two sdf files.
	 * 
	 * Development only.
	 * 
	 * @param fileA
	 * @param fileB
	 */
	public static void getCoeffs(String filename){

		List<IAtomContainer> mols = parseSDF( filename);
		
		IAtomContainer a;
		IAtomContainer b;
		MoleculeStruct repA;
		MoleculeStruct repB;
		
		
		Logger.log("molA_ID molB_ID molA_size molB_size mcs_size overlap_coeff tanimoto_coeff", 0);
		Logger.log("repA_ID repB_ID repA_size repB_size mcs_size overlap_coeff tanimoto_coeff", 0);
		for(int i=0; i< mols.size(); ++i){
			a = mols.get(i);
			for(int j=0; j<i; ++j){
				b = mols.get(j);
				//edu.mit.csail.ammolite.Logger.debug("Comparing "+a.getID()+" to "+b.getID());
				a = new AtomContainer(AtomContainerManipulator.removeHydrogens(a));
				b = new AtomContainer(AtomContainerManipulator.removeHydrogens(b));
				repA = new CyclicStruct(a);
				repB = new CyclicStruct(b);
				//edu.mit.csail.ammolite.Logger.debug("Removed hydrogens");
				FMCS myMCS = new FMCS(a,b);
				FMCS repMCS = new FMCS(repA, repB);
				boolean timeOut = false;
				myMCS.calculate();
				repMCS.calculate();
				if(!timeOut){
					//edu.mit.csail.ammolite.Logger.debug("Calculated MCS");
					double overlap = overlapCoeff( myMCS.size(), a.getAtomCount(), b.getAtomCount());
					double tanimoto = tanimotoCoeff( myMCS.size(), a.getAtomCount(), b.getAtomCount());
					double rep_overlap = overlapCoeff( repMCS.size(), repA.getAtomCount(), repB.getAtomCount());
					double rep_tanimoto = tanimotoCoeff( repMCS.size(), repA.getAtomCount(), repB.getAtomCount());
					//edu.mit.csail.ammolite.Logger.debug("Calculated coeffs");
					Logger.log("mol "+a.getID() +" "+ b.getID() +" "+a.getAtomCount()+" "+b.getAtomCount()+" "+myMCS.size()+" "+overlap+" "+tanimoto, 0);
					Logger.log("rep "+repA.getID() +" "+ repB.getID() +" "+repA.getAtomCount()+" "+repB.getAtomCount()+" "+repMCS.size()+" "+rep_overlap+" "+rep_tanimoto, 0);
				}
			}
		}
		
	}
	
	private static double overlapCoeff(int overlap, int a, int b){
		
		if( a < b){
			return ( (double) overlap) / a;
		}
		return ( (double) overlap) / b;
	}
	 
	private static double tanimotoCoeff(int overlap, int a, int b){
		return ( (double) overlap) / ( a + b - overlap);
	}
	
	/**
	 * Find the Max Common Subgraph between two molecules. Makes an sdf file and prints a picture.
	 * 
	 * @param input
	 * @param output
	 * @throws IOException
	 * @throws CDKException
	 */
	public static void doFMCS(String input, String output) throws IOException, CDKException{
		IteratingSDFReader molecules = null;
		try{
			
		FileInputStream fs = new FileInputStream(input);
		BufferedReader br = new BufferedReader( new InputStreamReader(fs ));
		molecules =new IteratingSDFReader( br, DefaultChemObjectBuilder.getInstance());
		} catch( IOException e){
			//edu.mit.csail.ammolite.Logger.error("Failed to read file");
			e.printStackTrace();
		}
		//edu.mit.csail.ammolite.Logger.log("reading molecules",3);
		
		IAtomContainer a = null;
		IAtomContainer b = null;
		
		a = molecules.next();
		//edu.mit.csail.ammolite.Logger.log("molecule one: "+a.getID(), 2);
		
		
		b = molecules.next();
		//edu.mit.csail.ammolite.Logger.log("molecule two: "+b.getID(), 2);
		
		molecules.close();
		a = new AtomContainer(AtomContainerManipulator.removeHydrogens(a));
		b = new AtomContainer(AtomContainerManipulator.removeHydrogens(b));
		edu.mit.csail.ammolite.MolDrawer.draw(a, output + "_inp1");
		edu.mit.csail.ammolite.MolDrawer.draw(b, output + "_inp2");
		FMCS myMCS = new FMCS(a,b);
		myMCS.calculate();
		SDFWriter sdfwriter = new SDFWriter(new BufferedWriter( new FileWriter( output + ".sdf" )));
		for(IAtomContainer overlap: myMCS.getSolutions()){
			sdfwriter.write(overlap);
		}
		sdfwriter.close();
		//edu.mit.csail.ammolite.Logger.log("found mcs!", 2);
		int i=0;
		for(IAtomContainer sol: myMCS.getSolutions()){
			edu.mit.csail.ammolite.MolDrawer.draw(sol, output + i);
			++i;
		}
	}
	
	private static List<IAtomContainer> parseSDF(String filename){
		IteratingSDFReader molecules = null;
		try{
			
		FileInputStream fs = new FileInputStream(filename);
		BufferedReader br = new BufferedReader( new InputStreamReader(fs ));
		molecules =new IteratingSDFReader( br, DefaultChemObjectBuilder.getInstance());
		} catch( IOException e){
			//edu.mit.csail.ammolite.Logger.error("Failed to read file");
			e.printStackTrace();
		}
		
		List<IAtomContainer> mols = new ArrayList<IAtomContainer>();
		while(molecules.hasNext()){
			mols.add( molecules.next());
		}
		return mols;
	}
	
	private static List<Integer> testIsoRank(List<IAtomContainer> mols, List<MoleculeStruct> structs, double baseThresh){
		// IsoRank Structures
		List<Integer> isoStructSizes = new ArrayList<Integer>(mols.size()*mols.size() / 2);
		List<Double> isoOverlaps = new ArrayList<Double>(mols.size()*mols.size() / 2);
		List<Double> isoTanimotos = new ArrayList<Double>(mols.size()*mols.size() / 2);
		int numberOfIsoMCS = 0;
		long isoStartTime = System.currentTimeMillis();
		for(int i=0; i<mols.size(); ++i){
			for(int j=0; j<i; ++j){
				IsoRank myMCS = new IsoRank(structs.get(i), structs.get(j));
				myMCS.calculate();
				isoStructSizes.add(myMCS.size());
				isoOverlaps.add(UtilFunctions.overlapCoeff(myMCS.size(), structs.get(i).getAtomCount(), structs.get(j).getAtomCount()));
				isoTanimotos.add(UtilFunctions.tanimotoCoeff(myMCS.size(), structs.get(i).getAtomCount(), structs.get(j).getAtomCount()));
				numberOfIsoMCS++;
			}
		}
		long elapsedIsoTime = System.currentTimeMillis() - isoStartTime;
		long aveIsoTime = elapsedIsoTime / numberOfIsoMCS;
		System.out.println("Did "+numberOfIsoMCS+" comparisons of structures in "+elapsedIsoTime+ "ms with IsoRank. Average time: "+aveIsoTime);
		return isoStructSizes;
	}
	
	private static List<Integer> testFMCSMols(List<IAtomContainer> mols, List<MoleculeStruct> structs){
		// FMCSj Molecules
		List<Integer> fmcsSizes = new ArrayList<Integer>(mols.size()*mols.size() / 2);
		int numberOfMolFMCSMCS = 0;
		long molMCSFMCSStartTime = System.currentTimeMillis();
		for(int i=0; i<mols.size(); ++i){
			for(int j=0; j<i; ++j){
				AbstractMCS myMCS = new FMCS(mols.get(i), mols.get(j));
				myMCS.timedCalculate(2000);
				fmcsSizes.add(myMCS.size());
				numberOfMolFMCSMCS++;
			}
		}
		long elapsedFMCSMolTime = System.currentTimeMillis() - molMCSFMCSStartTime;
		long aveFMCSMolTime = elapsedFMCSMolTime / numberOfMolFMCSMCS;
		System.out.println("Did "+numberOfMolFMCSMCS+" comparisons of molecules in "+elapsedFMCSMolTime+ "ms with FMCS. Average time: "+aveFMCSMolTime);
		return fmcsSizes;
	}
	
	private static List<Integer> testFMCSStructs(List<IAtomContainer> mols, List<MoleculeStruct> structs){
		// FMCSj Structures
		List<Integer> fmcsStructSizes = new ArrayList<Integer>(mols.size()*mols.size() / 2);
		int numberOfStructFMCS = 0;
		long structFMCSStartTime = System.currentTimeMillis();
		for(int i=0; i<mols.size(); ++i){
			for(int j=0; j<i; ++j){
				AbstractMCS myMCS = new FMCS(mols.get(i), mols.get(j));
				myMCS.timedCalculate(2000);
				fmcsStructSizes.add(myMCS.size());
				numberOfStructFMCS++;
			}
		}
		long elapsedStructFMCSMolTime = System.currentTimeMillis() - structFMCSStartTime;
		long aveStructFMCSMolTime = elapsedStructFMCSMolTime / numberOfStructFMCS;
		System.out.println("Did "+numberOfStructFMCS+" comparisons of structures in "+elapsedStructFMCSMolTime+ "ms with FMCS. Average time: "+aveStructFMCSMolTime);
		return fmcsStructSizes;
	}
	
	private static List<Integer> testSMSDMols(List<IAtomContainer> mols, List<MoleculeStruct> structs){
		// SMSD Molecules
		List<Integer> smsdSizes = new ArrayList<Integer>(mols.size()*mols.size() / 2);
		int numberOfMolMCS = 0;
		long molMCSStartTime = System.currentTimeMillis();
		for(int i=0; i<mols.size(); ++i){
			for(int j=0; j<i; ++j){
				AbstractMCS myMCS = new SMSD(mols.get(i), mols.get(j));
				myMCS.timedCalculate(2000);
				smsdSizes.add(myMCS.size());
				numberOfMolMCS++;
			}
		}
		long elapsedMolTime = System.currentTimeMillis() - molMCSStartTime;
		long aveMolTime = elapsedMolTime / numberOfMolMCS;
		System.out.println("Did "+numberOfMolMCS+" comparisons of molecules in "+elapsedMolTime+ "ms with SMSD. Average time: "+aveMolTime);
		return smsdSizes;
	}

	private static List<Integer> testSMSDStructs(List<IAtomContainer> mols, List<MoleculeStruct> structs){
		
		// SMSD Structures
		List<Integer> smsdStructSizes = new ArrayList<Integer>(mols.size()*mols.size() / 2);
		List<Double> smsdOverlaps = new ArrayList<Double>(mols.size()*mols.size() / 2);
		List<Double> smsdTanimotos = new ArrayList<Double>(mols.size()*mols.size() / 2);
		int numberOfStructMCS = 0;
		long molStructStartTime = System.currentTimeMillis();
		for(int i=0; i<mols.size(); ++i){
			for(int j=0; j<i; ++j){
				AbstractMCS myMCS = new SMSD(structs.get(i), structs.get(j));
				myMCS.timedCalculate(2000);
				smsdStructSizes.add(myMCS.size());
				smsdOverlaps.add(UtilFunctions.overlapCoeff(myMCS.size(), structs.get(i).getAtomCount(), structs.get(j).getAtomCount()));
				smsdTanimotos.add(UtilFunctions.tanimotoCoeff(myMCS.size(), structs.get(i).getAtomCount(), structs.get(j).getAtomCount()));
				numberOfStructMCS++;
			}
		}
		long elapsedStructTime = System.currentTimeMillis() - molStructStartTime;
		long aveStructTime = elapsedStructTime / numberOfStructMCS;
		System.out.println("Did "+numberOfStructMCS+" comparisons of structures in "+elapsedStructTime+ "ms with SMSD. Average time: "+aveStructTime);
		
		
		return smsdStructSizes;
	}
	
	private static List<Integer> selfCompareSMSDStructs(List<IAtomContainer> mols, List<MoleculeStruct> structs){
		
		// SMSD Structures
		List<Integer> smsdStructSizes = new ArrayList<Integer>(mols.size()*mols.size() / 2);
		int numberOfStructMCS = 0;
		long molStructStartTime = System.currentTimeMillis();
		for(int i=0; i<mols.size(); ++i){
			AbstractMCS myMCS = new SMSD(structs.get(i), structs.get(i));
			myMCS.timedCalculate(2000);
			smsdStructSizes.add(myMCS.size());
			numberOfStructMCS++;
		}
		long elapsedStructTime = System.currentTimeMillis() - molStructStartTime;
		long aveStructTime = elapsedStructTime / numberOfStructMCS;
		System.out.println("Did "+numberOfStructMCS+" comparisons of structures to themselves in "+elapsedStructTime+ "ms with SMSD. Average time: "+aveStructTime);

		return smsdStructSizes;
	}
	
	private static List<Integer> selfCompareSMSD(List<IAtomContainer> mols, List<MoleculeStruct> structs){
		
		// SMSD Structures
		List<Integer> smsdSizes = new ArrayList<Integer>(mols.size()*mols.size() / 2);
		int numberOfStructMCS = 0;
		long molStructStartTime = System.currentTimeMillis();
		for(int i=0; i<mols.size(); ++i){
			AbstractMCS myMCS = new SMSD(mols.get(i), mols.get(i));
			myMCS.timedCalculate(2000);
			smsdSizes.add(myMCS.size());
			numberOfStructMCS++;
		}
		long elapsedStructTime = System.currentTimeMillis() - molStructStartTime;
		long aveStructTime = elapsedStructTime / numberOfStructMCS;
		System.out.println("Did "+numberOfStructMCS+" comparisons of molecules to themselves in "+elapsedStructTime+ "ms with SMSD. Average time: "+aveStructTime);

		return smsdSizes;
	}
	
	
	private static List<Integer> selfCompareFMCSStructs(List<IAtomContainer> mols, List<MoleculeStruct> structs){
		
		// SMSD Structures
		List<Integer> smsdStructSizes = new ArrayList<Integer>(mols.size()*mols.size() / 2);
		int numberOfStructMCS = 0;
		long molStructStartTime = System.currentTimeMillis();
		for(int i=0; i<mols.size(); ++i){
			AbstractMCS myMCS = new FMCS(structs.get(i), structs.get(i));
			myMCS.timedCalculate(2000);
			smsdStructSizes.add(myMCS.size());
			numberOfStructMCS++;
		}
		long elapsedStructTime = System.currentTimeMillis() - molStructStartTime;
		long aveStructTime = elapsedStructTime / numberOfStructMCS;
		System.out.println("Did "+numberOfStructMCS+" comparisons of structures to themselves in "+elapsedStructTime+ "ms with FMCS. Average time: "+aveStructTime);

		return smsdStructSizes;
	}
	
	private static List<Integer> selfCompareFMCS(List<IAtomContainer> mols, List<MoleculeStruct> structs){
		
		// SMSD Structures
		List<Integer> smsdSizes = new ArrayList<Integer>(mols.size()*mols.size() / 2);
		int numberOfStructMCS = 0;
		long molStructStartTime = System.currentTimeMillis();
		for(int i=0; i<mols.size(); ++i){
			AbstractMCS myMCS = new FMCS(mols.get(i), mols.get(i));
			myMCS.timedCalculate(2000);
			smsdSizes.add(myMCS.size());
			numberOfStructMCS++;
		}
		long elapsedStructTime = System.currentTimeMillis() - molStructStartTime;
		long aveStructTime = elapsedStructTime / numberOfStructMCS;
		System.out.println("Did "+numberOfStructMCS+" comparisons of molecules to themselves in "+elapsedStructTime+ "ms with FMCS. Average time: "+aveStructTime);

		return smsdSizes;
	}
	
	private static void compare(String name1, String name2, List l1, List l2){
		System.out.println("COMPARISON_DELIMITER");
		System.out.println(name1+" "+name2);
		
		for( int i=0; i< l1.size(); ++i){
			System.out.print(l1.get(i));
			System.out.print(" ");
			System.out.println(l2.get(i));
		}
	}
	
	private static List<Double> buildIsoOverlap(List<MoleculeStruct> structs, List<Integer> isos){
		List<Double> isoOverlaps = new ArrayList<Double>();
		int k=0;
		for(int i=0; i<structs.size(); i++){
			for(int j=0; j<i; j++){
				int iso = isos.get(k);
				double overlap = UtilFunctions.overlapCoeff(iso, structs.get(i).getAtomCount(), structs.get(j).getAtomCount());
				k++;
				isoOverlaps.add(overlap);
			}
		}
		return isoOverlaps;
	}
	
	public static void prettyPrintMatrix(RealMatrix m){
		int rows=m.getRowDimension();
		int cols = m.getColumnDimension();
		
		for(int r=0; r<rows; r++){
			System.out.print(r+":\t{");
			for(int c=0; c<cols; c++){
				System.out.print(m.getEntry(r, c));
				if(c!=cols-1){
					System.out.print(", ");
				} else {
					System.out.println("}");
				}
			}
		}
	}
	public static void prettyPrintMatrix(DualMatrix m){
		int rows=m.getRowDimension();
		int cols = m.getColumnDimension();
		
		for(int r=0; r<rows; r++){
			System.out.print(r+":\t{");
			for(int c=0; c<cols; c++){
				System.out.print(m.getEntry(r, c));
				if(c!=cols-1){
					System.out.print(", ");
				} else {
					System.out.println("}");
				}
			}
		}
	}
	
	private static SparseMatrix getSparseMatrix(int d){
		SparseMatrix A = new SparseMatrix(d, d);
		for(int rowA=0; rowA<d; rowA++){
			for(int colA=0; colA<rowA; colA++){
				if(Math.random() <= 2.0 / (d*d)){
					A.add(rowA, colA, 1.0);
					A.add(colA, rowA, 1.0);
				}
			}
		}
		return A;
	}
	
	private static void speedTestMatrices(){
		SparseMatrix A;
		SparseMatrix B;
		RealMatrix rA;
		RealMatrix rB;
		DualMatrix D;
		RealMatrix rD;
		RealVector V;
		for(int rep=0; rep<5; rep++){
		System.out.println("Small Matrices");
		int sDim = 10;
		A = getSparseMatrix(sDim);
		B = getSparseMatrix(sDim);
		rA = A.getMatrix();
		rB = B.getMatrix();
		
		V = new OpenMapRealVector(sDim);
		V.mapAddToSelf(1.0);
		long start = System.currentTimeMillis();
		for(int iter=0; iter<100; iter++){
			V = A.postOperate(V);
		}
		System.out.println("SparseMatrix took "+(System.currentTimeMillis() - start));
		
		V = new OpenMapRealVector(sDim);
		V.mapAddToSelf(1.0);
		start = System.currentTimeMillis();
		for(int iter=0; iter<100; iter++){
			V = rA.operate(V);
		}
		System.out.println("RealMatrix took "+(System.currentTimeMillis() - start));
		
		System.out.println("Medium Matrices");
		int mDim = sDim*sDim;
		D = new DualMatrix(A,B);
		A = getSparseMatrix(mDim);
		B = getSparseMatrix(mDim);
		rA = A.getMatrix();
		rB = B.getMatrix();
		
		V = new OpenMapRealVector(mDim);
		V.mapAddToSelf(1.0);
		start = System.currentTimeMillis();
		for(int iter=0; iter<100; iter++){
			V = A.postOperate(V);
		}
		System.out.println("SparseMatrix took "+(System.currentTimeMillis() - start));
		
		V = new OpenMapRealVector(mDim);
		V.mapAddToSelf(1.0);
		start = System.currentTimeMillis();
		for(int iter=0; iter<100; iter++){
			V = rA.operate(V);
		}
		System.out.println("RealMatrix took "+(System.currentTimeMillis() - start));
		
		V = new OpenMapRealVector(mDim);
		V.mapAddToSelf(1.0);
		D = new DualMatrix(A,B);
		start = System.currentTimeMillis();
		for(int iter=0; iter<100; iter++){
			V = rA.operate(V);
		}
		System.out.println("DualMatrix took "+(System.currentTimeMillis() - start));
		
		System.out.println("Large Matrices");
		int lDim = mDim*mDim;
		D = new DualMatrix(A,B);
		A = getSparseMatrix(lDim);
		B = getSparseMatrix(lDim);
		rA = A.getMatrix();
		rB = B.getMatrix();
		
		V = new OpenMapRealVector(lDim);
		V.mapAddToSelf(1.0);
		start = System.currentTimeMillis();
		for(int iter=0; iter<100; iter++){
			V = A.postOperate(V);
		}
		System.out.println("SparseMatrix took "+(System.currentTimeMillis() - start));
		
		V = new OpenMapRealVector(lDim);
		V.mapAddToSelf(1.0);
		start = System.currentTimeMillis();
		for(int iter=0; iter<100; iter++){
			V = rA.operate(V);
		}
		System.out.println("RealMatrix took "+(System.currentTimeMillis() - start));
		
		V = new OpenMapRealVector(lDim);
		V.mapAddToSelf(1.0);
		D = new DualMatrix(A,B);
		start = System.currentTimeMillis();
		for(int iter=0; iter<100; iter++){
			V = rA.operate(V);
		}
		System.out.println("DualMatrix took "+(System.currentTimeMillis() - start));
		}
		
	}
	
	private static boolean testMatrices(){
		int aDim = 3;
		int bDim = 4;
		SparseMatrix A = new SparseMatrix(aDim, aDim);
		SparseMatrix B = new SparseMatrix(bDim, bDim);
		
		for(int rowA=0; rowA<aDim; rowA++){
			for(int colA=0; colA<rowA; colA++){
				if(Math.random() <= 0.5){
					A.add(rowA, colA, 1.0);
					A.add(colA, rowA, 1.0);
				}
			}
		}
		
		for(int rowB=0; rowB<bDim; rowB++){
			for(int colB=0; colB<rowB; colB++){
				if(Math.random() <= 0.5){
					B.add(rowB, colB, 1.0);
					B.add(colB, rowB, 1.0);
				}
			}
		}
		
		RealMatrix rA = A.getMatrix();
		RealMatrix rB = B.getMatrix();
		
//		System.out.println("A");
//		prettyPrintMatrix(rA);
//		System.out.println("B");
//		prettyPrintMatrix(rB);
		
		RealMatrix rAA = rA.multiply(rA);
		RealMatrix rBB = rB.multiply(rB);
		
		RealMatrix AA = A.postMultiply(rA);
		RealMatrix BB = B.postMultiply(rB);
		
		if(!(AA.equals(rAA) && BB.equals(rBB))){
			System.err.println("SparseMatrix Failed Tests.");
			return false;
		}
		
		int dDim = aDim*bDim;
		DualMatrix D = new DualMatrix(A,B);
		RealMatrix rD = new BlockRealMatrix(dDim, dDim);
		for(int row=0; row<dDim; row++){
			for(int col=0; col<dDim; col++){
				rD.setEntry(row, col, D.getEntry(row, col));
			}
		}


		
		RealVector V = new OpenMapRealVector(rD.getColumn(0));
		
		RealVector DV = D.postOperate(V);
		RealVector rDV = rD.operate(V);
		
//		System.out.println("ORIGINAL \tDUAL \tREAL");
//		
//		for(int i=0; i<dDim; i++){
//			System.out.print(V.getEntry(i));
//			System.out.print(" \t");
//			System.out.print(DV.getEntry(i));
//			System.out.print(" \t");
//			System.out.println(rDV.getEntry(i));
//		}
		
		double dist = DV.getDistance(rDV);
		if(dist > 0.000001){
			System.err.println("DualMatrix Failed Tests.");
			System.err.println("Vectors were off by "+ dist);
			return false;
		}
		
		return true;

	}
	
	public static void testMCS(String filename){

		
		if(!testMatrices()){
			System.exit(1);
		}
//		speedTestMatrices();

		List<IAtomContainer> mols = parseSDF(filename);
		List<MoleculeStruct> structs = new ArrayList<MoleculeStruct>(mols.size());
		List<Integer> molSizes = new ArrayList<Integer>(mols.size());
		List<Integer> structSizes = new ArrayList<Integer>(mols.size());
		for(IAtomContainer mol: mols){
			MoleculeStruct struct = new CyclicStruct(mol);
			IAtomContainer h = new AtomContainer(AtomContainerManipulator.removeHydrogens(mol));
			structs.add( struct);
			molSizes.add(h.getAtomCount());
			structSizes.add(struct.getAtomCount());
		}
		
		List<Integer> selfSMSD = selfCompareSMSD(mols,structs);
		List<Integer> selfSMSDStructs = selfCompareSMSDStructs(mols,structs);
		List<Integer> selfFMCS = selfCompareFMCS(mols,structs);
		List<Integer> selfFMCSStructs = selfCompareFMCSStructs(mols,structs);
		

		compare("self-smsd", "molecule-sizes", selfSMSD, molSizes);
		compare("self-smsd-structs", "structure-sizes", selfSMSDStructs, structSizes);
		compare("self-fmcs", "molecule-sizes", selfFMCS, molSizes);
		compare("self-fmcs-structs", "structure-sizes", selfFMCSStructs, structSizes);

		
	}
}
