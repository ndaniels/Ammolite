package fmcs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import speedysearch.MoleculeStruct;

public class MCS {
	
	public enum MatchType {
		DEFAULT, 
		// AROMATICITY_SENSITIVE, // Unsupported
		// RING_SENSITIVE // Unsupported
	}
	
	private int timeout;
	private boolean identicalGraph;
	public MCSList<MCSMap> bestList = new MCSList<MCSMap>();
	public MCSMap currentMapping = new MCSMap();
	private MatchType matchType;
	private int userDefinedLowerBound;
	private int substructureNumLimit;
	private int currSubstructureNum;
	private int atomMismatchUpperBound = 1;
	private int bondMismatchUpperBound = 1;
	private int atomMismatchCurr;
	private int bondMismatchCurr;
	public IAtomContainer compoundOne;
	public IAtomContainer compoundTwo;
	boolean timeoutStop = false;
	boolean introducedNewComponent;
	private int bondMisCount;
	private double start_time;

	/**
	 * Default constructor
	 * 
	 * @param _compoundOne
	 * @param _compoundTwo
	 */
	public MCS(IAtomContainer _compoundOne, IAtomContainer _compoundTwo ){
		this(	_compoundOne ,_compoundTwo, 
				0, 10, 1, 
				MCS.MatchType.DEFAULT, 1000);
	}
	
	/**
	 * Detailed Constructor
	 * 
	 * @param _compoundOne
	 * @param _compoundTwo
	 * @param _userDefinedLowerBound
	 * @param _substructureNumLimit
	 * @param _bondMismatchLower
	 * @param _bondMismatchUpper
	 * @param _matchType
	 * @param _timeout
	 */
 	public MCS(	IAtomContainer _compoundOne, IAtomContainer _compoundTwo, 
				int _userDefinedLowerBound, int _substructureNumLimit, int _bondMismatchUpper,
				MatchType _matchType, int _timeout){
		
		userDefinedLowerBound = _userDefinedLowerBound;
		substructureNumLimit = _substructureNumLimit;
		bondMismatchUpperBound = _bondMismatchUpper;
		matchType = _matchType;
		timeout = _timeout;
		
		atomMismatchCurr = 0;
		bondMismatchCurr = 0;
		currSubstructureNum = 0;
		identicalGraph = false;
		
		// Compound One is the smaller of the two compounds, by definition.
		if( _compoundOne.getAtomCount() < _compoundTwo.getAtomCount() ){
			compoundOne = _compoundOne;
			compoundTwo = _compoundTwo;
		} else {
			compoundOne = _compoundTwo;
			compoundTwo = _compoundOne;
		}
		
	}


	public void calculate(){
		Logger.log("Starting MCS");
		clearResult();
		
		start_time = System.currentTimeMillis();
		
		if( compoundOne.equals(compoundTwo)){
			Logger.log("Identical graphs");
			identicalGraph = true;	
		} else {
			max();
		}
		
	}
	
	/**
	 * @return the size of the best match so far
	 */
	public int size(){
		
		if (identicalGraph) {
            return compoundOne.getAtomCount();
        } else {
			if( bestList.size() > 0){
				return bestList.get(0).size();
			}
        }
		
		return -1;
	}
	
	/**
	 * Straightforward. Resets the class.
	 */
	public void clearResult(){
	     bestList.clear();
	        
        identicalGraph = false;
        currentMapping.clear();
	        
        timeoutStop = false;
	}
	
	
	/**
	 * Convert IAtomContainers into lists and then call grow.
	 * 
	 * TODO: this is very C++
	 */
	private void max(){
		Logger.log("Max");
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
	
	/**
	 * Checks if two atoms match. Just compares type but could do other stuff.
	 * @param a
	 * @param b
	 * @return
	 */
	private boolean match(IAtom a, IAtom b){
		return a.getAtomicNumber() == b.getAtomicNumber();
	}
	
	/**
	 * Checks whether a given pair of atoms are compatible by seeing if they share 
	 * the same neighboring atoms. Does not penalize atoms for having different bond
	 * types than their counterpart but does keep track of this.
	 * 
	 * TODO: check how IAtom equality works
	 * 
	 * @param atom1
	 * @param atom2
	 * @return whether the atoms are compatible
	 */
	private boolean compatible(IAtom atom1, IAtom atom2){
		Logger.log("compatible");
		MCSList<IAtom> targetNeighborMapping = new MCSList<IAtom>();
		MCSList<IAtom> atomOneNeighborList = new MCSList<IAtom>( compoundOne.getConnectedAtomsList( atom1 ));
		for(IAtom atom1Neighbor: atomOneNeighborList){
			
			if( currentMapping.containsKey( atom1Neighbor ) ){
				targetNeighborMapping.push(atom1Neighbor);
			}
		}
		
		MCSList<IAtom> currNeighborMapping = new MCSList<IAtom>();

		MCSList<IAtom> atomTwoNeighborList = new MCSList<IAtom>( compoundTwo.getConnectedAtomsList( atom2 )); 
		for(IAtom atom2N: atomTwoNeighborList){

			
			if( currentMapping.containsValue( atom2N ) ){
				IAtom k = currentMapping.getKey( atom2N );
				currNeighborMapping.push(k);
			}
		}
		
		boolean targetMapEqualsCurrMap = true;
		for(IAtom t: targetNeighborMapping){
			targetMapEqualsCurrMap &= currNeighborMapping.contains(t);
		}

		
		if(!targetMapEqualsCurrMap){ 
			return false;
			
		} else if( targetNeighborMapping.size() == 0){// Trivial compatibility 
			return true;
			
		}
			
		// Count how many bonds are not the same order between the two atoms
		// We already know they have the same neighbors.
		for(IAtom target: targetNeighborMapping){
			
			IAtom counterpart = currentMapping.getVal( target );

			IBond bondOne = compoundOne.getBond(atom1, target);
			IBond bondTwo = compoundTwo.getBond(atom2, counterpart );
			
			if( bondOne.getOrder() != bondTwo.getOrder() ){
				bondMisCount++;
			}
			
		}
			
		return true;
		
	}
	
	/**
	 * Selectively pops the atom with the most potential connections with the current structure
	 * @param atomList
	 * @return the candidate
	 */
	private IAtom top(MCSList<IAtom> atomList){

		IAtom bestCandidateAtom = atomList.get(0);
		IAtom candidateAtom = null;
		IAtom nextContender;
		
		for(int i=0; i<atomList.size(); i++){
			nextContender = atomList.get(i);
			// sets the 'best' candidate as the one with the most bonds in the smaller molecule
			if( compoundOne.getConnectedBondsCount(nextContender) > compoundOne.getConnectedBondsCount(bestCandidateAtom)){
				bestCandidateAtom = atomList.get(i);
			}
			
			MCSList<IAtom> neighborAtomList = new MCSList<IAtom>( compoundOne.getConnectedAtomsList(nextContender));
			for(int j=0; j<neighborAtomList.size(); j++){
				// The real candidate is the one with the most bonds that has a neighbor in the current mapping
				if( currentMapping.containsKey(neighborAtomList.get(j))){
					if(		( candidateAtom == null ) || 
							( compoundOne.getConnectedBondsCount(nextContender) > compoundOne.getConnectedBondsCount(candidateAtom))){

						candidateAtom = nextContender;
					}
				}
			}
		}
		// Really the 'best' candidate is only used if we don't have anything else
		if( candidateAtom == null ){

			atomList.remove(bestCandidateAtom);
			return bestCandidateAtom;
		}
		atomList.remove(candidateAtom);
		return candidateAtom;
	}
	
	/**
	 * Checks the current solution to see if it represents a best solution
	 * @modifies bestList
	 */
	private void boundary(){
        if (currentMapping.size() == size() ) {
        	
            bestList.push(currentMapping.deepCopy());
            
        } else if (currentMapping.size() > size()) {
        	
            bestList.clear();
            bestList.push(currentMapping.deepCopy()); // TODO: deepcopy?
        }
	}
	
	/**
	 * Converts the best list into a set of IAtomContainers
	 * 
	 * TODO: Currently only returns solutions of size 1. Probably not this function though. 
	 * 
	 * @return
	 */
	public List<IAtomContainer> getSolutions(){
		
		ArrayList<IAtomContainer> out = new ArrayList<IAtomContainer>(bestList.size());

		for(MCSMap solution: bestList){

			Set<IAtom> atoms = solution.keySet();
						
			IAtomContainer thisSol = new AtomContainer();
			for(IAtom atom: atoms){
				thisSol.addAtom(atom);
			}
			
			for(IAtom atom: atoms){
				Iterator<IBond> bonds = compoundOne.getConnectedBondsList(atom).iterator();
				
				for(IBond bond=bonds.next(); bonds.hasNext(); bond=bonds.next()){
					thisSol.addBond(bond);
				}
				
			}
			
			out.add(thisSol);
		}
		return out;
	}
	
	/**
	 * The core of the fmcs algorithm. Explores likely substructure branches.
	 * 
	 * @param atomListOne
	 * @param atomListTwo
	 */
	private void grow(MCSList<IAtom> atomListOne, MCSList<IAtom> atomListTwo){
		Logger.log("Grow");
		
		MCSList<IAtom> atomListOneCopy = new MCSList<IAtom>( atomListOne );
		MCSList<IAtom> atomListTwoCopy = new MCSList<IAtom>( atomListTwo );
		MCSList<Integer> atomListOneDegrees = new MCSList<Integer>();
		MCSList<Integer> atomListTwoDegrees = new MCSList<Integer>();
		
		// For every atom in list one makes a corresponding list of their potential degree in the current mapping
		Logger.log("Going through " +atomListOne.size()+" atoms in list one");
		for( IAtom atom: atomListOne){
			
			if(!currentMapping.containsKey( atom )){
				Logger.log("IAtom " + atom + " is not in current mapping",3);
				
				int degree = 0;
				
				for(IAtom neighbor: compoundOne.getConnectedAtomsList(atom)){

					if(currentMapping.containsKey(neighbor)){
						degree++;
						Logger.log("IAtom " + atom + " has "+degree+" neighbors in the current mapping",3);
					}
				}

				atomListOneDegrees.push(degree);	
			}
		}
		
		// For every atom in list two makes a corresponding list of their potential degree in the current mapping
		Logger.log("Going through " +atomListTwo.size()+" atoms in list two");
		for( IAtom atom: atomListTwo){
			
			if(!currentMapping.containsKey( atom )){
				Logger.log("IAtom " + atom + " is not in current mapping",3);
				int degree = 0;
				
				for(IAtom neighbor: compoundTwo.getConnectedAtomsList(atom)){
					if(currentMapping.containsKey(neighbor)){
						degree++;
						Logger.log("IAtom " + atom + " has "+degree+" neighbors in the current mapping",3);
					}
				}

				atomListTwoDegrees.push(degree);	
			}
		}
		
		// Check how many atoms in list one and two share the same degree to establish an upper bound
		int currentBound = currentMapping.size();
		for(int degree:atomListOneDegrees){
			if(atomListTwoDegrees.contains(degree)){
				Logger.log("Both lists have at least one atom with "+degree+" neighbors in the current mapping",2);
				currentBound++;
				atomListTwoDegrees.remove(degree);
			}
		}
		
		// Throw out anything that's too little
        if(currentBound < userDefinedLowerBound || currentBound < size()) {
        	Logger.log("Our current upper bound of "+currentBound+" is too little, aborting");
            return;
        }
		
		
		while( true ){

			// End conditions for the loop
			// if ( System.currentTimeMillis() - start_time > this.timeout){
			// 	boundary();
			// 	Logger.log("Timed out. Ending.");
			// 	return;
			// } else 

			if (atomListOneCopy.isEmpty() || atomListTwoCopy.isEmpty()) { 
                boundary();
                Logger.log("One or both lists of atoms is empty. Ending.");
                return;
            }
            
			
            IAtom topCandidateAtom = top(atomListOneCopy);
    		for( IAtom otherAtom: atomListTwoCopy){
    			
                boolean atomMismatched = false;
                boolean atomMismatchAllowed = true;
                
                if (!topCandidateAtom.getAtomicNumber().equals( otherAtom.getAtomicNumber())){

                	Logger.log("Atoms mismatched. " + topCandidateAtom.getAtomicNumber()+" and "+otherAtom.getAtomicNumber());
                    ++atomMismatchCurr;
                    atomMismatched = true;
                }
                
                // If we can still have an atom mismatch
                if ((!(atomMismatchCurr > atomMismatchUpperBound) && atomMismatchAllowed)) {
                	
                	Logger.log("Allowing atom mismatch");
                    bondMisCount = 0;

                    
                    if ( introducedNewComponent = compatible(topCandidateAtom, otherAtom) ) {
                    	
                        if (!(bondMismatchCurr + bondMisCount > bondMismatchUpperBound)) {
                            
                            bondMismatchCurr = bondMismatchCurr + bondMisCount;
                            
                            if (introducedNewComponent) {
                                ++currSubstructureNum;
                            }

                            /**
                             * This is where the algorithm gets a bit tricky. The recursive call to
                             * grow explores all the possible substructures that include the atom we 
                             * just deemed compatible. The eventual pop can then explore substructures 
                             * that may not contain the given pair of atoms.
                             */
                            if ( !(currSubstructureNum > substructureNumLimit) ) {
                    			
                                currentMapping.push(topCandidateAtom, otherAtom);
                                                   			
                                atomListTwo.remove(otherAtom);

                                grow(atomListOneCopy, atomListTwo);
                             
                                atomListTwo.push(otherAtom);
                                currentMapping.pop();                      

                            }
                            
                            if (introducedNewComponent) {
                                --currSubstructureNum;
                            }
                            
                            bondMismatchCurr = bondMismatchCurr - bondMisCount;
                        }
                    }
                    
                }
                if (atomMismatched) {
                    --atomMismatchCurr;
                }
            }
		}
		
		
	}
}
