package edu.mit.csail.ammolite.tests;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;

public abstract class MultiTester implements Tester{
    

    abstract List<SearchResult> singleTestMultipleResults(IAtomContainer query, IStructDatabase db, 
            Iterator<IAtomContainer> targets,  List<MolStruct> sTargets, double fineThresh, 
            double coarseThresh, String name);

    public void test(List<IAtomContainer> queries, IStructDatabase db, 
                Iterator<IAtomContainer> targets, Iterator<MolStruct> sTargets,  double fineThresh, 
                double coarseThresh, String name, PrintStream out){
        throw new UnsupportedOperationException();
//        List<SearchResult> results = new LinkedList<SearchResult>();
//        CommandLineProgressBar progressBar = new CommandLineProgressBar(name, queries.size());
//            for(IAtomContainer query: queries){
//            targets = SearchTest.getTargetIterator(db, targets);
//            List<SearchResult> result = singleTestMultipleResults(query, db, targets, sTargets, fineThresh, coarseThresh, name);
//            results.addAll(result);
//            progressBar.event();
//        }
        
    }
    
    abstract public String getName();
}
