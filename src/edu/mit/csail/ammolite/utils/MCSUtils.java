 package edu.mit.csail.ammolite.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;




public class MCSUtils {
	
	public static double tanimotoCoeff(int overlap, int a, int b){
		return ( (double) overlap) / ( a + b - overlap);
	}
	
	public static double overlapCoeff(int overlap, int a, int b){
		if( a < b){
			return ( 1.0*overlap) / a;
		}
		return ( 1.0*overlap) / b;
	}
	
	public static double overlapCoeff(int overlap, IAtomContainer a, IAtomContainer b){
		return overlapCoeff(overlap, getAtomCountNoHydrogen(a), getAtomCountNoHydrogen(b));
	}
	
	public static int getAtomCountNoHydrogen(IAtomContainer mol){
		return AtomContainerManipulator.removeHydrogens(mol).getAtomCount();
	}
	

}
