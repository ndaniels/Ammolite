package edu.mit.csail.fmcsj;

import java.util.ArrayList;
import java.util.List;

import edu.mit.csail.ammolite.Logger;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

public class MCS {
	
	public enum MatchType {
		DEFAULT, 
		// AROMATICITY_SENSITIVE, // Unsupported
		// RING_SENSITIVE // Unsupported
	}
	
	private boolean identicalGraph;
	public MCSList<MCSMap> bestList = new MCSList<MCSMap>();
	public MCSMap currentMapping = new MCSMap();
	private int userDefinedLowerBound;
	private int substructureNumLimit;
	private int currSubstructureNum;
	private int atomMismatchUpperBound = 1;
	private int bondMismatchUpperBound = 1;
	private int atomMismatchCurr;
	private int bondMismatchCurr;
	private int solutionSize = -1;
	public IAtomContainer compoundOne;
	public IAtomContainer compoundTwo;
	boolean timeoutStop = false;

	/**
	 * Default constructor
	 * 
	 * @param _compoundOne
	 * @param _compoundTwo
	 */
	public MCS(IAtomContainer _compoundOne, IAtomContainer _compoundTwo ){
		this(	_compoundOne ,_compoundTwo, 0, 1);
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
				int _userDefinedLowerBound, int _substructureNumLimit){
		
		userDefinedLowerBound = _userDefinedLowerBound;
		substructureNumLimit = _substructureNumLimit;

		
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

 	/**
 	 * Calculates the Maximal Common Subgraph
 	 * 
 	 * @return the running time in milliseconds
 	 */
	public long calculate(){
		long startTime = System.currentTimeMillis();
		
		clearResult();
		
		if( compoundOne == compoundTwo){
			Logger.debug("Identical Graphs");
			identicalGraph = true;	
		} else {
			max();
		}
		return System.currentTimeMillis() - startTime;
		
	}
	
	/**
	 * @return the size of the best match so far
	 */
	public int size(){
		
		if (identicalGraph) {
            return compoundOne.getAtomCount();
        } else {
			return solutionSize;
        }
	
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
	private CompatibleReturn compatible(IAtom atom1, IAtom atom2){
		
		CompatibleReturn out = new CompatibleReturn();
		
		MCSList<IAtom> targetNeighborMapping = new MCSList<IAtom>();
		MCSList<IAtom> atomOneNeighborList 
			= new MCSList<IAtom>( compoundOne.getConnectedAtomsList( atom1 ));
		
		for(IAtom atom1Neighbor: atomOneNeighborList){
			
			if( currentMapping.containsKey( atom1Neighbor ) ){
				targetNeighborMapping.push(atom1Neighbor);
			}
		}
		
		MCSList<IAtom> currNeighborMapping = new MCSList<IAtom>();
		MCSList<IAtom> atomTwoNeighborList 
			= new MCSList<IAtom>( compoundTwo.getConnectedAtomsList( atom2 )); 
		
		for(IAtom atom2N: atomTwoNeighborList){

			
			if( currentMapping.containsVal( atom2N ) ){
				IAtom k = currentMapping.getKey( atom2N );
				currNeighborMapping.push(k);
			}
		}
		

		if (!targetNeighborMapping.equals(currNeighborMapping)) {
            return out;
        } else {
        	out.compatible = true;
        }
		
		if( targetNeighborMapping.size() == 0){
			out.introducedNewComponent = true;
			////edu.mit.csail.ammolite.Logger.debug("Trying to introduce a new component");
		}
			
		// Count how many bonds are not the same order between the two atoms
		// We already know they have the same neighbors.
		for(IAtom target: targetNeighborMapping){
			
			IAtom counterpart = currentMapping.getVal( target );

			IBond bondOne = compoundOne.getBond(atom1, target);
			IBond bondTwo = compoundTwo.getBond(atom2, counterpart );
			
			if( bondOne.getOrder() != bondTwo.getOrder() ){
				out.bondMisCount++;
			}
			
		}
			
		return out;
		
	}
	
	/**
	 * Selectively pops the atom with the most potential connections with the current 
	 * structure
	 * @param atomList
	 * @return the candidate
	 */
	private IAtom top(MCSList<IAtom> atomList){

		IAtom bestCandidateAtom = atomList.get(0);
		IAtom candidateAtom = null;
		
		for(int i=0; i<atomList.size(); i++){
			IAtom nextContender = atomList.get(i);
			// sets the 'best' candidate as the one with the most bonds in the smaller molecule
			int bondsToNextContender = compoundOne.getConnectedBondsCount(nextContender);
			int bondsToBestCandidate = compoundOne.getConnectedBondsCount(bestCandidateAtom);
			
			if( bondsToNextContender > bondsToBestCandidate){
				bestCandidateAtom = nextContender;
			}
			
			MCSList<IAtom> neighborAtomList 
				= new MCSList<IAtom>( compoundOne.getConnectedAtomsList(nextContender));
			for(int j=0; j<neighborAtomList.size(); j++){
				// The real candidate is the one with the most bonds that has a neighbor in the 
				// current mapping
				if( currentMapping.containsKey(neighborAtomList.get(j))){
					if(		( candidateAtom == null ) || 
							( bondsToNextContender > bondsToBestCandidate)){

						candidateAtom = nextContender;
						break;
					}
				}
			}
		}
		// Really the 'best' candidate is only used if we don't have anything else
		if( candidateAtom == null ){

			atomList.remove(bestCandidateAtom);
			return bestCandidateAtom;
		} else {
			atomList.remove(candidateAtom);
		}
		return candidateAtom;
	}
	
	/**
	 * Checks the current solution to see if it represents a best solution
	 * @modifies bestList
	 */
	private void boundary(){
        if (currentMapping.size() == size() ) {
        	
            bestList.push(currentMapping.copy());
            ////edu.mit.csail.ammolite.Logger.debug("Adding a solution of size "+currentMapping.size()+" vs "+size());
        } else if (currentMapping.size() > size()) {
        	solutionSize = currentMapping.size();
            bestList.clear();
            bestList.push(currentMapping.copy()); // TODO: deepcopy?
            ////edu.mit.csail.ammolite.Logger.debug("Better solution of size "+currentMapping.size());

        }
	}
	
	/**
	 * Converts the best list into a set of IAtomContainers
	 * 
	 * @return
	 */
	public List<IAtomContainer> getSolutions(){
		
		////edu.mit.csail.ammolite.Logger.debug("Found "+bestList.size()+" solutions of size "+size());
		ArrayList<IAtomContainer> out = new ArrayList<IAtomContainer>(bestList.size());
		if(identicalGraph){
			out.add(compoundOne);
			return out;
		}

		for(MCSMap map: bestList){
			if(map.size() != size()){
				////edu.mit.csail.ammolite.Logger.debug("!!!");
			}
			IAtomContainer keySol = new AtomContainer();
			IAtomContainer valSol = new AtomContainer();
			for(IAtom atom: map.getKeyList()){
				keySol.addAtom(atom);
			}
			for(IAtom atom: map.getValList()){
				valSol.addAtom(atom);
			}
			
			for(int i=0; i<keySol.getAtomCount(); ++i){
				for(int j=0; j<i; ++j){
					IBond b = compoundOne.getBond(keySol.getAtom(i), keySol.getAtom(j));
					if( b != null){
						keySol.addBond(b);
					}
				}
			}
			
			for(int i=0; i<valSol.getAtomCount(); ++i){
				for(int j=0; j<i; ++j){
					IBond b = compoundTwo.getBond(valSol.getAtom(i), valSol.getAtom(j));
					if( b != null){
						valSol.addBond(b);
					}
				}
			}
			
			out.add(keySol);
			out.add(valSol);
		}
		return out;
	}
	
	private void grow(MCSList<IAtom> atomListOne, MCSList<IAtom> atomListTwo){
		grow(atomListOne, atomListTwo, 0);
	}
	
	/**
	 * The core of the fmcs algorithm. Explores likely substructure branches.
	 * 
	 * @param atomListOne
	 * @param atomListTwo
	 */
	private void grow(MCSList<IAtom> atomListOne, MCSList<IAtom> atomListTwo, int indLevel){
		MCSList<IAtom> atomListOneCopy = new MCSList<IAtom>( atomListOne );
		MCSList<IAtom> atomListTwoCopy = new MCSList<IAtom>( atomListTwo );
		MCSList<Integer> atomListOneDegrees = new MCSList<Integer>();
		MCSList<Integer> atomListTwoDegrees = new MCSList<Integer>();
		
		// For every atom in list one makes a corresponding list of their potential degree in the 
		// current mapping
		for( IAtom atom: atomListOne){
			
			if(!currentMapping.containsKey( atom )){
				int degree = 0;
				
				for(IAtom neighbor: compoundOne.getConnectedAtomsList(atom)){

					if(currentMapping.containsKey(neighbor)){
						degree++;
					}
				}

				atomListOneDegrees.push(degree);	
			}
		}
		
		// For every atom in list two makes a corresponding list of their potential degree in the 
		// current mapping
		for( IAtom atom: atomListTwo){
			
			if(!currentMapping.containsKey( atom )){
				int degree = 0;
				
				for(IAtom neighbor: compoundTwo.getConnectedAtomsList(atom)){
					if(currentMapping.containsKey(neighbor)){
						degree++;
					}
				}

				atomListTwoDegrees.push(degree);	
			}
		}
		
		// Check how many atoms in list one and two share the same degree to establish an upper 
		// bound
		int currentBound = currentMapping.size();
		for(int degree:atomListOneDegrees){
			if(atomListTwoDegrees.contains(degree)){
				currentBound++;
				atomListTwoDegrees.remove(degree);
			}
		}
		
		// Throw out anything that's too little
        if(currentBound < userDefinedLowerBound || currentBound < size()) {
            return;
        }
		
		
		while( true ){

			if (atomListOneCopy.isEmpty() || atomListTwoCopy.isEmpty()) { 
                boundary();
                return;
            }
            
			
            IAtom topCandidateAtom = top(atomListOneCopy);
            //////edu.mit.csail.ammolite.Logger.debug("topCandidateAtom: "+topCandidateAtom);
    		for( IAtom otherAtom: atomListTwoCopy){
    			
                boolean atomMismatched = false;
                
                int atom1 = topCandidateAtom.getAtomicNumber();
                int atom2 = otherAtom.getAtomicNumber();
                
//                ////edu.mit.csail.ammolite.Logger.debug("atom1: "+atom1+" atom2: "+atom2);
                
                if ( atom1 != atom2){
                    ++atomMismatchCurr;
                    atomMismatched = true;
                }
                
                // If we can still have an atom mismatch
                boolean tooManyAtomMismatches = atomMismatchCurr > atomMismatchUpperBound;
                if ( !tooManyAtomMismatches) {
                	

                    CompatibleReturn compOut = compatible(topCandidateAtom, otherAtom);
                    
                    int bondMisCount = compOut.bondMisCount;
                    boolean introducedNewComponent = compOut.introducedNewComponent;
                    boolean foundCompatible = compOut.compatible;

                    if ( foundCompatible ) {

                    	
                    	//////edu.mit.csail.ammolite.Logger.debug("Currently there are "+bondMismatchCurr+" bond mismatches");
                    	boolean tooManyBondMismatches = bondMismatchCurr + bondMisCount > bondMismatchUpperBound;
                        if (!tooManyBondMismatches) {
                            
                            bondMismatchCurr = bondMismatchCurr + bondMisCount;
                            
                            if (introducedNewComponent) {
                                ++currSubstructureNum;
                            }

                            /**
                             * This is where the algorithm gets a bit tricky. The recursive call to
                             * grow explores all the possible substructures that include the atom we 
                             * just deemed compatible. The eventual pop can then explore 
                             * substructures that may not contain the given pair of atoms.
                             */
                            //////edu.mit.csail.ammolite.Logger.debug("Currently there are "+currSubstructureNum+" substructures");
                            boolean aboveBound = currSubstructureNum > substructureNumLimit;
                            if ( !aboveBound ) {
                            	//////edu.mit.csail.ammolite.Logger.debug("Did introduce a new component, now there are "+currSubstructureNum);
                    			
                                currentMapping.push(topCandidateAtom, otherAtom);        			
                                atomListTwo.remove(otherAtom);

                                grow(atomListOneCopy, atomListTwo, indLevel+1);
                             
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
            }// end for( IAtom otherAtom: atomListTwoCopy)
		}
		
		
	}
}
