package edu.mit.csail.ammolite.search;

import java.util.Collection;

import org.openscience.cdk.interfaces.IAtomContainer;

public class AmmoliteResult {
    private IAtomContainer query;
    private Collection<IAtomContainer> matches;

    public AmmoliteResult(IAtomContainer query, Collection<IAtomContainer> matches) {
        this.query = query;
        this.matches = matches;
    }
    
    public IAtomContainer query(){
        return query;
    }
    
    public Collection<IAtomContainer> matches(){
        return matches;
    }

}
