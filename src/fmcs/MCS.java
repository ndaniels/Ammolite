package fmcs;

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import speedysearch.MoleculeStruct;

public class MCS {
	
	public enum MatchType {
		DEFAULT,
		AROMATICITY_SENSITIVE,
		RING_SENSITIVE
	}
	
	public enum RunningMode{
		FAST,
		DETAIL
	}
	
	private boolean haveBeenSwapped;
	private int timeout;
	private boolean isTimeout;
	private RunningMode runningMode;
	private boolean identicalGraph;
	private int bestSize;
	private double timeUsed;
	private MCSList<MCSMap> bestList;
	private MCSMap currentMapping;
	private MatchType matchType;
	private int userDefinedLowerBound;
	private int substructueNumLimit;
	private int currSubstructureNum;
	private int atomMismatchUpperBound;
	private int bondMismatchUpperBound;
	private int atomMismatchLowerBound;
	private int bondMismatchLowerBound;
	private int atomMismatchCurr;
	private int bondMismatchCurr;
	private boolean strictRingMatch;
	private MoleculeStruct compoundOne;
	private MoleculeStruct compoundTwo;
	boolean timeoutStop = false;
	boolean introducedNewComponent;
	private int bondMisCount;
	
	
	public MCS(	MoleculeStruct _compoundOne, MoleculeStruct _compoundTwo, 
				int _userDefinedLowerBound, int _substructureNumLimit,
				int _bondMismatchLower, int _bondMismatchUpper,
				MatchType _matchType, RunningMode _runningMode, int _timeout){
		
		userDefinedLowerBound = _userDefinedLowerBound;
		substructueNumLimit = _substructureNumLimit;
		bondMismatchLowerBound = _bondMismatchLower;
		bondMismatchUpperBound = _bondMismatchUpper;
		matchType = _matchType;
		runningMode = _runningMode;
		timeout = _timeout;
		
		atomMismatchCurr = 0;
		bondMismatchCurr = 0;
		currSubstructureNum = 0;
		timeUsed = 0.0;
		bestSize = 0;
		identicalGraph = false;
		isTimeout = false;
		

		if( _compoundOne.getAtomCount() < _compoundTwo.getAtomCount() ){
			compoundOne = _compoundOne;
			compoundTwo = _compoundTwo;
		} else {
			compoundOne = _compoundTwo;
			compoundTwo = _compoundOne;
			haveBeenSwapped = true;
		}
		
		
		
	}
	

	public double getTime(){
		return timeUsed;
	}
	
	public void calculate(){
		clearResult();
		double start = System.currentTimeMillis();
		if( compoundOne.equals(compoundTwo)){
			identicalGraph = true;
			if( runningMode == RunningMode.DETAIL){
				// TODO: lines 90 - 118 in MCS.cpp
			} 	
		} else {
			max();
		}
		
		double end = System.currentTimeMillis();
		timeUsed = end - start;
		
		if( runningMode == runningMode.DETAIL){
			// TODO: lines 127-176 MCS.cpp
		}
		
	}
	
	public int size(){
		
		if (identicalGraph) {
            return compoundOne.getAtomCount();
        } else if (runningMode == RunningMode.FAST) {
            return bestSize;
		} else if (runningMode == RunningMode.DETAIL){ 
            return bestList.get(0).size();
        }
		
		return -1;
	}
	
	public MoleculeStruct getCompoundOne(){
		if( haveBeenSwapped ){
			return compoundTwo;
		} else{
			return compoundOne;
		}
	}
	
	public MoleculeStruct getCompoundTwo(){
		if( haveBeenSwapped ){
			return compoundOne;
		} else{
			return compoundTwo;
		}
	}
	
	public void clearResult(){
		 bestSize = 0;
	     bestList.clear();
	        
        identicalGraph = false;
        currentMapping.clear();
	        
        timeoutStop = false;
        isTimeout = false;
	}
	
	public boolean isTimeout(){
		return isTimeout;
	}
	
	private MCS( MCS m){
		
	}
	
	private void max(){
		MCSList<IAtom> atomListOne = new MCSList<IAtom>();
		for(IAtom a: compoundOne.atoms()){
			atomListOne.push(a);
		}
		MCSList<IAtom> atomListTwo = new MCSList<IAtom>();
		for(IAtom a: compoundTwo.atoms()){
			atomListTwo.push(a);
		}
		grow(atomListOne, atomListTwo);
	}
	
	private boolean compatible(IAtom atom1, IAtom atom2){
		MCSList<IAtom> targetNeighborMapping = new MCSList<IAtom>();
		
		MCSList<IAtom> atomOneNeighborList = new MCSList<IAtom>( compoundOne.getConnectedAtomsList( atom1 ));
		
		for(int i=0; i<atomOneNeighborList.size(); i++){
			IAtom atom = atomOneNeighborList.get(i);
			if(currentMapping.containsKey( atom )){
				targetNeighborMapping.push(atom);
			}
		}
		
		MCSList<IAtom> currNeighborMapping = new MCSList<IAtom>();
		MCSList<IAtom> atomTwoNeighborList = new MCSList<IAtom>( compoundTwo.getConnectedAtomsList( atom2 ));
		for(int i=0; i<atomTwoNeighborList.size(); i++){
			IAtom k = currentMapping.getKey( atomTwoNeighborList.get(i));
			if(k != null){
				currNeighborMapping.push(k);
			}
		}
		if(!targetNeighborMapping.equals(currNeighborMapping)){ // TODO: could be an issue
			return false;
		}
		if( targetNeighborMapping.size() == 0){
			introducedNewComponent = true;// This could get tricky
		}
		if( matchType == MatchType.DEFAULT){
			for(int i=0; i<targetNeighborMapping.size(); i++){
				IAtom n = currentMapping.getVal( targetNeighborMapping.get(i));
				IBond bondOne = compoundOne.getBond(atom1, targetNeighborMapping.get(i));
				IBond bondTwo = compoundTwo.getBond(atom2, n);
				if( bondOne.getOrder() != bondTwo.getOrder() ){
					bondMisCount++;
				}
				
			}
		} else if ( matchType == MatchType.AROMATICITY_SENSITIVE){
			// TODO
		} else if ( matchType == MatchType.RING_SENSITIVE ){
			// TODO
		}
		
		return true;
		
	}
	
	private IAtom top(MCSList<IAtom> atomList){
		IAtom bestCandidateAtom = atomList.get(0);
		IAtom candidateAtom = null;
		int candidateIdx = -1;
		int bestIdx = -1;
		for(int i=0; i<atomList.size(); i++){
			if( compoundOne.getConnectedBondsCount(compoundOne.getAtom(i)) > compoundOne.getConnectedBondsCount(bestCandidateAtom)){
				bestCandidateAtom = compoundOne.getAtom(i);
				bestIdx = i;
			}
			MCSList<IAtom> neighborAtomList = new MCSList( compoundOne.getConnectedAtomsList(compoundOne.getAtom(i)));
			for(int j=0; j<neighborAtomList.size(); j++){
				if( currentMapping.containsKey(neighborAtomList.get(j))){
					if(		( candidateAtom == null ) || 
							( compoundOne.getConnectedBondsCount(compoundOne.getAtom(i)) > compoundOne.getConnectedBondsCount(candidateAtom))){
						
						candidateAtom = compoundOne.getAtom(i);
						candidateIdx = i;
					}
				}
			}
		}
		if( candidateAtom == null ){
			atomList.remove(bestIdx);
			return bestCandidateAtom;
		}
		if( candidateAtom != null ){
			atomList.remove(candidateIdx);
		}
		return candidateAtom;
	}
	
	private void boundary(){
        if (runningMode == RunningMode.FAST) {
            if (currentMapping.size() > bestSize) {
                
                if (atomMismatchCurr < atomMismatchLowerBound || bondMismatchCurr < bondMismatchLowerBound) {
                    return;
                }
                bestSize = currentMapping.size();
            }
        } else if (runningMode == RunningMode.DETAIL){
            if (currentMapping.size() == size() ) {
                if (atomMismatchCurr < atomMismatchLowerBound || bondMismatchCurr < bondMismatchLowerBound) {
                    return;
                }
                bestList.push(currentMapping);
            } else if (currentMapping.size() > size()) {
                
                if (atomMismatchCurr < atomMismatchLowerBound || bondMismatchCurr < bondMismatchLowerBound) {
                    return;
                }
                bestList.clear();
                bestList.push(currentMapping);
            }
        }
	}
	
	private void grow(MCSList v1_list, MCSList v2_list){
		
	}
}
