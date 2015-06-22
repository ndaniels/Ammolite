package edu.mit.csail.ammolite.tests;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.MCSUtils;

public abstract class SearchComparison {

    protected IAtomContainer query;
    protected IAtomContainer target;
    protected int overlap;
    

    public IAtomContainer getQuery() {
        return query;
    }
    public IAtomContainer getTarget() {
        return target;
    }
    public int getOverlap() {
        return overlap;
    }
    
    public boolean isMatch(double thresh){
        return MCSUtils.overlapCoeff(overlap, query, target) > thresh;
    }
    
    abstract public boolean isMatch();

}
