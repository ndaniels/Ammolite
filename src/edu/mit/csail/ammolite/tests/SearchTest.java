package edu.mit.csail.ammolite.tests;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.compression.MoleculeStructFactory;
import edu.mit.csail.ammolite.database.BigStructDatabase;
import edu.mit.csail.ammolite.database.CompressionType;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.SDFUtils;
import edu.mit.csail.ammolite.utils.WallClock;

public class SearchTest {
	

	private static final String smsd = "SMSD";
	private static final String fmcs = "FMCS";
	
	public static void testSearch(String queryFile, String databaseName, String outName, double fine, double coarse, 
                                    boolean testAmm, boolean testAmmPar, boolean testAmmCompressedQuery, 
                                    boolean testSMSD, boolean testFMCS,boolean testAmmSMSD, boolean useCaching){
	    SearchTest.testSearch(queryFile, databaseName, outName, fine, coarse, testAmm, testAmmPar, testAmmCompressedQuery, testSMSD, testFMCS, testAmmSMSD, useCaching, "no description");
	    
	}
	
	
	public static void testSearch(String queryFile, String databaseName, String outName, double fine, double coarse, 
									boolean testAmm, boolean testAmmPar, boolean testAmmCompressedQuery, 
									boolean testSMSD, boolean testFMCS,boolean testAmmSMSD, boolean useCaching, String description){
		
		boolean testingAmmolite = (testAmm || testAmmPar || testAmmCompressedQuery || testAmmSMSD);
		boolean testingLinear = (testFMCS || testSMSD);
		
		IStructDatabase db = StructDatabaseDecompressor.decompress(databaseName, useCaching);
		List<IAtomContainer> queries = SDFUtils.parseSDF( queryFile);
		Iterator<IAtomContainer> targets = null;
		if( db.numMols() < 50000){
			db = new BigStructDatabase( db);
			((BigStructDatabase) db).preloadMolecules();
			targets = ((BigStructDatabase) db).getMolecules().iterator();
		} else if( testingLinear) {
			List<String> sdfFiles = db.getSourceFiles().getFilenames();
			targets = SDFUtils.parseSDFSetOnline(sdfFiles);
		} 
		
		List<MolStruct> sTargets = db.getStructs();
		
		PrintStream stream = getPrintStream(outName);
		System.out.println("fine_threshold: "+fine+" coarse_threshold: "+coarse);
		System.out.println(db.info());
		System.out.println("Description: "+description);
		stream.println("fine_threshold: "+fine+" coarse_threshold: "+coarse);
		stream.println(db.info());
		stream.println("Description: "+description);

		
		if( testAmm){
			Tester tester = new Ammolite_SingleThread_IsoRank();
			runTest(tester, stream, queries, db, targets, sTargets, fine, coarse);
		}
		if( testAmmSMSD){
			Tester tester = new Ammolite_SingleThread_SMSD();
			runTest(tester, stream, queries, db, targets, sTargets,  fine, coarse);
		}

		if( testAmmPar){
			Tester tester = new Ammolite_MultipleQueriesInParallel_QueryCompression();
			runTest(tester, stream, queries, db, targets, sTargets,  fine, coarse);
		}
		if( testSMSD){
			Tester tester = new SMSD_SingleThread();
			runTest(tester, stream, queries, db, targets, sTargets,  fine, coarse);
		}
		if( testFMCS){
			Tester tester = new FMCS_SingleThread();
			runTest(tester, stream, queries, db, targets, sTargets,  fine, coarse);
		}
		if( testAmmCompressedQuery){
			Tester tester = new Ammolite_QuerywiseParallel_QueryCompression();
			runTest(tester, stream, queries, db, targets, sTargets,  fine, coarse);
		}

		stream.close();
		
	}
	
	public static Iterator<IAtomContainer> getTargetIterator(IStructDatabase db, Iterator<IAtomContainer> oldIterator){
		if( oldIterator == null){
			return null;
		}
		if( db instanceof BigStructDatabase){
			
			return ((BigStructDatabase) db).getMolecules().iterator();
		} else  {
			List<String> sdfFiles = db.getSourceFiles().getFilenames();
			return SDFUtils.parseSDFSetOnline(sdfFiles);
		}
	}
	
	private static void runTest(Tester tester, PrintStream stream, List<IAtomContainer> queries, IStructDatabase db, 
										Iterator<IAtomContainer> targets, List<MolStruct> sTargets, double thresh, 
										double prob){
	    
		String name = tester.getName();
		WallClock clock = new WallClock( name);
		List<SearchResult> results = tester.test(queries, db, targets, sTargets, thresh, prob, name);
		System.out.println("");
		clock.printElapsed();
		stream.println(clock.getElapsedString());
		processResults(results, stream);
	}
	
	private static PrintStream getPrintStream(String outName){
		PrintStream writer= null;
		try {
			writer = new PrintStream(outName, "UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
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
	
	public static void testFMCS(String queryFile){
		List<IAtomContainer> molecules = SDFUtils.parseSDF( queryFile);
		System.out.println(fmcs);
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
				MolStruct sA = sf.makeMoleculeStruct(a);
				MolStruct sB = sf.makeMoleculeStruct(b);
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
	
	public static void processResults(List<SearchResult> results, PrintStream out){
		for(SearchResult r: results){
			processSingleResult( r, out);
		}
	}
	
	private static void processSingleResult(SearchResult result, PrintStream out){
		out.println("START_QUERY "+result.query.getProperty("PUBCHEM_COMPOUND_CID"));
		out.println("START_METHOD "+result.methodName);
		out.println("time: "+result.time());
		out.print("matches: ");
		for(IAtomContainer match: result.matches){
			out.print(MolUtils.getPubID(match));
			out.print(" ");
		}
		out.println("\nSTART_DETAILED_MATCHES");
		for(int i=0; i< result.matches.size(); i++){
			IAtomContainer match = result.matches.get(i);
			int matchSize = result.matchSizes.get(i);
			out.print(MolUtils.getPubID(match));
			out.print(" ");
			out.print(matchSize);
			out.print(" ");
			out.print(MCSUtils.getAtomCountNoHydrogen(match));
			out.print(" ");
			out.println(MCSUtils.getAtomCountNoHydrogen(result.query));
		}
		out.println("END_DETAILED_MATCHES");
		out.println("END_METHOD");
		out.println("END_QUERY");
	}
}
