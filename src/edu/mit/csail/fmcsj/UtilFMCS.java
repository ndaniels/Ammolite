 package edu.mit.csail.fmcsj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.compression.FragStruct;
import edu.mit.csail.ammolite.compression.MoleculeStruct;



public class UtilFMCS {
	
	/**
	 * prints out tanimoto and overlap coefficients between two sdf files.
	 * 
	 * Development only.
	 * 
	 * @param fileA
	 * @param fileB
	 */
	public static void getCoeffs(String fileA, String fileB){
		IteratingSDFReader molsA = null;
		try{
			
		FileInputStream fsA = new FileInputStream(fileA);
		BufferedReader brA = new BufferedReader( new InputStreamReader(fsA ));
		molsA =new IteratingSDFReader( brA, DefaultChemObjectBuilder.getInstance());
		} catch( IOException e){
			//edu.mit.csail.ammolite.Logger.error("Failed to read first file");
			e.printStackTrace();
		}
		//edu.mit.csail.ammolite.Logger.error("Opened " + fileA);
		IteratingSDFReader molsB = null;
		try{
			
		FileInputStream fsB = new FileInputStream(fileB);
		BufferedReader brB = new BufferedReader( new InputStreamReader(fsB ));
		molsB =new IteratingSDFReader( brB, DefaultChemObjectBuilder.getInstance());
		} catch( IOException e){
			//edu.mit.csail.ammolite.Logger.error("Failed to second read file");
			e.printStackTrace();
		}
		//edu.mit.csail.ammolite.Logger.error("Opened "+fileB);
		
		IAtomContainer a;
		IAtomContainer b;
		MoleculeStruct repA;
		MoleculeStruct repB;
		
		
		//edu.mit.csail.ammolite.Logger.log("molA_ID molB_ID molA_size molB_size mcs_size overlap_coeff tanimoto_coeff", 0);
		//edu.mit.csail.ammolite.Logger.log("repA_ID repB_ID repA_size repB_size mcs_size overlap_coeff tanimoto_coeff", 0);
		while( molsA.hasNext() ){
			a = molsA.next();
			while( molsB.hasNext()){
				b = molsB.next();
				//edu.mit.csail.ammolite.Logger.debug("Comparing "+a.getID()+" to "+b.getID());
				a = new AtomContainer(AtomContainerManipulator.removeHydrogens(a));
				b = new AtomContainer(AtomContainerManipulator.removeHydrogens(b));
				repA = new FragStruct(a);
				repB = new FragStruct(b);
				//edu.mit.csail.ammolite.Logger.debug("Removed hydrogens");
				FMCS myMCS = new FMCS(a,b);
				FMCS repMCS = new FMCS(repA, repB);
				boolean timeOut = false;
				try {
					myMCS.calculate();
					repMCS.calculate();
				} catch (TimeoutException e) {
					timeOut = true;
				}
				if(!timeOut){
					//edu.mit.csail.ammolite.Logger.debug("Calculated MCS");
					double overlap = overlapCoeff( myMCS.size(), a.getAtomCount(), b.getAtomCount());
					double tanimoto = tanimotoCoeff( myMCS.size(), a.getAtomCount(), b.getAtomCount());
					double rep_overlap = overlapCoeff( repMCS.size(), repA.getAtomCount(), repB.getAtomCount());
					double rep_tanimoto = tanimotoCoeff( repMCS.size(), repA.getAtomCount(), repB.getAtomCount());
					//edu.mit.csail.ammolite.Logger.debug("Calculated coeffs");
					//edu.mit.csail.ammolite.Logger.log("mol "+a.getID() +" "+ b.getID() +" "+a.getAtomCount()+" "+b.getAtomCount()+" "+myMCS.size()+" "+overlap+" "+tanimoto, 0);
					//edu.mit.csail.ammolite.Logger.log("rep "+repA.getID() +" "+ repB.getID() +" "+repA.getAtomCount()+" "+repB.getAtomCount()+" "+repMCS.size()+" "+rep_overlap+" "+rep_tanimoto, 0);
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
		try {
			myMCS.calculate();
		} catch (TimeoutException te) {
			te.printStackTrace();
			System.exit(1);
		}
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
}
