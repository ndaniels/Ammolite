package edu.mit.csail.ammolite.mcs;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import edu.mit.csail.ammolite.IteratingSDFReader;

import edu.mit.csail.ammolite.compression.CyclicStruct;
import edu.mit.csail.ammolite.compression.MoleculeStruct;
import edu.mit.csail.ammolite.mcs.AbstractMCS;
import edu.mit.csail.ammolite.mcs.MCSFinder;
import edu.mit.csail.ammolite.mcs.SMSD;
import edu.mit.csail.ammolite.utils.ParallelUtils;




public class SMSDTest{
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
	public void testCalculateSpeed() {
		int numComp = 0;
		int n = randomMolecules.size();
		long totalTime = 0;
		for(int j=1; j<n/2-1; ++j){
			for(int i=0; i<n; ++i){
				IAtomContainer a = randomMolecules.get(i);
				IAtomContainer b = randomMolecules.get( (i+j) % n);
				SMSD myFMCS = new SMSD(a,b);
				totalTime += myFMCS.calculate();
				numComp += 1;
			}
		}
		long aveTime = totalTime / numComp;
		System.out.println("Matched "+numComp+" cyclic structure pairs in "+totalTime+" ms. Average Time: "+aveTime);
	}
	
	@Test
	public void testCalculateSpeedOnStructs() {
		int numComp = 0;
		int n = randomMolecules.size();
		long totalTime = 0;
		for(int j=1; j<n/2-1; ++j){
			for(int i=0; i<n; ++i){
				MoleculeStruct a = new MoleculeStruct(randomMolecules.get(i));
				MoleculeStruct b = new MoleculeStruct(randomMolecules.get( (i+j) % n));
				SMSD myFMCS = new SMSD(a,b);
				totalTime += myFMCS.calculate();
				numComp += 1;
			}
		}
		long aveTime = totalTime / numComp;
		System.out.println("Matched "+numComp+" structure pairs in "+totalTime+" ms. Average Time: "+aveTime);
	}
	
	
	
	@Test
	public void testCalculateSpeedOnCyclicStructs() {
		int numComp = 0;
		int n = randomMolecules.size();
		long totalTime = 0;
		for(int j=1; j<n/2-1; ++j){
			for(int i=0; i<n; ++i){
				MoleculeStruct a = new CyclicStruct(randomMolecules.get(i));
				MoleculeStruct b = new CyclicStruct(randomMolecules.get( (i+j) % n));
				SMSD myFMCS = new SMSD(a,b);
				totalTime += myFMCS.calculate();
				numComp += 1;
			}
		}
		long aveTime = totalTime / numComp;
		System.out.println("Matched "+numComp+" cyclic structure pairs in "+totalTime+" ms. Average Time: "+aveTime);
	}
	
	@Test
	public void testCalculateParallelSpeedOnCyclicStructs() {
		int numComp = 0;
		int n = randomMolecules.size();
		long totalTime = 0;
		
		List<Callable<Long>> callList = new ArrayList<Callable<Long>>();
		
		for(int j=1; j<n/2-1; ++j){
			for(int i=0; i<n; ++i){
				final MoleculeStruct a = new CyclicStruct(randomMolecules.get(i));
				final MoleculeStruct b = new CyclicStruct(randomMolecules.get( (i+j) % n));
				numComp += 1;
				Callable<Long> callable = new Callable<Long>(){
					public Long call() throws InterruptedException, ExecutionException{
						SMSD mySMSD = new SMSD(a,b);
						return mySMSD.calculate();
					}
				};
				callList.add(callable);
			}
		}
		ParallelUtils.parallelFullExecution(callList);
		long aveTime = totalTime / numComp;
		System.out.println("Matched "+numComp+" cyclic structure pairs in "+totalTime+" ms. Average Time: "+aveTime);
	}

	@Test
	public void testMySize() {
		SMSD mySMSD = new SMSD(caffeine, viagra);
		mySMSD.calculate();
//		IAtomContainer solution = mySMSD.getFirstSolution();
		assertTrue("Did not find MCS of correct size. Expected at least 8 overlapping atoms, found "+mySMSD.size(), 8 <= mySMSD.size());

	}

}
