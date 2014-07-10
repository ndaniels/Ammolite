package edu.mit.csail.ammolite.tests;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;

import scala.reflect.internal.Trees.This;
import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.compression.StructCompressor;
import edu.mit.csail.ammolite.database.BigStructDatabase;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.SDFUtils;

public class SearchTest {
	
	private static final String ammoliteCompressed = "AMMOLITE_COMPRESSED_QUERIES";
	private static final String ammoliteCoarseCompressed = "AMMOLITE_COMPRESSED_QUERIES_COARSE";
	private static final String ammoliteCoarse = "AMMOLITE_COARSE";
	private static final String ammolite = "AMMOLITE";
	private static final String smsd = "SMSD";
	private static final String fmcs = "FMCS";
	
	
	public static void testCompressedSearch(String queryFile, String databaseName, double thresh, double prob){
		System.out.println("fine_threshold: "+thresh+" coarse_threshold: "+prob);
		BigStructDatabase db = (BigStructDatabase) StructDatabaseDecompressor.decompress(databaseName);
		db.preloadMolecules();
		
		List<IAtomContainer> queries = SDFUtils.parseSDF( queryFile);
		Collection<IAtomContainer> targets = db.getMolecules();
		List<SearchResult> results = new ArrayList<SearchResult>();

		results.addAll( ammoliteQuerySideCompression(queries, db, thresh, prob));


		for(IAtomContainer query: queries){
			results.add( ammoliteSearch(query, db, thresh, prob));
		}

		for(IAtomContainer query: queries){
			results.add( ammoliteCoarseSearch(query, db, thresh, prob));
		}
		
		for(IAtomContainer query: queries){
			results.add( smsdSearch(query, targets, thresh));
		}
		
		processResults(results);
	}
	
	public static void processResults(List<SearchResult> results){
		for(SearchResult r: results){
			processSingleResult( r);
		}
	}
	
	
	private static void processSingleResult(SearchResult result){
		System.out.println("START_QUERY "+result.query.getProperty("PUBCHEM_COMPOUND_CID"));
		System.out.println("START_METHOD "+result.methodName);
		System.out.println("time: "+result.time());
		System.out.print("matches: ");
		for(IAtomContainer match: result.matches){
			System.out.print(match.getProperty("PUBCHEM_COMPOUND_CID"));
			System.out.print(" ");
		}
		System.out.println("\nSTART_DETAILED_MATCHES");
		for(int i=0; i< result.matches.size(); i++){
			IAtomContainer match = result.matches.get(i);
			int matchSize = result.matchSizes.get(i);
			System.out.print(match.getProperty("PUBCHEM_COMPOUND_CID"));
			System.out.print(" ");
			System.out.print(matchSize);
			System.out.print(" ");
			System.out.print(MCSUtils.getAtomCountNoHydrogen(match));
			System.out.print(" ");
			System.out.println(MCSUtils.getAtomCountNoHydrogen(result.query));
		}
		System.out.println("END_DETAILED_MATCHES");
		System.out.println("END_METHOD");
		System.out.println("END_QUERY");
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
		}
		return allResults;
	}

}
