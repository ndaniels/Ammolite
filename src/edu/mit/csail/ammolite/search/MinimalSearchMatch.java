package edu.mit.csail.ammolite.search;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.ID;
import edu.mit.csail.ammolite.utils.MolUtils;

public class MinimalSearchMatch implements ISearchMatch {
    protected int querySize;
    protected int targetSize;
    protected int overlapSize;
    protected ID queryID;
    protected ID targetID;
    
    public MinimalSearchMatch(IAtomContainer query, IAtomContainer target, int overlap){
        this.overlapSize = overlap;
        this.querySize = MolUtils.getAtomCountNoHydrogen(query);
        this.targetSize = MolUtils.getAtomCountNoHydrogen(target);
        this.queryID = MolUtils.getUnknownOrID(query);
        this.targetID = MolUtils.getUnknownOrID(target);
    }
    
    public boolean moleculeStructuresAvailable() {
        return false;
    }

    @Override
    public IAtomContainer getQuery() {
        return null;
    }

    @Override
    public IAtomContainer getTarget() {
        return null;
    }

    @Override
    public int getQuerySize() {
        return querySize;
    }

    @Override
    public int getTargetSize() {
        return targetSize;
    }

    @Override
    public int getOverlap() {
        return overlapSize;
    }

    @Override
    public void setMCS(IAtomContainer mcs) {

    }

    @Override
    public IAtomContainer getMCS() {
        return null;
    }

    @Override
    public ID getQueryID() {
        return queryID;
    }

    @Override
    public ID getTargetID() {
        return targetID;
    }

}
