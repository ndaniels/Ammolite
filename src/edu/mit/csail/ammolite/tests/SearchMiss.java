package edu.mit.csail.ammolite.tests;

import org.openscience.cdk.interfaces.IAtomContainer;

public class SearchMiss extends SearchComparison{
   
    public SearchMiss(IAtomContainer query, IAtomContainer target, int overlap){
        this.query = query;
        this.target = target;
        this.overlap = overlap;
    }

    @Override
    public boolean isMatch() {
        return false;
    }

}
