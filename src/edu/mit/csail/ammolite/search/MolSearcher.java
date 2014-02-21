package edu.mit.csail.ammolite.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import edu.mit.csail.ammolite.compression.MoleculeStruct;
import edu.mit.csail.ammolite.database.StructDatabase;
import edu.mit.csail.fmcsj.MCS;

public class MolSearcher implements IMolSearcher {

	private StructDatabase db;
	private boolean useTanimoto;
	
	public MolSearcher( StructDatabase _db, boolean _useTanimoto) {
		db = _db;
		useTanimoto = _useTanimoto;
		
	}
	@Override
	public MolTriple[] search(IAtomContainer query, double threshold, double probability) {
		query = new AtomContainer(AtomContainerManipulator.removeHydrogens(query));
		double repThreshold = db.convertThreshold(threshold, probability, useTanimoto);
		edu.mit.csail.ammolite.Logger.debug("Using rep threshold of "+repThreshold);
		String[] repMatches = thresholdRepMatches( query, repThreshold);
		edu.mit.csail.ammolite.Logger.debug("Found "+repMatches.length+" representative matches");
		List<MolTriple> molMatches = thresholdMoleculeMatches( query, repMatches, threshold);
		edu.mit.csail.ammolite.Logger.debug("Found "+molMatches.size()+" molecule matches");
		return molMatches.toArray(new MolTriple[0]);
	}

	private List<MolTriple> thresholdMoleculeMatches( IAtomContainer query, String[]  targetIDs, double threshold){
		IAtomContainer target;
		List<MolTriple> matches = new ArrayList<MolTriple>();
		
		for(String id: targetIDs){
			target = db.getMolecule(id);
			target = new AtomContainer(AtomContainerManipulator.removeHydrogens(target));
			MCS myMCS = new MCS(query,target);
			myMCS.calculate();
			
			if(coeff( myMCS.size(), myMCS.compoundOne.getAtomCount(), myMCS.compoundTwo.getAtomCount()) > threshold){
				matches.add( new MolTriple( myMCS.getSolutions(), query, target));
				
			}
		}
		
		return matches;
	}
	
	/**
	 * Returns all representative matches above a certain given threshold
	 * 
	 * @param query
	 * @param reprThreshold
	 * @return
	 */
	private String[] thresholdRepMatches(IAtomContainer query, Double reprThreshold){
		MoleculeStruct sQuery = db.makeMoleculeStruct(query);

		List<MoleculeStruct> matches = new ArrayList<MoleculeStruct>();
		Iterator<MoleculeStruct> structs = db.iterator();
		
		MoleculeStruct target;
		while( structs.hasNext() ){
			
			target = structs.next();

			
			MCS myMCS = new MCS(sQuery,target);
			myMCS.calculate();
			
			double coef = coeff(myMCS.size(), myMCS.compoundOne.getAtomCount(), myMCS.compoundTwo.getAtomCount());
			
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
	
	private double coeff(int overlap, int a, int b){
		if( useTanimoto){
			return Util.tanimotoCoeff(overlap, a, b);
		}
		return Util.overlapCoeff(overlap, a, b);
		
	}
}
