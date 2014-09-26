package edu.mit.csail.ammolite.tests;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;

import scala.reflect.internal.Trees.This;
import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.compression.DatabaseCompression;
import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.compression.MoleculeStructFactory;
import edu.mit.csail.ammolite.compression.StructCompressor;
import edu.mit.csail.ammolite.database.BigStructDatabase;
import edu.mit.csail.ammolite.database.CompressionType;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.ParallelWorkerPool;
import edu.mit.csail.ammolite.utils.PubchemID;
import edu.mit.csail.ammolite.utils.SDFUtils;
import edu.mit.csail.ammolite.utils.StructID;
import edu.mit.csail.ammolite.utils.WallClock;

public class SearchTest {
	
	private static final String ammoliteCompressed = "AMMOLITE_COMPRESSED_QUERIES";
	private static final String ammoliteParallelCompressed = "AMMOLITE_COMPRESSED_QUERIES_PARALLEL";
	private static final String ammoliteCoarseCompressed = "AMMOLITE_COMPRESSED_QUERIES_COARSE";

	private static final String ammolite = "AMMOLITE_ISORANK";
	private static final String ammoliteSMSD = "AMMOLITE_SMSD";
	private static final String ammoliteParallel = "AMMOLITE_PARALLEL";
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
			Tester tester = new AmmoliteSearch();
			runTest(tester, stream, queries, db, targets, sTargets, fine, coarse, ammolite);
		}
		if( testAmmSMSD){
			Tester tester = new AmmoliteSMSDSearch();
			runTest(tester, stream, queries, db, targets, sTargets,  fine, coarse, ammoliteSMSD);
		}

		if( testAmmPar){
			Tester tester = new AmmoliteParallelSearch();
			runTest(tester, stream, queries, db, targets, sTargets,  fine, coarse, ammoliteParallel);
		}
		if( testSMSD){
			Tester tester = new SMSDSearch();
			runTest(tester, stream, queries, db, targets, sTargets,  fine, coarse, smsd);
		}
		if( testFMCS){
			Tester tester = new FMCSSearch();
			runTest(tester, stream, queries, db, targets, sTargets,  fine, coarse, fmcs);
		}
		if( testAmmCompressedQuery){
			Tester tester = new ParallelQuerySideCompression();
			runTest(tester, stream, queries, db, targets, sTargets,  fine, coarse, ammoliteParallelCompressed);
		}

		stream.close();
		
	}
	
	private static Iterator<IAtomContainer> getTargetIterator(IStructDatabase db, Iterator<IAtomContainer> oldIterator){
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
										double prob, String name){
		
		WallClock clock = new WallClock( name);
		List<SearchResult> results = new ArrayList<SearchResult>();
		results.addAll( tester.test(queries, db, targets, sTargets, thresh, prob, name));
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
	

	static interface Tester{
		public List<SearchResult> test(List<IAtomContainer> queries, IStructDatabase db, 
										Iterator<IAtomContainer> targets,  List<MolStruct> sTargets, double thresh, 
										double prob, String name);
	}
	
	static abstract class MultiTester implements Tester {
		abstract List<SearchResult> singleTestMultipleResults(IAtomContainer query, IStructDatabase db, 
										Iterator<IAtomContainer> targets,  List<MolStruct> sTargets, double fineThresh, 
										double coarseThresh, String name);
		
		public List<SearchResult> test(List<IAtomContainer> queries, IStructDatabase db, 
										Iterator<IAtomContainer> targets, List<MolStruct> sTargets,  double fineThresh, 
										double coarseThresh, String name){
			List<SearchResult> results = new LinkedList<SearchResult>();
			CommandLineProgressBar progressBar = new CommandLineProgressBar(name, queries.size());
			for(IAtomContainer query: queries){
				targets = getTargetIterator(db, targets);
				List<SearchResult> result = singleTestMultipleResults(query, db, targets, sTargets, fineThresh, coarseThresh, name);
				results.addAll(result);
				progressBar.event();
			}
			return results;
		}
	}
	
	static abstract class MultiSingleTester extends MultiTester{
		abstract SearchResult singleTest(IAtomContainer query, IStructDatabase db, 
										Iterator<IAtomContainer> targets,  List<MolStruct> sTargets, double fineThresh, 
										double coarseThresh, String name);
		
		public List<SearchResult> singleTestMultipleResults(IAtomContainer query, IStructDatabase db, 
												Iterator<IAtomContainer> targets,  List<MolStruct> sTargets, double fineThresh, 
												double coarseThresh, String name){
			List<SearchResult> l = new ArrayList<SearchResult>(1);
			l.add(singleTest(query, db, targets, sTargets, fineThresh, coarseThresh, name));
			return l;
		}
	}
	
	static abstract class AmmoliteTester extends MultiTester{
		
		abstract int getCoarseOverlap(MolStruct query, MolStruct target);
		
		protected int getFineOverlap(IAtomContainer query, IAtomContainer target){
			return MCS.getSMSDOverlap(query, target);
		}
		
		public List<SearchResult> singleTestMultipleResults(IAtomContainer query, IStructDatabase db, 
										Iterator<IAtomContainer> targets, List<MolStruct> sTargets,  double fineThresh, 
										double coarseThresh, String name){
			SearchResult result = new SearchResult(query, name);
			SearchResult coarseResult = new SearchResult(query, name+"_COARSE");
			result.start();
			MolStruct sQuery = (MolStruct) db.makeMoleculeStruct(query);
		
			for(MolStruct sTarget: sTargets){
				coarseResult.start();
				int coarseOverlap = getCoarseOverlap(sQuery, sTarget);
				coarseResult.end();
				if(coarseThresh <= MCSUtils.overlapCoeff(coarseOverlap, sQuery, sTarget)){
					List<IAtomContainer> sTargetMatches;
					if(db.isOrganized()){
						sTargetMatches = db.getMatchingMolecules(MolUtils.getStructID(sTarget));
					} else {
						sTargetMatches = new ArrayList<IAtomContainer>();
						for(PubchemID pubchemID: sTarget.getIDNums()){
							IAtomContainer target = db.getMolecule(pubchemID);
							sTargetMatches.add(target);
						}
					}
			
					
					for(IAtomContainer target: sTargetMatches){
						coarseResult.addMatch(target, coarseOverlap);
						int fineOverlap = getFineOverlap(query, target);
						if(fineThresh <= MCSUtils.overlapCoeff(fineOverlap, query, target)){
							result.addMatch(target, fineOverlap);
						}
					}
				}
				
			}
			result.end();
			List<SearchResult> l = new ArrayList<SearchResult>(2);
			l.add(result);
			l.add(coarseResult);
			return l;

		}
	}
	
	static class SearchResult{
		
		private long startTime;
		private long endTime;
		private long duration = -1;
		public IAtomContainer query;
		public String methodName;
		public List<IAtomContainer> matches = new ArrayList<IAtomContainer>();
		public List<Integer> matchSizes = new ArrayList<Integer>();
		
		public SearchResult(IAtomContainer q, String _methodName){
			query = q;
			methodName = _methodName;
		}
		
		public void start(){
			start(System.currentTimeMillis());
		}
		
		public void start(long _startTime){
			startTime = _startTime;
		}
		
		public void end(){
			end( System.currentTimeMillis());
		}
		
		public void end(long _endTime){
			endTime = _endTime;
			if(duration < 0){
				duration = 0;
			}
			duration += endTime - startTime;
		}
		
		public void setDuration(long _duration){
			duration = _duration;
		}
		
		public long time(){
			if(duration < 0){
				duration = endTime - startTime;
			}
			return duration;
		}
		
		public void addMatch(IAtomContainer match, int mcsSize){
			matches.add(match);
			matchSizes.add(mcsSize);
		}
		
	}
	
	static class AmmoliteSearch extends AmmoliteTester {
	
		int getCoarseOverlap(MolStruct query, MolStruct target) {
			return MCS.getIsoRankOverlap(target, query);
		}

	}
	
	static class AmmoliteSMSDSearch extends AmmoliteTester {
		
		int getCoarseOverlap(MolStruct query, MolStruct target) {
			return MCS.getSMSDOverlap(target, query);
		}
	}
	

	static class AmmoliteParallelSearch implements Tester {
		
		public List<SearchResult> test(List<IAtomContainer> queries, IStructDatabase db, 
											Iterator<IAtomContainer> targets, List<MolStruct> sTargets,  double thresh, 
											double prob, String name){
			List<SearchResult> results = new LinkedList<SearchResult>();
			for(IAtomContainer query: queries){
				double sThresh = prob; // !!! not using the conversion I came up with, yet.
				SearchResult result = new SearchResult(query, name);
				result.start();
				MolStruct sQuery = (MolStruct) db.makeMoleculeStruct(query);
				
				List<Callable<Boolean>> coarseTests = new LinkedList<Callable<Boolean>>();
				List<MolStruct> coarseTargetsInOrder = new LinkedList<MolStruct>();
				for(MolStruct sTarget: sTargets){
					
					coarseTests.add(MCS.getCallableIsoRankTest(sQuery, sTarget, sThresh));
					coarseTargetsInOrder.add(sTarget);
				}
				List<Boolean> coarseResults = ParallelUtils.parallelFullExecution(coarseTests);
				List<Callable<Boolean>> fineTests = new LinkedList<Callable<Boolean>>();
				List<IAtomContainer> fineTargetsInOrder = new LinkedList<IAtomContainer>();
				for(int i=0; i<coarseResults.size(); i++){
					boolean coarseMatch = coarseResults.get(i);
					MolStruct sTarget = coarseTargetsInOrder.get(i);
					
					if( coarseMatch){
						for(PubchemID pubchemID: sTarget.getIDNums()){
							IAtomContainer target = db.getMolecule(pubchemID);
							fineTests.add(MCS.getCallableSMSDTest(query, target, thresh));
							fineTargetsInOrder.add(target);
						}
					}
				}
				
				List<Boolean> fineResults = ParallelUtils.parallelFullExecution(fineTests);
				for(int i=0; i<fineResults.size(); i++){
					boolean fineMatch = fineResults.get(i);
					if(fineMatch){
						IAtomContainer target = fineTargetsInOrder.get(i);
						result.addMatch(target, -1);
					}

				}
				result.end();
				results.add(result);
			}
			return results;
		}
	}
	
	static class ParallelQuerySideCompression implements Tester {
	    
	    private final int CHUNK_SIZE = 1000;
	    private ParallelWorkerPool workerPool = new ParallelWorkerPool();
	    
	    /**
	     * Creates and runs an SMSD operation between every matching in a set of queries and 
	     * a set of targets.
	     * 
	     * @param queries
	     * @param targets
	     * @return
	     */
	    private List<List<Integer>> testSetOfMolecules(List<IAtomContainer> queries, List<IAtomContainer> targets){
	        List<List<Integer>> results = new ArrayList<List<Integer>>(queries.size());
	        List<Callable<Integer>> tests = new ArrayList<Callable<Integer>>(targets.size());
	        
	        for(IAtomContainer fineQuery: queries){
	            tests.clear();
                for(IAtomContainer fineTarget: targets){
                    tests.add(MCS.getCallableSMSDOperation(fineTarget, fineQuery));
                }
                results.add( workerPool.parallelFullExecution(tests));
            }
	        return results;
	    }
	    
	        
	    private void processTests(List<IAtomContainer> queries, List<IAtomContainer> targets, List<List<Integer>> testResults, List<SearchResult> searchResults, List<SearchResult> coarseSearchResults, double thresh){
	        for(int i=0; i<queries.size(); ++i){
                IAtomContainer query = queries.get(i);
                List<Integer> myMatchSizes = testResults.get(i);
                SearchResult mySearchResult = searchResults.get(i);
                SearchResult myCoarseSearchResult = coarseSearchResults.get(i);
                
                for(int j=0; j<targets.size(); j++){
                    int matchSize = myMatchSizes.get(j);
                    IAtomContainer relevantTarget = targets.get(j);
                    boolean result = MCSUtils.overlapCoeff(matchSize, query, relevantTarget) >= thresh;
                    myCoarseSearchResult.addMatch(relevantTarget, matchSize);
                    if(result){
                        mySearchResult.addMatch(relevantTarget, matchSize);
                    }
                }
            }
	    }
	    
	    private List<StructID> coarseSearch(MolStruct coarseQuery, List<MolStruct> sTargets, double coarseThresh){
	        List<Callable<Boolean>> coarseTests = new ArrayList<Callable<Boolean>>();
            for(MolStruct coarseTarget: sTargets){
                coarseTests.add(MCS.getCallableSMSDTest(coarseQuery, coarseTarget, coarseThresh));
            }
            List<Boolean> coarseResults = workerPool.parallelFullExecution(coarseTests);
            List<StructID> coarseMatchIDs = new ArrayList<StructID>();
            
            for(int i=0; i<coarseResults.size(); ++i){
                boolean result = coarseResults.get(i);
                if( result){
                    MolStruct coarseMatch = sTargets.get(i);
                    coarseMatchIDs.add(MolUtils.getStructID(coarseMatch));
                }
            }
            return coarseMatchIDs;
	    }
	    
	    private void fineSearch(List<IAtomContainer> fineQueries, List<StructID> coarseMatchIDs, IStructDatabase db, 
	                            List<SearchResult> mySearchResults, List<SearchResult> myCoarseSearchResults, double thresh){
	        // Fine Search
            List<IAtomContainer> fineTargetChunk = new ArrayList<IAtomContainer>(CHUNK_SIZE);
            for(StructID sID: coarseMatchIDs){
                if( fineTargetChunk.size() < CHUNK_SIZE){
                    fineTargetChunk.addAll(db.getMatchingMolecules(sID));
                } else {
                    List<List<Integer>> results = testSetOfMolecules(fineQueries, fineTargetChunk);
                    processTests(fineQueries, fineTargetChunk, results, mySearchResults, myCoarseSearchResults, thresh);
                }  
            }
            List<List<Integer>> results = testSetOfMolecules(fineQueries, fineTargetChunk);
            processTests(fineQueries, fineTargetChunk, results, mySearchResults, myCoarseSearchResults, thresh);
	    }

        @Override
        public List<SearchResult> test(List<IAtomContainer> queries,
                                        IStructDatabase db, Iterator<IAtomContainer> targets,
                                        List<MolStruct> sTargets, double thresh, double coarseThresh,
                                        String name) {
            
            KeyListMap<MolStruct,IAtomContainer> compressedQueries = DatabaseCompression.compressMoleculeSet(queries, db.getStructFactory());
            CommandLineProgressBar bar = new CommandLineProgressBar(name, queries.size());
            List<SearchResult> allResults = new ArrayList<SearchResult>();
            
            for(MolStruct coarseQuery: compressedQueries.keySet()){
                List<IAtomContainer> fineQueries = compressedQueries.get(coarseQuery);
                List<SearchResult> mySearchResults = new ArrayList<SearchResult>(fineQueries.size());
                List<SearchResult> myCoarseSearchResults = new ArrayList<SearchResult>(fineQueries.size());
                
                // Results pre-processing
                for(IAtomContainer fineQuery: fineQueries){
                    SearchResult mySearchResult = new SearchResult(fineQuery, name);
                    mySearchResult.start();
                    mySearchResults.add( mySearchResult);
                    allResults.add( mySearchResult);
                    
                    SearchResult myCoarseSearchResult = new SearchResult(fineQuery, name+"_COARSE");
                    myCoarseSearchResult.start();
                    myCoarseSearchResults.add( myCoarseSearchResult);
                    allResults.add( myCoarseSearchResult);
                }

                List<StructID> coarseMatchIDs = coarseSearch(coarseQuery, sTargets, coarseThresh);
                fineSearch( fineQueries, coarseMatchIDs, db, mySearchResults, myCoarseSearchResults, thresh);
                
                // Results Processing
                for(SearchResult result: mySearchResults){
                    result.end();
                    bar.event();
                }
                for(SearchResult coarseResult: myCoarseSearchResults){
                    coarseResult.end();
                }
            }
            workerPool.shutdown();
            return allResults;
        }  
	}
	    
	
	static class SMSDSearch extends MultiSingleTester {

		public SearchResult singleTest(IAtomContainer query, IStructDatabase db, 
										Iterator<IAtomContainer> targets,  List<MolStruct> sTargets, double thresh, 
										double prob, String name){

			SearchResult result = new SearchResult(query, name);
			result.start();
			
			for(IAtomContainer target= targets.next(); targets.hasNext(); target=targets.next()){
				int mcsSize = MCS.getSMSDOverlap(target, query);
				if(thresh <= MCSUtils.overlapCoeff(mcsSize, target, query)){
					result.addMatch(target, mcsSize);
				}
			}
			result.end();
			return result;

		}
		
	}
	
	static class FMCSSearch extends MultiSingleTester {

		public SearchResult singleTest(IAtomContainer query, IStructDatabase db, 
										Iterator<IAtomContainer> targets, List<MolStruct> sTargets,  double thresh, 
										double prob, String name){

			SearchResult result = new SearchResult(query, name);
			result.start();
			for(IAtomContainer target= targets.next(); targets.hasNext(); target=targets.next()){
				int mcsSize = MCS.getFMCSOverlap(target, query);
				if(thresh <= MCSUtils.overlapCoeff(mcsSize, target, query)){
					result.addMatch(target, mcsSize);
				}
			}
			result.end();
			return result;

		}
		
	}


	static class AmmoliteCoarseQuerySideCompressionSearch implements Tester {


		public List<SearchResult> test(List<IAtomContainer> queries,
										IStructDatabase db, Iterator<IAtomContainer> targets, List<MolStruct> sTargets, 
										double thresh, double prob, String name) {
			KeyListMap<MolStruct,IAtomContainer> compressedQueries = DatabaseCompression.compressMoleculeSet(queries, db.getStructFactory());
			double sThresh = prob; // !!! not using the conversion I came up with, yet.
			List<SearchResult> allResults = new ArrayList<SearchResult>( 3* compressedQueries.keySet().size());
			for(MolStruct cQuery: compressedQueries.keySet()){
				List<IAtomContainer> exQueries = compressedQueries.get(cQuery);
				List<SearchResult> results = new ArrayList<SearchResult>( exQueries.size());
				for(IAtomContainer exQuery: exQueries){
					results.add(new SearchResult(exQuery, ammoliteCoarseCompressed));
				}

				long startTime = System.currentTimeMillis();
				for(SearchResult res: results){
					res.start(startTime);
				}
				for(MolStruct sTarget: sTargets){
					
					int mcsSize = MCS.getIsoRankOverlap(sTarget, cQuery);
					if(sThresh <= MCSUtils.overlapCoeff(mcsSize, sTarget, cQuery)){
						for(PubchemID pubchemID: sTarget.getIDNums()){
							IAtomContainer target = db.getMolecule(pubchemID);
							for(SearchResult res: results){
								res.addMatch(target, mcsSize);
							}
						}
					}	
				}
				long endTime = System.currentTimeMillis();
				for(SearchResult res: results){
					res.end(endTime);
				}
				allResults.addAll(results);
			}
			return allResults;
		}
		
	}
	
	static class AmmoliteQuerySideCompressionSearch implements Tester {

		@Override
		public List<SearchResult> test(List<IAtomContainer> queries,
				IStructDatabase db, Iterator<IAtomContainer> targets, List<MolStruct> sTargets,
				double thresh, double prob, String name) {
			KeyListMap<MolStruct,IAtomContainer> compressedQueries = DatabaseCompression.compressMoleculeSet(queries, db.getStructFactory());
			CommandLineProgressBar progressBar = new CommandLineProgressBar(ammoliteCompressed, compressedQueries.keySet().size());
			double sThresh = prob; // !!! not using the conversion I came up with, yet.
			List<SearchResult> allResults = new ArrayList<SearchResult>( 3* compressedQueries.keySet().size());
			Iterator<MolStruct> iter;
			
			for(MolStruct cQuery: compressedQueries.keySet()){
				List<IAtomContainer> exQueries = compressedQueries.get(cQuery);
				List<SearchResult> results = new ArrayList<SearchResult>( exQueries.size());
				for(IAtomContainer exQuery: exQueries){
					results.add(new SearchResult(exQuery, ammoliteCompressed));
				}
				
				
				long startTime = System.currentTimeMillis();
				for(MolStruct sTarget: sTargets){
					
					if(MCS.beatsOverlapThresholdIsoRank(cQuery, sTarget, sThresh)){
						for(PubchemID pubchemID: sTarget.getIDNums()){
							IAtomContainer target = db.getMolecule(pubchemID);
							for(int i=0; i<exQueries.size(); i++){
								IAtomContainer exQuery = exQueries.get(i);
								SearchResult result = results.get(i);
								int mcsSize = MCS.getSMSDOverlap(target, exQuery);
								if(thresh <= MCSUtils.overlapCoeff(mcsSize, target, exQuery)){
									result.addMatch(target, mcsSize);
								}
							}
						}
					}	
				}
				long endTime = System.currentTimeMillis();
				long aveTime = (endTime - startTime) / exQueries.size();
				for(SearchResult res: results){
					res.setDuration(aveTime);
				}
				allResults.addAll(results);
				progressBar.event();
			}
			return allResults;
		}		
	}

}
