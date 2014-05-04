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

import edu.mit.csail.ammolite.compression.MoleculeStruct;
import edu.mit.csail.ammolite.mcs.FMCS;



public class FMCSTest{
	protected IAtomContainer caffeine;
	protected IAtomContainer viagra;
	protected List<IAtomContainer> randomMolecules;
	
	protected List<IAtomContainer> getMolecules(String filename){
		IteratingSDFReader sdfReader= null;
		try{
			
		FileInputStream fsA = new FileInputStream(filename);
		BufferedReader brA = new BufferedReader( new InputStreamReader(fsA ));
		sdfReader =new IteratingSDFReader( brA, DefaultChemObjectBuilder.getInstance());
		} catch( IOException e){
			//edu.mit.csail.ammolite.Logger.error("Failed to read first file");
			e.printStackTrace();
		}
		List<IAtomContainer> molecules = new ArrayList<IAtomContainer>();
		
		while(sdfReader.hasNext()){
			molecules.add(sdfReader.next());
		}
		return molecules;
	}
	
	@Before
	public void setUp() throws Exception {
		List<IAtomContainer> caff_viagra = getMolecules("test-data/caff_viagra.sdf");
		caffeine = caff_viagra.get(0);
		viagra = caff_viagra.get(1);
		randomMolecules = getMolecules("test-data/30_random_molecules.sdf");
		
		
	}
	
	@Test
	public void testCalculateSpeedOnStructs() {
		int molOffset = randomMolecules.size()/2;
		long totalTime = 0;
		for(int i=0; i<molOffset; ++i){
			MoleculeStruct a = new MoleculeStruct(randomMolecules.get(i));
			MoleculeStruct b = new MoleculeStruct(randomMolecules.get(i+molOffset));
			FMCS myFMCS = new FMCS(a,b);
			totalTime += myFMCS.calculate();
		}
		long aveTime = totalTime / molOffset;
		System.out.println("Matched "+molOffset+" structure pairs in "+totalTime+" ms. Average Time: "+aveTime);
	}

	@Test
	public void testCalculateSpeed() {
		int molOffset = randomMolecules.size()/2;
		long totalTime = 0;
		for(int i=0; i<molOffset; ++i){
			FMCS myFMCS = new FMCS(randomMolecules.get(i), randomMolecules.get(i+molOffset));
			totalTime += myFMCS.calculate();
		}
		long aveTime = totalTime / molOffset;
		System.out.println("Matched "+molOffset+" pairs in "+totalTime+" ms. Average Time: "+aveTime);
	}

	@Test
	public void testMySize() {
		FMCS myFMCS = new FMCS(caffeine, viagra);
		myFMCS.calculate();
		assertEquals("Did not find MCS of correct size. Expected 8 overlapping atoms, found "+myFMCS.size(), 8, myFMCS.size());

	}

}
