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

import edu.mit.csail.ammolite.mcs.FMCS;



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
	
	/**
	 * Find the Max Common Subgraph between two molecules. Makes an sdf file and prints a picture.
	 * 
	 * @param input
	 * @param output
	 * @throws IOException
	 * @throws CDKException
	 */
	public static void doFMCS(String input, String output) throws IOException, CDKException{
		List<IAtomContainer> mols = SDFUtils.parseSDF(input);
		
		IAtomContainer a = new AtomContainer(AtomContainerManipulator.removeHydrogens(mols.get(0)));
		IAtomContainer b = new AtomContainer(AtomContainerManipulator.removeHydrogens(mols.get(1)));
		edu.mit.csail.ammolite.MolDrawer.draw(a, output + "_inp1");
		edu.mit.csail.ammolite.MolDrawer.draw(b, output + "_inp2");
		FMCS myMCS = new FMCS(a,b);
		myMCS.calculate();
		SDFWriter sdfwriter = new SDFWriter(new BufferedWriter( new FileWriter( output + ".sdf" )));
		for(IAtomContainer overlap: myMCS.getSolutions()){
			sdfwriter.write(overlap);
		}
		sdfwriter.close();

		int i=0;
		for(IAtomContainer sol: myMCS.getSolutions()){
			edu.mit.csail.ammolite.MolDrawer.draw(sol, output + i);
			++i;
		}
	}
	

}
