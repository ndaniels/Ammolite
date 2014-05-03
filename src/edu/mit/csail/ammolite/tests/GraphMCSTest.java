package edu.mit.csail.ammolite.tests;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.IteratingSDFReader;

import edu.mit.csail.fmcsj.FMCS;
import edu.mit.csail.fmcsj.GraphMCS;
import edu.mit.csail.fmcsj.MolStruct;


public class GraphMCSTest{
	protected MolStruct caffeineStruct;
	protected MolStruct viagraStruct;
	protected List<MolStruct> randomMolStructs;
	
	protected List<MolStruct> getMolecules(String filename){
		IteratingSDFReader sdfReader= null;
		try{
			
		FileInputStream fsA = new FileInputStream(filename);
		BufferedReader brA = new BufferedReader( new InputStreamReader(fsA ));
		sdfReader = new IteratingSDFReader( brA, DefaultChemObjectBuilder.getInstance());
		} catch( IOException e){
			//edu.mit.csail.ammolite.Logger.error("Failed to read first file");
			e.printStackTrace();
		}
		List<MolStruct> molecules = new ArrayList<MolStruct>();
		
		while(sdfReader.hasNext()){
			molecules.add(new MolStruct( sdfReader.next()));
		}
		return molecules;
	}
	
	@Before
	public void setUp() throws Exception {
		List<MolStruct> caff_viagra = getMolecules("test-data/caff_viagra.sdf");
		caffeineStruct = caff_viagra.get(0);
		viagraStruct = caff_viagra.get(1);
		randomMolStructs = getMolecules("test-data/30_random_molecules.sdf");
		
		
	}
	
	@Test
	public void testCalculateSpeedOnStructs() {
		int molOffset = randomMolStructs.size()/2;
		long totalTime = 0;
		for(int i=0; i<molOffset; ++i){
			MolStruct a = randomMolStructs.get(i);
			MolStruct b = randomMolStructs.get(i+molOffset);
			FMCS myFMCS = new FMCS(a,b);
			totalTime += myFMCS.calculate();
		}
		long aveTime = totalTime / molOffset;
		System.out.println("Matched "+molOffset+" structure pairs in "+totalTime+" ms. Average Time: "+aveTime);
	}



	@Test
	public void testMySize() {
		GraphMCS myMCS = new GraphMCS(caffeineStruct, viagraStruct);
		myMCS.calculate();
		assertEquals("Did not find MCS of correct size. Expected 8 overlapping atoms, found "+myMCS.size(), 8 <= myMCS.size());

	}

}
