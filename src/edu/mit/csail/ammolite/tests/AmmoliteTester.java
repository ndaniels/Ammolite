package edu.mit.csail.ammolite.tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.PubchemID;

public abstract class AmmoliteTester extends MultiTester{
        
        abstract int getCoarseOverlap(MolStruct query, MolStruct target);
        
        protected int getFineOverlap(IAtomContainer query, IAtomContainer target){
            return MCS.getSMSDOverlap(query, target);
        }
        
        public List<SearchResult> singleTestMultipleResults(IAtomContainer query, IStructDatabase db, 
                                        Iterator<IAtomContainer> targets, List<MolStruct> sTargets,  double fineThresh, 
                                        double coarseThresh, String name){
            SearchResult result = new SearchResult(query, name);
            SearchResult coarseResult = new SearchResult(query, name+"_COARSE");
            result.start();
            MolStruct sQuery = (MolStruct) db.makeMoleculeStruct(query);
        
            for(MolStruct sTarget: sTargets){
                coarseResult.start();
                int coarseOverlap = getCoarseOverlap(sQuery, sTarget);
                coarseResult.end();
                if(coarseThresh <= MCSUtils.overlapCoeff(coarseOverlap, sQuery, sTarget)){
                    List<IAtomContainer> sTargetMatches;
                    if(db.isOrganized()){
                        sTargetMatches = db.getMatchingMolecules(MolUtils.getStructID(sTarget));
                    } else {
                        sTargetMatches = new ArrayList<IAtomContainer>();
                        for(PubchemID pubchemID: sTarget.getIDNums()){
                            IAtomContainer target = db.getMolecule(pubchemID);
                            sTargetMatches.add(target);
                        }
                    }
            
                    
                    for(IAtomContainer target: sTargetMatches){
                        coarseResult.addMatch(target, coarseOverlap);
                        int fineOverlap = getFineOverlap(query, target);
                        if(fineThresh <= MCSUtils.overlapCoeff(fineOverlap, query, target)){
                            result.addMatch(target, fineOverlap);
                        }
                    }
                }
                
            }
            result.end();
            List<SearchResult> l = new ArrayList<SearchResult>(2);
            l.add(result);
            l.add(coarseResult);
            return l;

        }
}
