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

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;

import scala.reflect.internal.Trees.This;
import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.KeyListMap;
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
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.SDFUtils;
import edu.mit.csail.ammolite.utils.WallClock;

public class SearchTest {
	
	private static final String ammoliteCompressed = "AMMOLITE_COMPRESSED_QUERIES";
	private static final String ammoliteCoarseCompressed = "AMMOLITE_COMPRESSED_QUERIES_COARSE";
	private static final String ammoliteCoarse = "AMMOLITE_COARSE_ISORANK";
	private static final String ammoliteSMSDCoarse = "AMMOLITE_COARSE_SMSD";
	private static final String ammoliteDoubleCoarse = "AMMOLITE_COARSE_DOUBLE";
	private static final String ammolite = "AMMOLITE";
	private static final String ammoliteSMSD = "AMMOLITE_SMSD";
	private static final String ammoliteDouble = "AMMOLITE_DOUBLE";
	private static final String ammoliteParallel = "AMMOLITE_PARALLEL";
	private static final String smsd = "SMSD";
	private static final String fmcs = "FMCS";
	
	
	public static void testSearch(String queryFile, String databaseName, String outName, double fine, double coarse, 
									boolean testAmm, boolean testAmmCoarse, boolean testAmmPar,
									boolean testAmmCompressedQuery, boolean testSMSD, boolean testFMCS,
									boolean testAmmSMSDCoarse, boolean testAmmDoubleCoarse, boolean testAmmDouble,
									boolean testAmmSMSD){
		
		BigStructDatabase db = (BigStructDatabase) StructDatabaseDecompressor.decompress(databaseName);
		db.preloadMolecules();
		
		List<IAtomContainer> queries = SDFUtils.parseSDF( queryFile);
		Collection<IAtomContainer> targets = db.getMolecules();
		List<SearchResult> results = new ArrayList<SearchResult>();
		
		PrintStream stream = getPrintStream(outName);
		System.out.println("fine_threshold: "+fine+" coarse_threshold: "+coarse);
		stream.println("fine_threshold: "+fine+" coarse_threshold: "+coarse);
		WallClock clock;
		CommandLineProgressBar progressBar;
		
		if( testAmm){
			clock = new WallClock( ammolite);
			progressBar = new CommandLineProgressBar(ammolite, queries.size());
			for(IAtomContainer query: queries){
				results.add( ammoliteSearch(query, db, fine, coarse));
				progressBar.event();
			}
			clock.printElapsed();
			stream.println(clock.getElapsedString());
			processResults(results, stream);
			results.clear();
		}
		if( testAmmSMSD){
			clock = new WallClock( ammoliteSMSD);
			progressBar = new CommandLineProgressBar(ammoliteSMSD, queries.size());
			for(IAtomContainer query: queries){
				results.add( ammoliteSMSDSearch(query, db, fine, coarse));
				progressBar.event();
			}
			clock.printElapsed();
			stream.println(clock.getElapsedString());
			processResults(results, stream);
			results.clear();
		}
		if( testAmmDouble){
			clock = new WallClock( ammoliteDouble);
			progressBar = new CommandLineProgressBar(ammoliteDouble, queries.size());
			for(IAtomContainer query: queries){
				results.add( ammoliteDoubleSearch(query, db, fine, coarse));
				progressBar.event();
			}
			clock.printElapsed();
			stream.println(clock.getElapsedString());
			processResults(results, stream);
			results.clear();
		}
		if( testAmmCoarse){
			clock = new WallClock( ammoliteCoarse);
			progressBar = new CommandLineProgressBar(ammoliteCoarse, queries.size());
			for(IAtomContainer query: queries){
				results.add( ammoliteCoarseSearch(query, db, fine, coarse));
				progressBar.event();
			}
			clock.printElapsed();
			stream.println(clock.getElapsedString());
			processResults(results, stream);
			results.clear();
		}
		if( testAmmSMSDCoarse){
			clock = new WallClock( ammoliteSMSDCoarse);
			progressBar = new CommandLineProgressBar(ammoliteSMSDCoarse, queries.size());
			for(IAtomContainer query: queries){
				results.add( ammoliteSMSDCoarseSearch(query, db, fine, coarse));
				progressBar.event();
			}
			clock.printElapsed();
			stream.println(clock.getElapsedString());
			processResults(results, stream);
			results.clear();
		}
		if( testAmmDoubleCoarse){
			clock = new WallClock( ammoliteDoubleCoarse);
			progressBar = new CommandLineProgressBar(ammoliteDoubleCoarse, queries.size());
			for(IAtomContainer query: queries){
				results.add( ammoliteDoubleCoarseSearch(query, db, fine, coarse));
				progressBar.event();
			}
			clock.printElapsed();
			stream.println(clock.getElapsedString());
			processResults(results, stream);
			results.clear();
		}
		if( testAmmPar){
			clock = new WallClock( ammoliteParallel);
			progressBar = new CommandLineProgressBar(ammoliteParallel, queries.size());
			for(IAtomContainer query: queries){
				results.add( parallelAmmoliteSearch(query, db, fine, coarse));
				progressBar.event();
			}
			clock.printElapsed();
			stream.println(clock.getElapsedString());
			processResults(results, stream);
			results.clear();
		}
		if( testSMSD){
			clock = new WallClock( smsd);
			progressBar = new CommandLineProgressBar(smsd, queries.size());
			for(IAtomContainer query: queries){
				results.add( smsdSearch(query, targets, fine));
				progressBar.event();
			}
			clock.printElapsed();
			stream.println(clock.getElapsedString());
			processResults(results, stream);
			results.clear();
		}
		if( testFMCS){
			clock = new WallClock( fmcs);
			progressBar = new CommandLineProgressBar(fmcs, queries.size());
			for(IAtomContainer query: queries){
				results.add( fmcsSearch(query, targets, fine));
				progressBar.event();
			}
			clock.printElapsed();
			stream.println(clock.getElapsedString());
			processResults(results, stream);
			results.clear();
		}
		if( testAmmCompressedQuery){
			clock = new WallClock( ammoliteCompressed);
			results.addAll( ammoliteQuerySideCompression(queries, db, fine, coarse));
			clock.printElapsed();
			stream.println(clock.getElapsedString());
			processResults(results, stream);
			results.clear();
		}

		stream.close();
		
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
		System.out.println(ammoliteCoarse);
		System.out.println("id1 id2 size(1) size(2) compressedSize(1) compressedSize(2) overlapSize(1,2) timeInMillis");
		System.out.println("BEGIN_DATA");
		for(int i=0; i<molecules.size(); i++){
			for(int j=0; j<=i; j++){
				IAtomContainer a = molecules.get(i);
				IAtomContainer b = molecules.get(j);
				MolStruct sA = sf.makeMoleculeStruct(a);
				MolStruct sB = sf.makeMoleculeStruct(b);
				long wallClockStart = System.currentTimeMillis();
				int mcsSize = MCS.getIsoRankOverlap(sA, sB);
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
			out.print(match.getProperty("PUBCHEM_COMPOUND_CID"));
			out.print(" ");
		}
		out.println("\nSTART_DETAILED_MATCHES");
		for(int i=0; i< result.matches.size(); i++){
			IAtomContainer match = result.matches.get(i);
			int matchSize = result.matchSizes.get(i);
			out.print(match.getProperty("PUBCHEM_COMPOUND_CID"));
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
			startTime = System.currentTimeMillis();
		}
		
		public void start(long _startTime){
			startTime = _startTime;
		}
		
		public void end(){
			endTime = System.currentTimeMillis();
		}
		
		public void end(long _endTime){
			endTime = _endTime;
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
	
	private static SearchResult ammoliteSearch(IAtomContainer query, IStructDatabase db, double thresh, double prob){
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		SearchResult result = new SearchResult(query, ammolite);
		result.start();
		MolStruct sQuery = (MolStruct) db.makeMoleculeStruct(query);
		Iterator<MolStruct> iter = db.iterator();
		while(iter.hasNext()){
			MolStruct sTarget = iter.next();
			if(MCS.beatsOverlapThresholdIsoRank(sQuery, sTarget, sThresh)){
				for(String pubchemID: sTarget.getIDNums()){
					IAtomContainer target = db.getMolecule(pubchemID);
					int mcsSize = MCS.getSMSDOverlap(query, target);
					if(thresh <= MCSUtils.overlapCoeff(mcsSize, query, target)){
						result.addMatch(target, mcsSize);
					}
				}
			}
			
		}
		result.end();
		return result;
	}
	
	private static SearchResult ammoliteSMSDSearch(IAtomContainer query, IStructDatabase db, double thresh, double prob){
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		SearchResult result = new SearchResult(query, ammoliteSMSD);
		result.start();
		MolStruct sQuery = (MolStruct) db.makeMoleculeStruct(query);
		Iterator<MolStruct> iter = db.iterator();
		while(iter.hasNext()){
			MolStruct sTarget = iter.next();
			if(MCS.beatsOverlapThresholdSMSD(sQuery, sTarget, sThresh)){
				for(String pubchemID: sTarget.getIDNums()){
					IAtomContainer target = db.getMolecule(pubchemID);
					int mcsSize = MCS.getSMSDOverlap(query, target);
					if(thresh <= MCSUtils.overlapCoeff(mcsSize, query, target)){
						result.addMatch(target, mcsSize);
					}
				}
			}
			
		}
		result.end();
		return result;
	}
	
	private static SearchResult ammoliteDoubleSearch(IAtomContainer query, IStructDatabase db, double thresh, double prob){
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		SearchResult result = new SearchResult(query, ammoliteDouble);
		result.start();
		MolStruct sQuery = (MolStruct) db.makeMoleculeStruct(query);
		Iterator<MolStruct> iter = db.iterator();
		while(iter.hasNext()){
			MolStruct sTarget = iter.next();
			if(MCS.beatsOverlapThresholdIsoRank(sQuery, sTarget, sThresh)){
				int mcsSize = MCS.getSMSDOverlap(sTarget, sQuery);
				if(sThresh <= MCSUtils.overlapCoeff(mcsSize, sTarget, sQuery)){
					for(String pubchemID: sTarget.getIDNums()){
						IAtomContainer target = db.getMolecule(pubchemID);
						mcsSize = MCS.getSMSDOverlap(query, target);
						if(thresh <= MCSUtils.overlapCoeff(mcsSize, query, target)){
							result.addMatch(target, mcsSize);
						}
					}
				}
			}
			
		}
		result.end();
		return result;
	}
	
	private static SearchResult parallelAmmoliteSearch(IAtomContainer query, IStructDatabase db, double thresh, double prob){
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		SearchResult result = new SearchResult(query, ammoliteParallel);
		result.start();
		MolStruct sQuery = (MolStruct) db.makeMoleculeStruct(query);
		Iterator<MolStruct> iter = db.iterator();
		List<Callable<Boolean>> coarseTests = new LinkedList<Callable<Boolean>>();
		List<MolStruct> coarseTargetsInOrder = new LinkedList<MolStruct>();
		while(iter.hasNext()){
			MolStruct sTarget = iter.next();
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
				for(String pubchemID: sTarget.getIDNums()){
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
		return result;

	}
	
	
	
	private static SearchResult ammoliteCoarseSearch(IAtomContainer query, IStructDatabase db, double thresh, double prob){
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		SearchResult result = new SearchResult(query, ammoliteCoarse);
		result.start();
		MolStruct sQuery = (MolStruct) db.makeMoleculeStruct(query);
		Iterator<MolStruct> iter = db.iterator();
		while(iter.hasNext()){
			MolStruct sTarget = iter.next();
			int mcsSize = MCS.getIsoRankOverlap(sTarget, sQuery);
			if(sThresh <= MCSUtils.overlapCoeff(mcsSize, sTarget, sQuery)){
				for(String pubchemID: sTarget.getIDNums()){
					IAtomContainer target = db.getMolecule(pubchemID);
					result.addMatch(target, mcsSize);
				}
			}
			
		}
		result.end();
		return result;
	}
	
	private static SearchResult ammoliteSMSDCoarseSearch(IAtomContainer query, IStructDatabase db, double thresh, double prob){
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		SearchResult result = new SearchResult(query, ammoliteSMSDCoarse);
		result.start();
		MolStruct sQuery = (MolStruct) db.makeMoleculeStruct(query);
		Iterator<MolStruct> iter = db.iterator();
		while(iter.hasNext()){
			MolStruct sTarget = iter.next();
			int mcsSize = MCS.getSMSDOverlap(sTarget, sQuery);
			if(sThresh <= MCSUtils.overlapCoeff(mcsSize, sTarget, sQuery)){
				for(String pubchemID: sTarget.getIDNums()){
					IAtomContainer target = db.getMolecule(pubchemID);
					result.addMatch(target, mcsSize);
				}
			}
			
		}
		result.end();
		return result;
	}
	
	private static SearchResult ammoliteDoubleCoarseSearch(IAtomContainer query, IStructDatabase db, double thresh, double prob){
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		SearchResult result = new SearchResult(query, ammoliteDoubleCoarse);
		result.start();
		MolStruct sQuery = (MolStruct) db.makeMoleculeStruct(query);
		Iterator<MolStruct> iter = db.iterator();
		while(iter.hasNext()){
			MolStruct sTarget = iter.next();
			int mcsSize = MCS.getIsoRankOverlap(sTarget, sQuery);
			if(sThresh <= MCSUtils.overlapCoeff(mcsSize, sTarget, sQuery)){
				mcsSize = MCS.getSMSDOverlap(sTarget, sQuery);
				if(sThresh <= MCSUtils.overlapCoeff(mcsSize, sTarget, sQuery)){
					for(String pubchemID: sTarget.getIDNums()){
						IAtomContainer target = db.getMolecule(pubchemID);
						result.addMatch(target, mcsSize);
					}
				}
			}
			
		}
		result.end();
		return result;
	}
	
	
	private static SearchResult smsdSearch(IAtomContainer query, Collection<IAtomContainer> targets, double thresh){
		SearchResult result = new SearchResult(query, smsd);
		result.start();
		for(IAtomContainer target: targets){
			int mcsSize = MCS.getSMSDOverlap(target, query);
			if(thresh <= MCSUtils.overlapCoeff(mcsSize, target, query)){
				result.addMatch(target, mcsSize);
			}
		}
		result.end();
		return result;
	}
	
	private static SearchResult fmcsSearch(IAtomContainer query, Collection<IAtomContainer> targets, double thresh){
		SearchResult result = new SearchResult(query, fmcs);
		result.start();
		for(IAtomContainer target: targets){
			int mcsSize = MCS.getFMCSOverlap(target, query);
			if(thresh <= MCSUtils.overlapCoeff(mcsSize, target, query)){
				result.addMatch(target, mcsSize);
			}
		}
		result.end();
		return result;
	}
	
	private static List<SearchResult> ammoliteCoarseQuerySideCompression(List<IAtomContainer> queries, IStructDatabase db, double thresh, double prob){
		
		KeyListMap<MolStruct,IAtomContainer> compressedQueries = StructCompressor.compressQueries(queries, db.getStructFactory());
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		List<SearchResult> allResults = new ArrayList<SearchResult>( 3* compressedQueries.keySet().size());
		for(MolStruct cQuery: compressedQueries.keySet()){
			List<IAtomContainer> exQueries = compressedQueries.get(cQuery);
			List<SearchResult> results = new ArrayList<SearchResult>( exQueries.size());
			for(IAtomContainer exQuery: exQueries){
				results.add(new SearchResult(exQuery, ammoliteCoarseCompressed));
			}
			Iterator<MolStruct> iter = db.iterator();
			long startTime = System.currentTimeMillis();
			for(SearchResult res: results){
				res.start(startTime);
			}
			while(iter.hasNext()){
				MolStruct sTarget = iter.next();
				int mcsSize = MCS.getIsoRankOverlap(sTarget, cQuery);
				if(sThresh <= MCSUtils.overlapCoeff(mcsSize, sTarget, cQuery)){
					for(String pubchemID: sTarget.getIDNums()){
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
	
	private static List<SearchResult> ammoliteQuerySideCompression(List<IAtomContainer> queries, IStructDatabase db, double thresh, double prob){

		KeyListMap<MolStruct,IAtomContainer> compressedQueries = StructCompressor.compressQueries(queries, db.getStructFactory());
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
			
			iter = db.iterator();
			long startTime = System.currentTimeMillis();
			while(iter.hasNext()){
				MolStruct sTarget = iter.next();
				if(MCS.beatsOverlapThresholdIsoRank(cQuery, sTarget, sThresh)){
					for(String pubchemID: sTarget.getIDNums()){
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
