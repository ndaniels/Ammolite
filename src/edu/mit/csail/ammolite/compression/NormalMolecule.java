package edu.mit.csail.ammolite.compression;

import java.util.HashSet;
import java.util.Set;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import edu.mit.csail.ammolite.utils.PubchemID;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.isomorphism.AbstractIsomorphismTester;

public class NormalMolecule extends AtomContainer implements IMolStruct {
    
    protected Set<PubchemID> mol_ids = new HashSet<PubchemID>();
            
    public NormalMolecule(IAtomContainer base){
        super( new AtomContainer(AtomContainerManipulator.removeHydrogens(base)) );
    }
    
    @Override
    public boolean isIsomorphic(IAtomContainer struct, AbstractIsomorphismTester tester) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addID(PubchemID id) {
        mol_ids.add(id);

    }

    @Override
    public Set<PubchemID> getIDNums() {
        return mol_ids;
    }

    @Override
    public Graph<? extends Edge> getGraph() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int fingerprint() {
         throw new UnsupportedOperationException();
    }

}
