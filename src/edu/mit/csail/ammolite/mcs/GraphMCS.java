package edu.mit.csail.ammolite.mcs;

import java.util.ArrayList;
import java.util.List;

import edu.mit.csail.fmcsj.CompatibleReturn;
import edu.mit.csail.fmcsj.GraphMCSList;
import edu.mit.csail.fmcsj.GraphMCSMap;
import edu.mit.csail.fmcsj.MCSList;
import edu.mit.csail.fmcsj.MolStruct;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.SimpleEdge;
import edu.ucla.sspace.graph.SparseUndirectedGraph;
import edu.ucla.sspace.graph.isomorphism.VF2IsomorphismTester;

public class GraphMCS {
	
	
	private boolean identicalGraph;
	public MCSList<GraphMCSMap> bestList = new MCSList<GraphMCSMap>();
	public GraphMCSMap currentMapping = new GraphMCSMap();
	private int userDefinedLowerBound = 0;
	private int substructureNumLimit = 1;
	private int currSubstructureNum;
	private int solutionSize = -1;
	public SparseUndirectedGraph compoundOne;
	public SparseUndirectedGraph compoundTwo;
	boolean timeoutStop = false;
	
	public GraphMCS( MolStruct a, MolStruct b){
		this( a.getGraph(), b.getGraph());
	}

	/**
	 * Constructor
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
 	public GraphMCS(	SparseUndirectedGraph _compoundOne, SparseUndirectedGraph _compoundTwo){
 		
		currSubstructureNum = 0;
		identicalGraph = false;
		
		// Compound One is the smaller of the two compounds, by definition.
		if( _compoundOne.order() < _compoundTwo.order() ){
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
            return compoundOne.order();
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
	 * Convert intContainers into lists and then call grow.
	 * 
	 * TODO: this is very C++
	 */
	private void max(){
		
		GraphMCSList atomListOne = new GraphMCSList();
		for(int a: compoundOne.vertices()){
			atomListOne.push(a);
		}
		GraphMCSList atomListTwo = new GraphMCSList();
		for(int a: compoundTwo.vertices()){
			atomListTwo.push(a);
		}
		grow(atomListOne, atomListTwo);
	}
	
	
	/**
	 * Checks whether a given pair of atoms are compatible by seeing if they share 
	 * the same neighboring atoms. Does not penalize atoms for having different bond
	 * types than their counterpart but does keep track of this.
	 * 
	 * TODO: check how int equality works
	 * 
	 * @param atom1
	 * @param atom2
	 * @return whether the atoms are compatible
	 */
	private CompatibleReturn compatible(int atom1, int atom2){
		
		CompatibleReturn out = new CompatibleReturn();
		
		GraphMCSList targetNeighborMapping = new GraphMCSList();
		GraphMCSList atomOneNeighborList 
			= new GraphMCSList(  compoundOne.getNeighbors(atom1));
		
		for(int atom1Neighbor: atomOneNeighborList){
			
			if( currentMapping.containsKey( atom1Neighbor ) ){
				targetNeighborMapping.push(atom1Neighbor);
			}
		}
		
		GraphMCSList currNeighborMapping = new GraphMCSList();
		GraphMCSList atomTwoNeighborList 
			= new GraphMCSList( compoundTwo.getNeighbors(atom2) ); 
		
		for(int atom2N: atomTwoNeighborList){

			
			if( currentMapping.containsVal( atom2N ) ){
				int k = currentMapping.getKey( atom2N );
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
		}
			
		// No need to check mismatched bonds here
		return out;
		
	}
	
	/**
	 * Selectively pops the atom with the most potential connections with the current 
	 * structure
	 * @param atomList
	 * @return the candidate
	 */
	private int top(GraphMCSList atomList){

		int firstCandidate = atomList.peek();
		int candidateAtom = -1;
		
		for(int i=0; i<atomList.size(); i++){
			int nextContender = atomList.get(i);
			// sets the 'best' candidate as the one with the most bonds in the smaller molecule
			int bondsToNextContender = compoundOne.degree(nextContender);
			int bondsToBestCandidate = compoundOne.degree(firstCandidate);
			
			if( bondsToNextContender > bondsToBestCandidate){
				firstCandidate = nextContender;
			}
			
			GraphMCSList neighborAtomList 
				= new GraphMCSList( compoundOne.getNeighbors(nextContender));
			for(int j=0; j<neighborAtomList.size(); j++){
				// The real candidate is the one with the most bonds that has a neighbor in the 
				// current mapping
				if( currentMapping.containsKey(neighborAtomList.get(j))){
					if(		( candidateAtom == -1 ) || 
							( bondsToNextContender > bondsToBestCandidate)){

						candidateAtom = nextContender;
						break;
					}
				}
			}
		}
		// Really the 'best' candidate is only used if we don't have anything else
		if( candidateAtom == -1 ){

			atomList.remove(firstCandidate);
			return firstCandidate;
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
	 * Converts the best list into a set of intContainers
	 * 
	 * @return
	 */
	public List<SparseUndirectedGraph> getSolutions(){
		
		////edu.mit.csail.ammolite.Logger.debug("Found "+bestList.size()+" solutions of size "+size());
		ArrayList<SparseUndirectedGraph> out = new ArrayList<SparseUndirectedGraph>(bestList.size());
		if(identicalGraph){
			out.add(compoundOne);
			return out;
		}

		for(GraphMCSMap map: bestList){
			SparseUndirectedGraph keySol = new SparseUndirectedGraph();
			SparseUndirectedGraph valSol = new SparseUndirectedGraph();
			for(int atom: map.getKeyList()){
				keySol.add(atom);
			}
			for(int atom: map.getValList()){
				valSol.add(atom);
			}
			
			for(int i=0; i<keySol.order(); ++i){
				for(int j=0; j<i; ++j){
					if( compoundOne.getEdges(i,j).size() > 0){
						keySol.add(new SimpleEdge(i,j));
					}
				}
			}
			
			for(int i=0; i<valSol.order(); ++i){
				for(int j=0; j<i; ++j){
					if( compoundTwo.getEdges(i,j).size() > 0){
						valSol.add(new SimpleEdge(i,j));
					}
				}
			}
			
			out.add(keySol);
			out.add(valSol);
		}
		return out;
	}
	
	private void grow(GraphMCSList atomListOne, GraphMCSList atomListTwo){
		grow(atomListOne, atomListTwo, 0);
	}
	
	/**
	 * The core of the fmcs algorithm. Explores likely substructure branches.
	 * 
	 * @param atomListOne
	 * @param atomListTwo
	 */
	private void grow(GraphMCSList atomListOne, GraphMCSList atomListTwo, int indLevel){
		GraphMCSList atomListOneCopy = new GraphMCSList( atomListOne );
		GraphMCSList atomListTwoCopy = new GraphMCSList( atomListTwo );
		MCSList<Integer> atomListOneDegrees = new MCSList<Integer>();
		MCSList<Integer> atomListTwoDegrees = new MCSList<Integer>();
		
		// For every atom in list one makes a corresponding list of their potential degree in the 
		// current mapping
		for( int atom: atomListOne){
			
			if(!currentMapping.containsKey( atom )){
				int degree = 0;
				
				for(int neighbor: compoundOne.getNeighbors(atom)){

					if(currentMapping.containsKey(neighbor)){
						degree++;
					}
				}

				atomListOneDegrees.push(degree);	
			}
		}
		
		// For every atom in list two makes a corresponding list of their potential degree in the 
		// current mapping
		for( int atom: atomListTwo){
			
			if(!currentMapping.containsKey( atom )){
				int degree = 0;
				
				for(int neighbor: compoundTwo.getNeighbors(atom)){
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
            
			
            int topCandidateAtom = top(atomListOneCopy);

    		for( int otherAtom: atomListTwoCopy){

                CompatibleReturn compOut = compatible(topCandidateAtom, otherAtom);
                
                boolean introducedNewComponent = compOut.introducedNewComponent;
                boolean foundCompatible = compOut.compatible;

                if ( foundCompatible ) {
                    
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
            			
                        currentMapping.push(topCandidateAtom, otherAtom);        			
                        atomListTwo.remove(otherAtom);

                        grow(atomListOneCopy, atomListTwo, indLevel+1);
                     
                        atomListTwo.push(otherAtom);
                        currentMapping.pop();            

                    }
                    
                    if (introducedNewComponent) {
                        --currSubstructureNum;
                    }
                
                }
                
            }// end for( int otherAtom: atomListTwoCopy)
		}
		
		
	}
}
