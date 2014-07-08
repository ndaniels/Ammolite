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
import edu.mit.csail.ammolite.compression.MoleculeStruct;
import edu.mit.csail.ammolite.compression.StructCompressor;
import edu.mit.csail.ammolite.database.BigStructDatabase;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.SDFUtils;

public class SearchTest {

	
	public static void testCompressedSearch(String queryFile, String databaseName, double thresh, double prob){
		System.out.println("fine_threshold: "+thresh+" coarse_threshold: "+prob);
		BigStructDatabase db = (BigStructDatabase) StructDatabaseDecompressor.decompress(databaseName);
		db.preloadMolecules();
		
		List<IAtomContainer> queries = SDFUtils.parseSDF( queryFile);
		Collection<IAtomContainer> targets = db.getMolecules();
		List<SearchResult> results = new ArrayList<SearchResult>();
		results.addAll( ammoliteSMSDQuerySideCompression(queries, db, thresh, prob));
		results.addAll( ammoliteCoarseQuerySideCompression(queries, db, thresh, prob));
		results.addAll( ammoliteIsoRankSMSDQuerySideCompression(queries, db, thresh, prob));
		for(IAtomContainer query: queries){
			results.add( ammoliteSMSDSearch(query, db, thresh, prob));
			results.add( ammoliteIsoRankSMSDSearch(query, db, thresh, prob));
			results.add( ammoliteFMCSSearch(query, db, thresh, prob));
			results.add( ammoliteCoarseSearch(query, db, thresh, prob));
			results.add( smsdSearch(query, targets, thresh));
			results.add( fmcsSearch(query, targets, thresh));
		}
		processResults(results);
	}
	
	public static void processResults(List<SearchResult> results){
		KeyListMap<String, SearchResult> map = new KeyListMap<String, SearchResult>(results.size());
		for(SearchResult r: results){
			String rID = (String) r.query.getProperty("PUBCHEM_COMPOUND_CID");
			map.add(rID, r);
		}
		for(String rID: map.keySet()){
			System.out.println("START_QUERY "+rID);
			for(SearchResult matchingResult: map.get(rID)){
				processSingleResult( matchingResult);
			}
			
			System.out.println("END_QUERY");
		}
	}
	
	
	private static void processSingleResult(SearchResult result){
		System.out.println("START_METHOD "+result.methodName);
		System.out.println("time: "+result.time());
		System.out.print("matches: ");
		for(IAtomContainer match: result.matches){
			System.out.print(match.getProperty("PUBCHEM_COMPOUND_CID"));
			System.out.print(" ");
		}
		System.out.println("\nEND_METHOD");
	}
	
	
	
	static class SearchResult{
		
		private long startTime;
		private long endTime;
		private long duration = -1;
		public IAtomContainer query;
		public String methodName;
		public List<IAtomContainer> matches = new ArrayList<IAtomContainer>();
		
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
		
		public void addMatch(IAtomContainer match){
			matches.add(match);
		}
		
	}
	
	private static SearchResult ammoliteSMSDSearch(IAtomContainer query, IStructDatabase db, double thresh, double prob){
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		SearchResult result = new SearchResult(query, "AMMOLITE_SMSD");
		result.start();
		MoleculeStruct sQuery = (MoleculeStruct) db.makeMoleculeStruct(query);
		Iterator<MoleculeStruct> iter = db.iterator();
		while(iter.hasNext()){
			MoleculeStruct sTarget = iter.next();
			if(MCS.beatsOverlapThreshold(sQuery, sTarget, sThresh)){
				for(String pubchemID: sTarget.getIDNums()){
					IAtomContainer target = db.getMolecule(pubchemID);
					if(MCS.beatsOverlapThresholdSMSD(query, target, thresh)){
						result.addMatch(target);
					}
				}
			}
			
		}
		result.end();
		return result;
	}
	
	private static SearchResult ammoliteIsoRankSMSDSearch(IAtomContainer query, IStructDatabase db, double thresh, double prob){
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		SearchResult result = new SearchResult(query, "AMMOLITE_SMSD_ISORANK");
		result.start();
		MoleculeStruct sQuery = (MoleculeStruct) db.makeMoleculeStruct(query);
		Iterator<MoleculeStruct> iter = db.iterator();
		while(iter.hasNext()){
			MoleculeStruct sTarget = iter.next();
			if(MCS.beatsOverlapThresholdIsoRank(sQuery, sTarget, sThresh)){
				for(String pubchemID: sTarget.getIDNums()){
					IAtomContainer target = db.getMolecule(pubchemID);
					if(MCS.beatsOverlapThresholdSMSD(query, target, thresh)){
						result.addMatch(target);
					}
				}
			}
			
		}
		result.end();
		return result;
	}
	
	private static SearchResult ammoliteFMCSSearch(IAtomContainer query, IStructDatabase db, double thresh, double prob){
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		SearchResult result = new SearchResult(query, "AMMOLITE_FMCS");
		result.start();
		MoleculeStruct sQuery = (MoleculeStruct) db.makeMoleculeStruct(query);
		Iterator<MoleculeStruct> iter = db.iterator();
		while(iter.hasNext()){
			MoleculeStruct sTarget = iter.next();
			if(MCS.beatsOverlapThreshold(sQuery, sTarget, sThresh)){
				for(String pubchemID: sTarget.getIDNums()){
					IAtomContainer target = db.getMolecule(pubchemID);
					if(MCS.beatsOverlapThresholdFMCS(query, target, thresh)){
						result.addMatch(target);
					}
				}
			}
			
		}
		result.end();
		return result;
	}
	
	private static SearchResult ammoliteCoarseSearch(IAtomContainer query, IStructDatabase db, double thresh, double prob){
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		SearchResult result = new SearchResult(query, "AMMOLITE_COARSE");
		result.start();
		MoleculeStruct sQuery = (MoleculeStruct) db.makeMoleculeStruct(query);
		Iterator<MoleculeStruct> iter = db.iterator();
		while(iter.hasNext()){
			MoleculeStruct sTarget = iter.next();
			if(MCS.beatsOverlapThreshold(sQuery, sTarget, sThresh)){
				for(String pubchemID: sTarget.getIDNums()){
					IAtomContainer target = db.getMolecule(pubchemID);
					result.addMatch(target);
				}
			}
			
		}
		result.end();
		return result;
	}
	
	private static SearchResult smsdSearch(IAtomContainer query, Collection<IAtomContainer> targets, double thresh){
		SearchResult result = new SearchResult(query, "SMSD");
		result.start();
		for(IAtomContainer target: targets){
			if(MCS.beatsOverlapThresholdSMSD(query, target, thresh)){
				result.addMatch(target);
			}
		}
		result.end();
		return result;
	}
	
	private static SearchResult fmcsSearch(IAtomContainer query, Collection<IAtomContainer> targets, double thresh){
		SearchResult result = new SearchResult(query, "FMCS");
		result.start();
		for(IAtomContainer target: targets){
			if(MCS.beatsOverlapThresholdFMCS(query, target, thresh)){
				result.addMatch(target);
			}
		}
		result.end();
		return result;
	}
	
	private static List<SearchResult> ammoliteCoarseQuerySideCompression(List<IAtomContainer> queries, IStructDatabase db, double thresh, double prob){
		
		System.out.print("Compressing Queries... ");
		KeyListMap<MoleculeStruct,IAtomContainer> compressedQueries = StructCompressor.compressQueries(queries, db.getStructFactory());
		System.out.println("Done.");
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		List<SearchResult> allResults = new ArrayList<SearchResult>( 3* compressedQueries.keySet().size());
		for(MoleculeStruct cQuery: compressedQueries.keySet()){
			List<IAtomContainer> exQueries = compressedQueries.get(cQuery);
			List<SearchResult> results = new ArrayList<SearchResult>( exQueries.size());
			for(IAtomContainer exQuery: exQueries){
				results.add(new SearchResult(exQuery, "AMMOLITE_QUERY_SIDE_COMPRESSION_COARSE"));
			}
			Iterator<MoleculeStruct> iter = db.iterator();
			long startTime = System.currentTimeMillis();
			for(SearchResult res: results){
				res.start(startTime);
			}
			while(iter.hasNext()){
				MoleculeStruct sTarget = iter.next();
				if(MCS.beatsOverlapThreshold(cQuery, sTarget, sThresh)){
					for(String pubchemID: sTarget.getIDNums()){
						IAtomContainer target = db.getMolecule(pubchemID);
						for(SearchResult res: results){
							res.addMatch(target);
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
	
	private static List<SearchResult> ammoliteSMSDQuerySideCompression(List<IAtomContainer> queries, IStructDatabase db, double thresh, double prob){
		System.out.print("Compressing Queries... ");
		KeyListMap<MoleculeStruct,IAtomContainer> compressedQueries = StructCompressor.compressQueries(queries, db.getStructFactory());
		System.out.println("Done.");
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		List<SearchResult> allResults = new ArrayList<SearchResult>( 3* compressedQueries.keySet().size());
		Iterator<MoleculeStruct> iter;
		
		for(MoleculeStruct cQuery: compressedQueries.keySet()){
			List<IAtomContainer> exQueries = compressedQueries.get(cQuery);
			List<SearchResult> results = new ArrayList<SearchResult>( exQueries.size());
			for(IAtomContainer exQuery: exQueries){
				results.add(new SearchResult(exQuery, "AMMOLITE_QUERY_SIDE_COMPRESSION_SMSD"));
			}
			
			iter = db.iterator();
			long startTime = System.currentTimeMillis();
			while(iter.hasNext()){
				MoleculeStruct sTarget = iter.next();
				if(MCS.beatsOverlapThreshold(cQuery, sTarget, sThresh)){
					for(String pubchemID: sTarget.getIDNums()){
						IAtomContainer target = db.getMolecule(pubchemID);
						for(int i=0; i<exQueries.size(); i++){
							IAtomContainer exQuery = exQueries.get(i);
							SearchResult result = results.get(i);
							if(MCS.beatsOverlapThresholdSMSD(exQuery, target, thresh)){
								result.addMatch(target);
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
	
	private static List<SearchResult> ammoliteIsoRankSMSDQuerySideCompression(List<IAtomContainer> queries, IStructDatabase db, double thresh, double prob){
		System.out.print("Compressing Queries... ");
		KeyListMap<MoleculeStruct,IAtomContainer> compressedQueries = StructCompressor.compressQueries(queries, db.getStructFactory());
		System.out.println("Done.");
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		List<SearchResult> allResults = new ArrayList<SearchResult>( 3* compressedQueries.keySet().size());
		Iterator<MoleculeStruct> iter;
		
		for(MoleculeStruct cQuery: compressedQueries.keySet()){
			List<IAtomContainer> exQueries = compressedQueries.get(cQuery);
			List<SearchResult> results = new ArrayList<SearchResult>( exQueries.size());
			for(IAtomContainer exQuery: exQueries){
				results.add(new SearchResult(exQuery, "AMMOLITE_QUERY_SIDE_COMPRESSION_SMSD_ISORANK"));
			}
			
			iter = db.iterator();
			long startTime = System.currentTimeMillis();
			while(iter.hasNext()){
				MoleculeStruct sTarget = iter.next();
				if(MCS.beatsOverlapThresholdIsoRank(cQuery, sTarget, sThresh)){
					for(String pubchemID: sTarget.getIDNums()){
						IAtomContainer target = db.getMolecule(pubchemID);
						for(int i=0; i<exQueries.size(); i++){
							IAtomContainer exQuery = exQueries.get(i);
							SearchResult result = results.get(i);
							if(MCS.beatsOverlapThresholdSMSD(exQuery, target, thresh)){
								result.addMatch(target);
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
