package edu.mit.csail.ammolite.search;

import java.io.Serializable;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.ID;

public interface ISearchMatch extends Serializable{
    
    public boolean moleculeStructuresAvailable();
    public IAtomContainer getQuery();
    public IAtomContainer getTarget();
    public int getQuerySize();
    public int getTargetSize();
    public ID getQueryID();
    public ID getTargetID();
    public int getOverlap();
    public void setMCS(IAtomContainer mcs);
    public IAtomContainer getMCS();

}
