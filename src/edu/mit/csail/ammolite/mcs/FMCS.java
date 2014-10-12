package edu.mit.csail.ammolite.mcs;

import java.util.ArrayList;
import java.util.List;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import edu.mit.csail.ammolite.mcs.AbstractMCS;
import edu.mit.csail.ammolite.mcs.MCSList;
import edu.mit.csail.ammolite.mcs.MCSMap;

public class FMCS extends AbstractMCS{
	
	private boolean identicalGraph;
	public MCSList<MCSMap> bestList = new MCSList<MCSMap>();
	public MCSMap currentMapping = new MCSMap();
	private int userDefinedLowerBound = 0;
	private int substructureNumLimit = 1;
	private int currSubstructureNum;
	private int atomMismatchUpperBound = 0;
	private int bondMismatchUpperBound = 0;
	private int atomMismatchCurr;
	private int bondMismatchCurr;
	private int solutionSize = -1;
	boolean timeoutStop = false;

	/**
	 * Constructor
	 * 
	 * @param _compoundOne
	 * @param _compoundTwo
	 */
	public FMCS(IAtomContainer _compoundOne, IAtomContainer _compoundTwo) {
		super(_compoundOne, _compoundTwo);
		smallCompound = new FMCSAtomContainer( smallCompound);
		bigCompound = new FMCSAtomContainer( bigCompound);
 		boolean flexible = false;
 		if( flexible){
 			atomMismatchUpperBound = 1;
 			bondMismatchUpperBound = 1;
 		}

		atomMismatchCurr = 0;
		bondMismatchCurr = 0;
		currSubstructureNum = 0;
		identicalGraph = false;
	}


 	/**
 	 * Calculates the Maximal Common Subgraph
 	 * 
 	 * @return the running time in milliseconds
 	 */
	protected void myCalculate(){
		
		clearResult();
		
		if( smallCompound == bigCompound){
			
			identicalGraph = true;	
		} else {
			max();
		}
		
	}

	public void printMismatches(){
		IAtomContainer a = getSolutions().get(0);
		IAtomContainer b = getSolutions().get(0);
		int atomMis = 0;
		a.atoms();
	}
	
	/**
	 * @return the size of the best match so far
	 */
	protected int mySize(){
		
		if (identicalGraph) {
            return smallCompound.getAtomCount();
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
		for(IAtom a: smallCompound.atoms()){
			atomListOne.push(a);
		}
		MCSList<IAtom> atomListTwo = new MCSList<IAtom>();
		for(IAtom a: bigCompound.atoms()){
			atomListTwo.push(a);
		}
		grow(atomListOne, atomListTwo);
	}
	
	
	class CompatibleReturn {
		public boolean compatible = false;
		public boolean introducedNewComponent = false;
		public int bondMisCount = 0;
		
		public CompatibleReturn(){};
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
			= new MCSList<IAtom>( smallCompound.getConnectedAtomsList( atom1 ));
		
		for(IAtom atom1Neighbor: atomOneNeighborList){
			
			if( currentMapping.containsKey( atom1Neighbor ) ){
				targetNeighborMapping.push(atom1Neighbor);
			}
		}
		
		MCSList<IAtom> currNeighborMapping = new MCSList<IAtom>();
		MCSList<IAtom> atomTwoNeighborList 
			= new MCSList<IAtom>( bigCompound.getConnectedAtomsList( atom2 )); 
		
		for(IAtom atom2N: atomTwoNeighborList){
			if( currentMapping.containsVal( atom2N ) ){
				IAtom k = currentMapping.getKey( atom2N );
				currNeighborMapping.push(k);
			}
		}
		
		if(targetNeighborMapping.size() != currNeighborMapping.size()){
		    System.out.println("lengths do not match");
		    return out;
		}
		if (!targetNeighborMapping.equals(currNeighborMapping)) {
		    System.out.println("neighbours do not match");
            return out;
        } else {
        	out.compatible = true;
        }
		
		if( targetNeighborMapping.size() == 0){
			out.introducedNewComponent = true;
			
		}
			
		// Count how many bonds are not the same order between the two atoms
		// We already know they have the same neighbors.
		for(IAtom target: targetNeighborMapping){
			
			IAtom counterpart = currentMapping.getVal( target );

			IBond bondOne = smallCompound.getBond(atom1, target); // Slow
			IBond bondTwo = bigCompound.getBond(atom2, counterpart ); // Slow
			
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

		IAtom bestCandidateAtom = atomList.peek();
		IAtom candidateAtom = null;
		
		for(int i=0; i<atomList.size(); i++){
			IAtom nextContender = atomList.get(i);
			// sets the 'best' candidate as the one with the most bonds in the smaller molecule
			int bondsToNextContender = smallCompound.getConnectedBondsCount(nextContender);
			int bondsToBestCandidate = smallCompound.getConnectedBondsCount(bestCandidateAtom);
			
			if( bondsToNextContender > bondsToBestCandidate){
				bestCandidateAtom = nextContender;
			}
			
			MCSList<IAtom> neighborAtomList 
				= new MCSList<IAtom>( smallCompound.getConnectedAtomsList(nextContender));
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
           
        } else if (currentMapping.size() > size()) {
        	solutionSize = currentMapping.size();
            bestList.clear();
            bestList.push(currentMapping.copy()); // TODO: deepcopy?
            

        }
	}
	
	public IAtomContainer myGetFirstSolution(){
		return getSolutions().get(0);
	}
	
	/**
	 * Converts the best list into a set of IAtomContainers
	 * 
	 * @return
	 */
	public List<IAtomContainer> myGetSolutions(){
		
		
		ArrayList<IAtomContainer> out = new ArrayList<IAtomContainer>(bestList.size());
		if(identicalGraph){
			out.add(smallCompound);
			return out;
		}

		for(MCSMap map: bestList){
			if(map.size() != size()){
				
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
					IBond b = smallCompound.getBond(keySol.getAtom(i), keySol.getAtom(j));
					if( b != null){
						keySol.addBond(b);
					}
				}
			}
			
			for(int i=0; i<valSol.getAtomCount(); ++i){
				for(int j=0; j<i; ++j){
					IBond b = bigCompound.getBond(valSol.getAtom(i), valSol.getAtom(j));
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

		if(atomMismatchCurr > atomMismatchUpperBound){
			throw new RuntimeException("Too many atom mismatches");
		}
		if(bondMismatchCurr > bondMismatchUpperBound){
			throw new RuntimeException("Too many bond mismatches");
		}
		
		// For every atom in list one makes a corresponding list of their potential degree in the 
		// current mapping
		for( IAtom atom: atomListOne){
			
			if(!currentMapping.containsKey( atom )){
				int degree = 0;
				
				for(IAtom neighbor: smallCompound.getConnectedAtomsList(atom)){

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
				
				for(IAtom neighbor: bigCompound.getConnectedAtomsList(atom)){
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
            
    		for( IAtom otherAtom: atomListTwoCopy){
    			
                boolean atomMismatched = false;
                
                int atom1 = topCandidateAtom.getAtomicNumber();
                int atom2 = otherAtom.getAtomicNumber();
                

                
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
//                    System.out.println(foundCompatible);
                    if ( foundCompatible ) {

                    	
                    	
                    	boolean tooManyBondMismatches = (bondMismatchCurr + bondMisCount) > bondMismatchUpperBound;
                        if (!tooManyBondMismatches) {

                            bondMismatchCurr += bondMisCount;
                            
                            if (introducedNewComponent) {
                                ++currSubstructureNum;
                              
                            } 

                            /**
                             * This is where the algorithm gets a bit tricky. The recursive call to
                             * grow explores all the possible substructures that include the atom we 
                             * just deemed compatible. The eventual pop can then explore 
                             * substructures that may not contain the given pair of atoms.
                             */
                            
                            boolean aboveBound = currSubstructureNum > substructureNumLimit;
                            if ( !aboveBound ) {
                                System.out.println("growing...");
                                currentMapping.push(topCandidateAtom, otherAtom);        			
                                atomListTwo.remove(otherAtom);
                                System.out.println("Exploring "+topCandidateAtom.getSymbol()+" ("+smallCompound.getConnectedAtomsCount(topCandidateAtom)+" knex), "+otherAtom.getSymbol()+" ("+bigCompound.getConnectedAtomsCount(otherAtom) +" knex)");
                                grow(atomListOneCopy, atomListTwo, indLevel+1);
                             
                                atomListTwo.push(otherAtom);
                                currentMapping.pop();            

                            }  else {
                                System.out.println("only allow one connected component");
                            }
                            
                            if (introducedNewComponent) {
                                --currSubstructureNum;
                            }
                            
                            bondMismatchCurr -= bondMisCount;
                        } else {
                            System.out.println("bond orders do not match");
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
