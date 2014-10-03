package edu.mit.csail.ammolite.tests;

import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.MCSUtils;

public class SMSD_SingleThread extends MultiSingleTester {
    
    private static final String NAME = "SMSD_SingleThread";

    public SearchResult singleTest(IAtomContainer query, IStructDatabase db, 
            Iterator<IAtomContainer> targets,  List<MolStruct> sTargets, double thresh, 
            double prob, String name){

        SearchResult result = new SearchResult(query, name);
        result.start();
        
        for(IAtomContainer target= targets.next(); targets.hasNext(); target=targets.next()){
            int mcsSize = MCS.getSMSDOverlap(target, query);
            if(thresh <= MCSUtils.overlapCoeff(mcsSize, target, query)){
                result.addMatch(target, mcsSize);
                }
            }
        result.end();
        return result;

}

    @Override
    public String getName() {
        return NAME;
    }

}
