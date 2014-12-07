package edu.mit.csail.ammolite.compression;

import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.PubchemID;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.isomorphism.AbstractIsomorphismTester;

public interface IMolStruct extends IAtomContainer{
    
    
    public boolean isIsomorphic(IAtomContainer struct, AbstractIsomorphismTester tester);
    
    public void addID(PubchemID id);
    public Set<PubchemID> getIDNums();
    public Graph<? extends Edge> getGraph();
    public int fingerprint();
}
