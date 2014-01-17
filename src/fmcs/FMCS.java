package fmcs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;


import speedysearch.IteratingSDFReader;

public class FMCS {
	
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
			speedysearch.Logger.error("Failed to read first file");
			e.printStackTrace();
		}
		speedysearch.Logger.error("Opened " + fileA);
		IteratingSDFReader molsB = null;
		try{
			
		FileInputStream fsB = new FileInputStream(fileB);
		BufferedReader brB = new BufferedReader( new InputStreamReader(fsB ));
		molsB =new IteratingSDFReader( brB, DefaultChemObjectBuilder.getInstance());
		} catch( IOException e){
			speedysearch.Logger.error("Failed to second read file");
			e.printStackTrace();
		}
		speedysearch.Logger.error("Opened "+fileB);
		
		IAtomContainer a;
		IAtomContainer b;
		
		while( molsA.hasNext() ){
			a = molsA.next();
			while( molsB.hasNext()){
				b = molsB.next();
				speedysearch.Logger.debug("Comparing "+a.getID()+" to "+b.getID());
				a = new AtomContainer(AtomContainerManipulator.removeHydrogens(a));
				b = new AtomContainer(AtomContainerManipulator.removeHydrogens(b));
				speedysearch.Logger.debug("Removed hydrogens");
				MCS myMCS = new MCS(a,b);
				myMCS.calculate();
				speedysearch.Logger.debug("Calculated MCS");
				double overlap = overlapCoeff( myMCS.size(), a.getAtomCount(), b.getAtomCount());
				double tanimoto = tanimotoCoeff( myMCS.size(), a.getAtomCount(), b.getAtomCount());
				speedysearch.Logger.debug("Calculated coeffs");
				speedysearch.Logger.log("molA "+a.getID() +" molB "+ b.getID() +" mcs_size "+myMCS.size()+" overlap: "+overlap+" tanimoto: "+tanimoto, 0);
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
			speedysearch.Logger.error("Failed to read file");
			e.printStackTrace();
		}
		speedysearch.Logger.log("reading molecules",3);
		
		IAtomContainer a = null;
		IAtomContainer b = null;
		
		a = molecules.next();
		speedysearch.Logger.log("molecule one: "+a.getID(), 2);
		
		
		b = molecules.next();
		speedysearch.Logger.log("molecule two: "+b.getID(), 2);
		
		molecules.close();
		a = new AtomContainer(AtomContainerManipulator.removeHydrogens(a));
		b = new AtomContainer(AtomContainerManipulator.removeHydrogens(b));
		MCS myMCS = new MCS(a,b);
		myMCS.calculate();
		SDFWriter sdfwriter = new SDFWriter(new BufferedWriter( new FileWriter( output + ".sdf" )));
		for(IAtomContainer overlap: myMCS.getSolutions()){
			sdfwriter.write(overlap);
		}
		sdfwriter.close();
		speedysearch.Logger.log("found mcs!", 2);
		speedysearch.MolDrawer.draw(myMCS.getSolutions().get(0), output);
	}
}
