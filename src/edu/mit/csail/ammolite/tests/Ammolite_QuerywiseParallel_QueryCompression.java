package edu.mit.csail.ammolite.tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.compression.DatabaseCompression;
import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.StructID;

public class Ammolite_QuerywiseParallel_QueryCompression implements Tester {

    private static final String NAME = "Ammolite_QuerywiseParallel_QueryCompression";
    private static final int CHUNK_SIZE = 1000;
    private static final ExecutorService service = ParallelUtils.buildNewExecutorService();
    
    /**
     * Creates and runs an SMSD operation between every matching in a set of queries and 
     * a set of targets.
     * 
     * @param queries
     * @param targets
     * @return
     */
    private List<List<Integer>> testSetOfMolecules(List<IAtomContainer> queries, List<IAtomContainer> targets){
        List<List<Integer>> results = new ArrayList<List<Integer>>(queries.size());
        List<Callable<Integer>> tests = new ArrayList<Callable<Integer>>(targets.size());
        
        for(IAtomContainer fineQuery: queries){
            tests.clear();
            for(IAtomContainer fineTarget: targets){
                tests.add(MCS.getCallableSMSDOperation(fineTarget, fineQuery));
            }
            System.out.println("A");
            results.add( ParallelUtils.parallelFullExecution(tests, service));
        }
        return results;
    }
    
        
    private void processTests(List<IAtomContainer> queries, List<IAtomContainer> targets, List<List<Integer>> testResults, List<SearchResult> searchResults, List<SearchResult> coarseSearchResults, double thresh){
        for(int i=0; i<queries.size(); ++i){
            IAtomContainer query = queries.get(i);
            List<Integer> myMatchSizes = testResults.get(i);
            SearchResult mySearchResult = searchResults.get(i);
            SearchResult myCoarseSearchResult = coarseSearchResults.get(i);
            
            for(int j=0; j<targets.size(); j++){
                int matchSize = myMatchSizes.get(j);
                IAtomContainer relevantTarget = targets.get(j);
                boolean result = MCSUtils.overlapCoeff(matchSize, query, relevantTarget) >= thresh;
                myCoarseSearchResult.addMatch(relevantTarget, matchSize);
                if(result){
                    mySearchResult.addMatch(relevantTarget, matchSize);
                }
            }
        }
    }
    
    private List<StructID> coarseSearch(MolStruct coarseQuery, List<MolStruct> sTargets, double coarseThresh){
        List<Callable<Boolean>> coarseTests = new ArrayList<Callable<Boolean>>();
        for(MolStruct coarseTarget: sTargets){
            coarseTests.add(MCS.getCallableSMSDTest(coarseQuery, coarseTarget, coarseThresh));
        }
        System.out.println("B");
        List<Boolean> coarseResults = ParallelUtils.parallelFullExecution(coarseTests, service);
        List<StructID> coarseMatchIDs = new ArrayList<StructID>();
        
        for(int i=0; i<coarseResults.size(); ++i){
            boolean result = coarseResults.get(i);
            if( result){
                MolStruct coarseMatch = sTargets.get(i);
                coarseMatchIDs.add(MolUtils.getStructID(coarseMatch));
            }
        }
        return coarseMatchIDs;
    }
    
    private void fineSearch(List<IAtomContainer> fineQueries, List<StructID> coarseMatchIDs, IStructDatabase db, 
                            List<SearchResult> mySearchResults, List<SearchResult> myCoarseSearchResults, double thresh){
        // Fine Search
        List<IAtomContainer> fineTargetChunk = new ArrayList<IAtomContainer>(CHUNK_SIZE);
        for(StructID sID: coarseMatchIDs){
            if( fineTargetChunk.size() < CHUNK_SIZE){
                fineTargetChunk.addAll(db.getMatchingMolecules(sID));
            } else {
                List<List<Integer>> results = testSetOfMolecules(fineQueries, fineTargetChunk);
                processTests(fineQueries, fineTargetChunk, results, mySearchResults, myCoarseSearchResults, thresh);
            }  
        }
        List<List<Integer>> results = testSetOfMolecules(fineQueries, fineTargetChunk);
        processTests(fineQueries, fineTargetChunk, results, mySearchResults, myCoarseSearchResults, thresh);
    }

    @Override
    public List<SearchResult> test(List<IAtomContainer> queries,
                                    IStructDatabase db, Iterator<IAtomContainer> targets,
                                    List<MolStruct> sTargets, double thresh, double coarseThresh,
                                    String name) {
        
        KeyListMap<MolStruct,IAtomContainer> compressedQueries = DatabaseCompression.compressMoleculeSet(queries, db.getStructFactory());
        CommandLineProgressBar bar = new CommandLineProgressBar(name, queries.size());
        List<SearchResult> allResults = new ArrayList<SearchResult>();
        
        for(MolStruct coarseQuery: compressedQueries.keySet()){
            List<IAtomContainer> fineQueries = compressedQueries.get(coarseQuery);
            List<SearchResult> mySearchResults = new ArrayList<SearchResult>(fineQueries.size());
            List<SearchResult> myCoarseSearchResults = new ArrayList<SearchResult>(fineQueries.size());
            
            // Results pre-processing
            for(IAtomContainer fineQuery: fineQueries){
                SearchResult mySearchResult = new SearchResult(fineQuery, name);
                mySearchResult.start();
                mySearchResults.add( mySearchResult);
                allResults.add( mySearchResult);
                
                SearchResult myCoarseSearchResult = new SearchResult(fineQuery, name+"_COARSE");
                myCoarseSearchResult.start();
                myCoarseSearchResults.add( myCoarseSearchResult);
                allResults.add( myCoarseSearchResult);
            }

            List<StructID> coarseMatchIDs = coarseSearch(coarseQuery, sTargets, coarseThresh);
            fineSearch( fineQueries, coarseMatchIDs, db, mySearchResults, myCoarseSearchResults, thresh);
            
            // Results Processing
            for(SearchResult result: mySearchResults){
                result.end();
                bar.event();
            }
            for(SearchResult coarseResult: myCoarseSearchResults){
                coarseResult.end();
            }
        }
        service.isShutdown();
        return allResults;
    }


    @Override
    public String getName() {
        return NAME;
    }  

}
