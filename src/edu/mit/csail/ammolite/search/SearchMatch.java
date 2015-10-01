package edu.mit.csail.ammolite.search;

import java.io.Serializable;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.ID;
import edu.mit.csail.ammolite.utils.MolUtils;

public class SearchMatch implements ISearchMatch {
    
    protected IAtomContainer query;
    protected IAtomContainer target;
    protected IAtomContainer mcs = null;
    
    
    protected int overlap;

    public SearchMatch(IAtomContainer query, IAtomContainer target, int overlap){
        this.query = query;
        this.target = target;
        this.overlap = overlap;
    }
    
    public boolean moleculeStructuresAvailable(){
        return true;
    }
    
    public int getQuerySize(){
        return MolUtils.getAtomCountNoHydrogen(query);
    }
    
    public int getTargetSize(){
        return MolUtils.getAtomCountNoHydrogen(target);
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

    @Override
    public ID getQueryID() {
        return MolUtils.getUnknownOrID(query);
    }

    @Override
    public ID getTargetID() {
        return MolUtils.getUnknownOrID(target);
    }
    



}
