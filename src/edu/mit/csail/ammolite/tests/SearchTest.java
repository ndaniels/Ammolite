package edu.mit.csail.ammolite.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.IMolStruct;
import edu.mit.csail.ammolite.compression.MoleculeStructFactory;
import edu.mit.csail.ammolite.database.CompressionType;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.SDFUtils;
import edu.mit.csail.ammolite.utils.WallClock;

public class SearchTest {
	

	private static final String smsd = "SMSD";
	
	public static void testSearch(List<String> queryFiles, String databaseName, String outName, double fine, double coarse, 
                                    boolean testAmm,  
                                    boolean testSMSD){
	    SearchTest.testSearch(queryFiles, databaseName, outName, fine, coarse, testAmm, testSMSD, "no description");
	    
	}
	
	
	public static void testSearch(List<String> queryFiles, String databaseName, String outName, double fine, double coarse, 
									boolean testAmm,  
									boolean testSMSD, String description){
		
		
		IStructDatabase db = StructDatabaseDecompressor.decompress(databaseName);
		for(String queryFile: queryFiles){
    		List<IAtomContainer> queries = SDFUtils.parseSDF( queryFile);
    		
    		
    		Iterator<IMolStruct> sTargets = db.iterator();
    		
    		PrintStream stream = getPrintStream(outName);
    		System.out.println("fine_threshold: "+fine+" coarse_threshold: "+coarse);
    		System.out.println(db.info());
    		System.out.println("Description: "+description);
    		stream.println("fine_threshold: "+fine+" coarse_threshold: "+coarse);
    		stream.println(db.info());
    		stream.println("Description: "+description);
    
    		
    		if( testAmm){
    			Tester tester = new Ammolite_QuerywiseParallel_2();
    			runTest(tester, stream, queries, db, sTargets, fine, coarse);
    		}
    		if( testSMSD){
    			Tester tester = new SMSD_QuerywiseParallel();
    			runTest(tester, stream, queries, db, sTargets,  fine, coarse);
    		}
    
    		stream.close();
		}
		
	}
	
//	public static Iterator<IAtomContainer> getTargetIterator(IStructDatabase db, Iterator<IAtomContainer> oldIterator){
//		if( oldIterator == null){
//			return null;
//		}
//			List<String> sdfFiles = db.getSourceFiles().getFilenames();
//			return SDFUtils.parseSDFSetOnline(sdfFiles);
//
//	}
	
	private static void runTest(Tester tester, PrintStream stream, List<IAtomContainer> queries, IStructDatabase db, Iterator<IMolStruct> sTargets, double thresh, 
										double prob){
	    
		String name = tester.getName();
		WallClock clock = new WallClock( name);
		tester.test(queries, db, sTargets, thresh, prob, name, stream);
		System.out.println("");
		clock.printElapsed();
		stream.println(clock.getElapsedString());
	}
	
	private static PrintStream getPrintStream(String outName){
		PrintStream writer= null;
		try {
		    File f = new File(outName);
		    FileOutputStream fos = new FileOutputStream(f, true);
			writer = new PrintStream(fos);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if(writer == null){
			System.exit(1);
		}
		return writer;
		
	}
	
	
	public static void testSMSD(String queryFile){
		List<IAtomContainer> molecules = SDFUtils.parseSDF( queryFile);
		System.out.println(smsd);
		System.out.println("id1 id2 size(1) size(2) overlapSize(1,2) timeInMillis");
		System.out.println("BEGIN_DATA");
		for(int i=0; i<molecules.size(); i++){
			for(int j=0; j<=i; j++){
				IAtomContainer a = molecules.get(i);
				IAtomContainer b = molecules.get(j);
				long wallClockStart = System.currentTimeMillis();
				int mcsSize = MCS.getSMSDOverlap(a, b);
				long wallClockElapsed = System.currentTimeMillis() - wallClockStart;
				System.out.print(a.getProperty("PUBCHEM_COMPOUND_CID"));
				System.out.print(" ");
				System.out.print(b.getProperty("PUBCHEM_COMPOUND_CID"));
				System.out.print(" ");
				System.out.print(MCSUtils.getAtomCountNoHydrogen(a));
				System.out.print(" ");
				System.out.print(MCSUtils.getAtomCountNoHydrogen(b));
				System.out.print(" ");
				System.out.print(mcsSize);
				System.out.print(" ");
				System.out.println( wallClockElapsed);
			}
		}
		System.out.println("END_DATA");
	}
	
	
	public static void testAmmoliteCoarse(String queryFile){
		List<IAtomContainer> molecules = SDFUtils.parseSDF( queryFile);
		MoleculeStructFactory sf = new MoleculeStructFactory(CompressionType.CYCLIC);
		System.out.println("id1 id2 size(1) size(2) compressedSize(1) compressedSize(2) overlapSize(1,2) timeInMillis");
		System.out.println("BEGIN_DATA");
		for(int i=0; i<molecules.size(); i++){
			for(int j=0; j<=i; j++){
				IAtomContainer a = molecules.get(i);
				IAtomContainer b = molecules.get(j);
				IMolStruct sA = sf.makeMoleculeStruct(a);
				IMolStruct sB = sf.makeMoleculeStruct(b);
				long wallClockStart = System.currentTimeMillis();
				int mcsSize = MCS.getSMSDOverlap(sA, sB);
				long wallClockElapsed = System.currentTimeMillis() - wallClockStart;
				System.out.print(a.getProperty("PUBCHEM_COMPOUND_CID"));
				System.out.print(" ");
				System.out.print(b.getProperty("PUBCHEM_COMPOUND_CID"));
				System.out.print(" ");
				System.out.print(MCSUtils.getAtomCountNoHydrogen(a));
				System.out.print(" ");
				System.out.print(MCSUtils.getAtomCountNoHydrogen(b));
				System.out.print(" ");
				System.out.print(sA.getAtomCount());
				System.out.print(" ");
				System.out.print(sB.getAtomCount());
				System.out.print(" ");
				System.out.print(mcsSize);
				System.out.print(" ");
				System.out.println( wallClockElapsed);
			}
		}
		System.out.println("END_DATA");
	}
	
}