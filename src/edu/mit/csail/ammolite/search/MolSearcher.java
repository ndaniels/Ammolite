package edu.mit.csail.ammolite.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import edu.mit.csail.ammolite.compression.MoleculeStruct;
import edu.mit.csail.ammolite.database.StructDatabase;
import edu.mit.csail.fmcsj.AbstractMCS;
import edu.mit.csail.fmcsj.FMCS;

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
			FMCS myMCS = new FMCS(query,target);
			myMCS.calculate();
			
			if(coeff( myMCS.size(), myMCS.getCompoundOne().getAtomCount(), myMCS.getCompoundTwo().getAtomCount()) > threshold){
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
		long startTime = System.currentTimeMillis();
		long timeInMCS = 0L;
		MoleculeStruct sQuery = db.makeMoleculeStruct(query);

		List<MoleculeStruct> matches = new ArrayList<MoleculeStruct>();
		Iterator<IAtomContainer> structs = db.iterator();
		int count = 0;
		MoleculeStruct target;
		
		while( structs.hasNext() ){
			if( count % 50 == 0){
				edu.mit.csail.ammolite.Logger.debug("Scanned "+count+" representatives");
				edu.mit.csail.ammolite.Logger.debug("Working for "+(System.currentTimeMillis()-startTime)+" milliseconds total");
				edu.mit.csail.ammolite.Logger.debug("In MCS for "+timeInMCS+" milliseconds");
			}
			target = (MoleculeStruct) structs.next();

			boolean timeOut = false;
			FMCS myMCS = new FMCS(sQuery,target);
			try {
				timeInMCS += myMCS.calculate();
			} catch (TimeoutException e) {
				timeInMCS += AbstractMCS.getTimeoutMillis();
				timeOut = true;
			}
			if( !timeOut){
				double coef = coeff(myMCS.size(), myMCS.getCompoundOne().getAtomCount(), myMCS.getCompoundTwo().getAtomCount());
				
				if( coef >= reprThreshold){
					matches.add(target);
				} 
				count++;
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
