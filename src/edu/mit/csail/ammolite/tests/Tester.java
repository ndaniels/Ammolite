package edu.mit.csail.ammolite.tests;

import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.tests.SearchResult;

public interface Tester {
    public List<SearchResult> test(List<IAtomContainer> queries, IStructDatabase db, 
            Iterator<IAtomContainer> targets,  List<MolStruct> sTargets, double thresh, 
            double prob, String name);
    
    public String getName();
}
