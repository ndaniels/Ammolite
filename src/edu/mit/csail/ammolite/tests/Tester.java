package edu.mit.csail.ammolite.tests;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.IMolStruct;
import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.tests.SearchResult;

public interface Tester {
    public void test(List<IAtomContainer> queries, IStructDatabase db, Iterator<IMolStruct> sTargets, double thresh, 
            double prob, String name, PrintStream out);
    
    public String getName();
}
