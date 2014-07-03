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
		
		List<IAtomContainer> queries = parseSDF( queryFile);
		Collection<IAtomContainer> targets = db.getMolecules();

		for(IAtomContainer query: queries){
			SearchResult ammoliteResult = ammoliteSearch(query, db, thresh, prob);
			SearchResult smsdResult = smsdSearch(query, targets, thresh);
			processResults(ammoliteResult, smsdResult);
		}
	}
	
	public static void processResults(SearchResult amm, SearchResult smsd){
		if( amm.query != smsd.query){
			System.err.println("Queries do not match. Exiting.");
			System.exit(1);
		}
		System.out.println("START_QUERY "+amm.query.getProperty("PUBCHEM_COMPOUND_CID"));
		System.out.println("START_METHOD AMMOLITE");
		System.out.println("time: "+amm.time());
		System.out.print("matches: ");
		for(IAtomContainer match: amm.matches){
			System.out.print(match.getProperty("PUBCHEM_COMPOUND_CID"));
			System.out.print(" ");
		}
		System.out.println("\nEND_METHOD");
		System.out.println("START_METHOD SMSD");
		System.out.println("time: "+smsd.time());
		System.out.print("matches: ");
		for(IAtomContainer match: smsd.matches){
			System.out.print(match.getProperty("PUBCHEM_COMPOUND_CID"));
			System.out.print(" ");
		}
		System.out.println("\nEND_METHOD");
		System.out.println("END_QUERY");
	}
	
	
	
	static class SearchResult{
		private long startTime;
		private long endTime;
		public IAtomContainer query;
		public List<IAtomContainer> matches = new ArrayList<IAtomContainer>();
		
		public SearchResult(IAtomContainer q){
			query = q;
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
	
	private static SearchResult ammoliteSearch(IAtomContainer query, IStructDatabase db, double thresh, double prob){
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		SearchResult result = new SearchResult(query);
		result.start();
		MoleculeStruct sQuery = (MoleculeStruct) db.makeMoleculeStruct(query);
		Iterator<IAtomContainer> iter = db.iterator();
		while(iter.hasNext()){
			MoleculeStruct sTarget = (MoleculeStruct) iter.next();
			if(MCS.beatsOverlapThreshold(sQuery, sTarget, sThresh)){
				for(String pubchemID: sTarget.getIDNums()){
					IAtomContainer target = db.getMolecule(pubchemID);
					if(MCS.beatsOverlapThreshold(query, target, thresh)){
						result.addMatch(target);
					}
				}
			}
			
		}
		result.end();
		return result;
	}
	
	private static SearchResult smsdSearch(IAtomContainer query, Collection<IAtomContainer> targets, double thresh){
		SearchResult result = new SearchResult(query);
		result.start();
		for(IAtomContainer target: targets){
			if(MCS.beatsOverlapThreshold(query, target, thresh)){
				result.addMatch(target);
			}
		}
		result.end();
		return result;
	}
	
	private static List<IAtomContainer> parseSDF(String filename){
		IteratingSDFReader molecules = null;
		try{
			
		FileInputStream fs = new FileInputStream(filename);
		BufferedReader br = new BufferedReader( new InputStreamReader(fs ));
		molecules =new IteratingSDFReader( br, DefaultChemObjectBuilder.getInstance());
		} catch( IOException e){
			//edu.mit.csail.ammolite.Logger.error("Failed to read file");
			e.printStackTrace();
		}
		
		List<IAtomContainer> mols = new ArrayList<IAtomContainer>();
		while(molecules.hasNext()){
			mols.add( molecules.next());
		}
		return mols;
	}
}
