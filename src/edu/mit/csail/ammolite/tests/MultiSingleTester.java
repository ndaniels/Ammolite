package edu.mit.csail.ammolite.tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;

public abstract class MultiSingleTester extends MultiTester {

    abstract SearchResult singleTest(IAtomContainer query, IStructDatabase db, 
            Iterator<IAtomContainer> targets,  List<MolStruct> sTargets, double fineThresh, 
            double coarseThresh, String name);

    public List<SearchResult> singleTestMultipleResults(IAtomContainer query, IStructDatabase db, 
                        Iterator<IAtomContainer> targets,  List<MolStruct> sTargets, double fineThresh, 
                        double coarseThresh, String name){
        List<SearchResult> l = new ArrayList<SearchResult>(1);
        l.add(singleTest(query, db, targets, sTargets, fineThresh, coarseThresh, name));
        return l;
    }

}
