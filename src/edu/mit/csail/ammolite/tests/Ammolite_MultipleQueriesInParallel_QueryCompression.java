package edu.mit.csail.ammolite.tests;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.compression.DatabaseCompression;
import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.DoubleProgressBar;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.StructID;

public class Ammolite_MultipleQueriesInParallel_QueryCompression implements Tester {
    private static final String NAME = "Ammolite_MultipleQueriesInParallel_QueryCompression";
    private static final int CHUNK_SIZE = 72;
    private DoubleProgressBar bar;

    @Override
    public void test(List<IAtomContainer> queries, IStructDatabase db, 
                                    Iterator<IAtomContainer> targets, List<MolStruct> sTargets, double thresh, double coarseThresh, String name, PrintStream out) {
        throw new UnsupportedOperationException();
//        KeyListMap<MolStruct,IAtomContainer> comQueries = DatabaseCompression.compressMoleculeSet(queries, db.getStructFactory());
//        bar = new DoubleProgressBar(name, queries.size(), "Coarse", comQueries.keySet().size()*db.getStructs().size());
//        
//        ExecutorService service = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors());
//        
//        List<SearchResult> allResults = new LinkedList<SearchResult>();
//        
//        List<Callable<ResultList>> callChunk = new ArrayList<Callable<ResultList>>(CHUNK_SIZE);
//        for(final MolStruct comQuery: comQueries.keySet()){
//            if( callChunk.size() == CHUNK_SIZE){
//                List<ResultList> calledChunk = ParallelUtils.parallelFullExecution(callChunk, service);
//                callChunk.clear();
//                for(ResultList rL: calledChunk){
//                    allResults.addAll(rL);
//                }
//            }
//            
//            final List<IAtomContainer> exQueries = comQueries.get(comQuery);
//            Callable<ResultList> callable = callableQueries(comQuery, exQueries, sTargets, db, thresh, coarseThresh);
//            callChunk.add(callable);
//        }
//        
//        List<ResultList> calledChunk = ParallelUtils.parallelFullExecution(callChunk, service);
//        service.shutdown();
//        callChunk.clear();
//        for(ResultList rL: calledChunk){
//            allResults.addAll(rL);
//        }
    }
    
    private Callable<ResultList> callableQueries(final MolStruct comQuery, final List<IAtomContainer> queries, final List<MolStruct> coarseTargets, 
                                                    final IStructDatabase db, final double thresh, final double coarseThresh){
        
        Callable<ResultList> callable = new Callable<ResultList>(){
            
            public ResultList call(){
                ResultList results = new ResultList( queries, getName());
                SearchResult coarse = new SearchResult(comQuery, "COARSE_" + getName());
                results.startAllResults();
                coarse.start();
                // Coarse Search
                List<StructID> coarseHits = new ArrayList<StructID>();
                for(MolStruct coarseTarget: coarseTargets){
                    int cOverlap = MCS.getSMSDOverlap(comQuery, coarseTarget);
                    if( MCSUtils.overlapCoeff(cOverlap, comQuery, coarseTarget) >= coarseThresh){
                        coarseHits.add(MolUtils.getStructID(coarseTarget));
                        SearchMatch cMatch = new SearchMatch(comQuery, coarseTarget, cOverlap);
                        coarse.addMatch(cMatch);
                    } else {
                        SearchMiss cMiss = new SearchMiss(comQuery, coarseTarget, cOverlap);
                        coarse.addMiss(cMiss);
                    }
                    bar.secondEvent();
                }
                coarse.end();
                
                for(StructID coarseHitID: coarseHits){
                    for(IAtomContainer target: db.getMatchingMolecules(coarseHitID)){
                        for(IAtomContainer query: queries){
                            int overlap = MCS.getSMSDOverlap(query, target);
                            if( MCSUtils.overlapCoeff(overlap, query, target) >= thresh){
                                SearchMatch match = new SearchMatch(query, target, overlap);
                                results.get(query).addMatch(match);
                            } else {
                                SearchMiss miss = new SearchMiss(query, target, overlap);
                                results.get(query).addMiss(miss);
                            }
                        }
                    }
                }
                results.endAllResults();
                for(SearchResult arb: results){
                    bar.firstEvent();
                }
                results.add(coarse);
                return results;
            }
        };
        return callable;
        
    }

    @Override
    public String getName() {
        return NAME;
    }

}
