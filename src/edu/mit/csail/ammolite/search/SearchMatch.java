package edu.mit.csail.ammolite.search;

import org.openscience.cdk.interfaces.IAtomContainer;

public class SearchMatch{
    
    protected IAtomContainer query;
    protected IAtomContainer target;
    protected IAtomContainer mcs = null;
    
    protected int overlap;

    public SearchMatch(IAtomContainer query, IAtomContainer target, int overlap){
        this.query = query;
        this.target = target;
        this.overlap = overlap;
    }

    public IAtomContainer getQuery() {
        return query;
    }
    public IAtomContainer getTarget() {
        return target;
    }
    public int getOverlap() {
        return overlap;
    }
    public void setMCS(IAtomContainer mcs){
        this.mcs = mcs;
    }
    public IAtomContainer getMCS() {
        return mcs;
    }
    



}
