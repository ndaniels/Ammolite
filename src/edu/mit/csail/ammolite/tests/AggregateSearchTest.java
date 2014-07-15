package edu.mit.csail.ammolite.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.aggregation.Cluster;
import edu.mit.csail.ammolite.aggregation.ClusterDatabase;
import edu.mit.csail.ammolite.aggregation.ClusterDatabaseDecompressor;
import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.SDFUtils;

public class AggregateSearchTest {
	
	private static HashMap<IAtomContainer, AggSearchResult> results = new HashMap<IAtomContainer, AggSearchResult>();
	private static IStructDatabase db;
	
	static class AggSearchResult {
		
		private long startTime;
		private long endTime;
		private long aggStart;
		private long totalAggTime;
		private long duration = -1;
		public IAtomContainer query;
		public String methodName;
		public List<MolStruct> aggMatches = new ArrayList<MolStruct>();
		public List<MolStruct> coarseMatches = new ArrayList<MolStruct>();
		public List<IAtomContainer> matches = new ArrayList<IAtomContainer>();
		public List<Integer> matchSizes = new ArrayList<Integer>();
		
		public AggSearchResult(IAtomContainer q, String _methodName){
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
		
		public void startAggTime(){
			aggStart = System.currentTimeMillis();
		}
		
		public void endAggTime(){
			long dur = System.currentTimeMillis() - aggStart;
			totalAggTime += dur;
		}
		
		public long aggTime(){
			return totalAggTime;
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
		
		public void addAggMatch(MolStruct match){
			aggMatches.add(match);
		}
		
		public void addCoarseMatch(MolStruct match){
			coarseMatches.add(match);
		}
		
		public void addMatch(IAtomContainer match, int matchSize){
			matchSizes.add(matchSize);
			matches.add(match);
		}
		
		public void print(){
			System.out.println("START_QUERY "+query.getProperty("PUBCHEM_COMPOUND_CID"));
			System.out.println("START_METHOD AGGREGATION");
			System.out.println("time: "+aggTime());
			System.out.print("matches: ");
			for(IAtomContainer match: aggMatches){
				System.out.print(match.getProperty("PUBCHEM_COMPOUND_CID"));
				System.out.print(" ");
			}
			System.out.println("\nEND_METHOD");
			System.out.println("START_METHOD AGGREGATION_COARSE");
			System.out.println("time: "+time());
			System.out.print("matches: ");
			for(IAtomContainer match: coarseMatches){
				System.out.print(match.getProperty("PUBCHEM_COMPOUND_CID"));
				System.out.print(" ");
			}
			System.out.println("\nEND_METHOD");
			System.out.println("START_METHOD AGGREGATION_COARSE_FINE");
			System.out.println("time: "+time());
			System.out.print("matches: ");
			for(IAtomContainer match: matches){
				System.out.print(match.getProperty("PUBCHEM_COMPOUND_CID"));
				System.out.print(" ");
			}
			System.out.println("\nSTART_DETAILED_MATCHES");
			for(int i=0; i< matches.size(); i++){
				IAtomContainer match = matches.get(i);
				int matchSize = matchSizes.get(i);
				System.out.print(match.getProperty("PUBCHEM_COMPOUND_CID"));
				System.out.print(" ");
				System.out.print(matchSize);
				System.out.print(" ");
				System.out.print(MCSUtils.getAtomCountNoHydrogen(match));
				System.out.print(" ");
				System.out.println(MCSUtils.getAtomCountNoHydrogen(query));
			}
			System.out.println("END_DETAILED_MATCHES");
			System.out.println("END_METHOD");
			System.out.println("END_QUERY");
		}
		
	}
	
	public static void testAggSearch(String clusterName, String queryFile, double thresh, double prob){
		ClusterDatabase cDB = ClusterDatabaseDecompressor.decompress(clusterName);
		List<Cluster> cList = cDB.getClusterList();
		db = cDB.getDatabase();
		List<IAtomContainer> queries = SDFUtils.parseSDF(queryFile);
		for(IAtomContainer q: queries){
			MolStruct sQ = db.makeMoleculeStruct(q);
			AggSearchResult res = new AggSearchResult(sQ, "AGG_SEARCH");
			results.put(q, res);
			res.start();
			for(Cluster c: cList){
				singleAggQuery(q, sQ, c, thresh, prob);
			}
			res.end();
		}
		System.out.println("fine_threshold: "+thresh+" coarse_threshold: "+prob);
		for(AggSearchResult r: results.values()){
			r.print();
		}
		
	}
	
	private static void singleAggQuery(IAtomContainer oQuery, MolStruct query, Cluster masterCluster, double thresh, double prob){
		results.get(oQuery).startAggTime();
		MolStruct rep = masterCluster.getRep();
		double matchingThresh = thresh;
		
		if( MCS.beatsOverlapThresholdIsoRank(query, rep, matchingThresh)){
			if( masterCluster.order() == 0){
				results.get(oQuery).addAggMatch(rep);
				results.get(oQuery).endAggTime();
				singleStandardQuery(oQuery, query, rep, thresh, prob);
				
			} else {
				for(Cluster subCluster: masterCluster.getMembers()){
					results.get(oQuery).endAggTime();
					singleAggQuery(oQuery,  query, subCluster, thresh, prob);
				}
			}
		}
	}
	
	private static void singleStandardQuery(IAtomContainer oQuery, MolStruct sQuery, MolStruct sTarget, double thresh, double prob){
		double sThresh = prob; // !!! not using the conversion I came up with, yet.
		if(MCS.beatsOverlapThresholdIsoRank(sQuery, sTarget, sThresh)){
			results.get(oQuery).addCoarseMatch(sTarget);
			for(String pubchemID: sTarget.getIDNums()){
				IAtomContainer target = db.getMolecule(pubchemID);
				int mcsSize = MCS.getSMSDOverlap(oQuery, target);
				if(thresh <= MCSUtils.overlapCoeff(mcsSize, oQuery, target)){
					results.get(oQuery).addMatch(target, mcsSize);
				}
			}
		}
		
	}

}
