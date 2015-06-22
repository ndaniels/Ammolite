package edu.mit.csail.ammolite.search;

import java.util.ArrayList;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * Result handler that makes a lsit of all molecules that match a query during a search.
 * 
 * @author dcdanko
 *
 */
public class MatchingMoleculesHandler implements IResultHandler {
    
    List<IAtomContainer> matchingMolecules = new ArrayList<IAtomContainer>();

    @Override
    public boolean recordingStructures() {
        return false;
    }

    @Override
    public void handleCoarse(SearchMatch result) {
        // Pass
    }

    @Override
    public void handleFine(SearchMatch result) {
        matchingMolecules.add(result.target);

    }

    @Override
    public void finishOneQuery() {
        // Pass

    }
    
    public List<IAtomContainer> getMatches(){
        return matchingMolecules;
    }

}
