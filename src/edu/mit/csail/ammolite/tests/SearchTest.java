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

import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.compression.MoleculeStruct;
import edu.mit.csail.ammolite.database.BigStructDatabase;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;
import edu.mit.csail.ammolite.mcs.MCS;

public class SearchTest {

	
	public static void testCompressedSearch(String queryFile, String databaseName, double thresh, double prob){
		System.out.println("fine_threshold: "+thresh+" coarse_threshold: "+prob);
		BigStructDatabase db = (BigStructDatabase) StructDatabaseDecompressor.decompress(databaseName);
		db.preloadMolecules();
		
		List<IAtomContainer> queries = SDFUtils.parseSDF( queryFile);
		Collection<IAtomContainer> targets = db.getMolecules();
		List<SearchResult> results = new ArrayList<SearchResult>();
		for(IAtomContainer query: queries){
			results.clear();
			results.add( ammoliteSMSDSearch(query, db, thresh, prob));
			results.add( ammoliteFMCSSearch(query, db, thresh, prob));
			results.add( ammoliteCoarseSearch(query, db, thresh, prob));
			results.add( smsdSearch(query, targets, thresh));
			results.add( fmcsSearch(query, targets, thresh));
			processResults(results);
		}
	}
	
	public static void processResults(List<SearchResult> results){
		System.out.println("START_QUERY "+results.get(0).query.getProperty("PUBCHEM_COMPOUND_CID"));
		for(SearchResult r: results){
			processSingleResult(r);
		}
		System.out.println("END_QUERY");
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
		
		public void end(){
			endTime = System.currentTimeMillis();
		}
		
		public long time(){
			return endTime - startTime;
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
		Iterator<IAtomContainer> iter = db.iterator();
		while(iter.hasNext()){
			MoleculeStruct sTarget = (MoleculeStruct) iter.next();
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
	
	private static SearchResult ammoliteFMCSSearch(IAtomContainer query, IStructDatabase db, double thresh, double prob){
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		SearchResult result = new SearchResult(query, "AMMOLITE_FMCS");
		result.start();
		MoleculeStruct sQuery = (MoleculeStruct) db.makeMoleculeStruct(query);
		Iterator<IAtomContainer> iter = db.iterator();
		while(iter.hasNext()){
			MoleculeStruct sTarget = (MoleculeStruct) iter.next();
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
		Iterator<IAtomContainer> iter = db.iterator();
		while(iter.hasNext()){
			MoleculeStruct sTarget = (MoleculeStruct) iter.next();
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
	
	private static List<SearchResult> ammoliteQuerySideCompression(List<IAtomContainer> queries, IStructDatabase db, double thresh, double prob){
		return null;
	}

}
