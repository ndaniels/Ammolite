package edu.mit.csail.ammolite.tests;

import org.openscience.cdk.interfaces.IAtomContainer;

public class SearchTimeout extends SearchComparison {

    public SearchTimeout(IAtomContainer query, IAtomContainer target){
        this.query = query;
        this.target = target;
        this.overlap = 0;
    }

    @Override
    public boolean isMatch() {
        return false;
    }

}
