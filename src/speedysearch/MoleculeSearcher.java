package speedysearch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import edu.ucla.sspace.graph.isomorphism.VF2IsomorphismTester;
import fmcs.MCS;

public class MoleculeSearcher {
	private MoleculeStructFactory structFactory;
	private StructDatabase db;
	private boolean useTanimoto;
	
	
	
	public MoleculeSearcher( StructDatabase _db){
		this(_db, false);
	}
	
	
	public MoleculeSearcher( StructDatabase _db, boolean _useTanimoto) {
		structFactory= _db.getMoleculeStructFactory();
		db = _db;
		useTanimoto = _useTanimoto;
		
	}
	
	/**
	 * Returns the matching ids of an exact representative match
	 * 
	 * @param query
	 * @return
	 */
	private String[] exactRepMatches(IAtomContainer query){
		MoleculeStruct sQuery = structFactory.makeMoleculeStruct(query);
		Iterator<MoleculeStruct> targets = db.getStructsByHash( sQuery.hashCode() ).iterator();
		VF2IsomorphismTester iso_tester = new VF2IsomorphismTester();
		MoleculeStruct t;
		while( targets.hasNext()){
			t = targets.next();
			if( sQuery.isIsomorphic(t, iso_tester)){
				return t.getIDNums();
			}
		}
		return null;
	}
	
	private String[] hashRepMatches(IAtomContainer query){
		MoleculeStruct sQuery = structFactory.makeMoleculeStruct(query);
		Iterator<MoleculeStruct> targets = db.getStructsByHash( sQuery.hashCode() ).iterator();
		List<MoleculeStruct> bestMatches = new ArrayList<MoleculeStruct>();
		int bestMatchAtomCount = 0;
		MoleculeStruct t;
		while( targets.hasNext()){
			t = targets.next();
			
			MCS myMCS = new MCS(sQuery,t);
			myMCS.calculate();
			
			if(myMCS.size() > bestMatchAtomCount){
				bestMatches.clear();
				bestMatches.add(t);
				bestMatchAtomCount = myMCS.size();
			} else if( myMCS.size() == bestMatchAtomCount){
				bestMatches.add(t);
			}
		}
		
		String[] ids = new String[ bestMatches.size()];
		
		for(int i=0;i<bestMatches.size(); i++){
			ids[i] = bestMatches.get(i).getID();
		}
		
		if( 1.0 * bestMatchAtomCount / sQuery.getAtomCount() < 0.8 ){ // Arbitrary threshold!
			return null;
		}
		
		return ids;
		
	}
	
	/**
	 * Returns the matching ids of the largest (by atom count) matching representatives of a query
	 * 
	 * @param query
	 * @return
	 */
	private String[] bestRepMatches(IAtomContainer query){
		MoleculeStruct sQuery = structFactory.makeMoleculeStruct(query);
		int bestMatchAtomCount = 0;
		List<MoleculeStruct> bestMatches = new ArrayList<MoleculeStruct>();
		Iterator<MoleculeStruct> structs = db.iterator();
		MoleculeStruct target;
		while( structs.hasNext() ){
			
			target = structs.next();
			
			MCS myMCS = new MCS(sQuery,target);
			myMCS.calculate();
			if(myMCS.size() > bestMatchAtomCount){
				bestMatches.clear();
				bestMatches.add(target);
				bestMatchAtomCount = myMCS.size();
			} else if( myMCS.size() == bestMatchAtomCount){
				bestMatches.add(target);
			}
		}
		
		String[] ids = new String[ bestMatches.size()];
		
		for(int i=0;i<bestMatches.size(); i++){
			ids[i] = bestMatches.get(i).getID();
		}
		
		return ids;
	}
	
	
	
	/**
	 * Returns all representative matches above a certain given threshold
	 * 
	 * @param query
	 * @param reprThreshold
	 * @return
	 */
	private String[] thresholdRepMatches(IAtomContainer query, Double reprThreshold){
		MoleculeStruct sQuery = structFactory.makeMoleculeStruct(query);

		List<MoleculeStruct> matches = new ArrayList<MoleculeStruct>();
		Iterator<MoleculeStruct> structs = db.iterator();
		
		MoleculeStruct target;
		while( structs.hasNext() ){
			
			target = structs.next();

			
			MCS myMCS = new MCS(sQuery,target);
			myMCS.calculate();
			
			Logger.log( "Size of MCS: "+myMCS.getSolutions().get(0).getAtomCount());
			
			double coef = coeff(myMCS.size(), myMCS.compoundOne.getAtomCount(), myMCS.compoundTwo.getAtomCount());
			
			Logger.log("Coeff: "+coef,4);
			if( coef >= reprThreshold){
				matches.add(target);
			} 
		}
		
		ArrayList<String> ids = new ArrayList<String>(3 * matches.size());
		
		for(int i=0;i<matches.size(); i++){
			for(String id: matches.get(i).getIDNums()){
				ids.add(id);
			}
		}
		
		return ids.toArray(new String[0]);
	}
	
	
	private MoleculeTriple[] bestMoleculeMatches(IAtomContainer query, String[] targetIDs){
		IAtomContainer target;
		int bestMatchAtomCount = 0;
		List<MoleculeTriple> bestMatches = new ArrayList<MoleculeTriple>();
		
		for(String id: targetIDs){
			target = db.getMolecule(id);
			target = new AtomContainer(AtomContainerManipulator.removeHydrogens(target));
			MCS myMCS = new MCS(query,target);
			myMCS.calculate();
			
			if(myMCS.size() > bestMatchAtomCount){
				bestMatches.clear();
				bestMatches.add( new MoleculeTriple( myMCS.getSolutions(), query, target));
				bestMatchAtomCount = myMCS.size();
				
			} else if( myMCS.size() == bestMatchAtomCount){
				bestMatches.add( new MoleculeTriple( myMCS.getSolutions(), query, target));
				
			}
		}
		
		return bestMatches.toArray(new MoleculeTriple[0]);
	}
	
	private MoleculeTriple[] thresholdMoleculeMatches( IAtomContainer query, String[]  targetIDs, double threshold){
		IAtomContainer target;
		List<MoleculeTriple> matches = new ArrayList<MoleculeTriple>();
		
		for(String id: targetIDs){
			target = db.getMolecule(id);
			target = new AtomContainer(AtomContainerManipulator.removeHydrogens(target));
			MCS myMCS = new MCS(query,target);
			myMCS.calculate();
			
			if(coeff( myMCS.size(), myMCS.compoundOne.getAtomCount(), myMCS.compoundTwo.getAtomCount()) > threshold){
				matches.add( new MoleculeTriple( myMCS.getSolutions(), query, target));
				
			}
		}
		
		return matches.toArray(new MoleculeTriple[0]);
	}
	
	public MoleculeTriple[] bigSearch(IAtomContainer query, double threshold){
		query = new AtomContainer(AtomContainerManipulator.removeHydrogens(query));
		Logger.log("Searching for matches to " + query.getID() + " with threshold "+threshold, 2);
		
		double repThresh = convertThresh( threshold);
		Logger.log("Threshold: "+threshold+" Representative Threshold: "+repThresh,2);
		String[] repMatches = thresholdRepMatches( query, repThresh);
		Logger.log("Found "+repMatches.length+" representative matches",2);
		MoleculeTriple[] molMatches = thresholdMoleculeMatches( query, repMatches, threshold);
		Logger.log("Found "+molMatches.length+" molecule matches",2);
		return molMatches;
	}
	
	/**
	 * Uses the built in compression structure to try and find a good match quickly
	 * 
	 * @param query
	 * @return
	 */
	public MoleculeTriple[] quickSearch(IAtomContainer query){
		query = new AtomContainer(AtomContainerManipulator.removeHydrogens(query));
		Logger.log("Searching for matches to " + query.getID(), 2);
		
		String[] repMatches = exactRepMatches( query);
		if( repMatches == null){
			repMatches = hashRepMatches( query);
		}
		if( repMatches == null){
			repMatches = bestRepMatches( query);
		}
		return bestMoleculeMatches(query, repMatches);	
	}
	
	private double coeff(int overlap, int a, int b){
		if( useTanimoto){
			return ( (double) overlap) / ( a + b - overlap);
		}
		
		if( a < b){
			return ( (double) overlap) / a;
		}
		return ( (double) overlap) / b;
	}
	
	private double convertThresh( double threshold){
		return 0.5 * threshold; // TODO: this... 0.5 is just made up
	}
}
