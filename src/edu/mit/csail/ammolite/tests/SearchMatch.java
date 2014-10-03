package edu.mit.csail.ammolite.tests;

import org.openscience.cdk.interfaces.IAtomContainer;

public class SearchMatch {

    private IAtomContainer query;
    private IAtomContainer target;
    private int overlap;
    
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
}
