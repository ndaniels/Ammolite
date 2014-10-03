package edu.mit.csail.ammolite.tests;

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
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.StructID;

public class Ammolite_MultipleQueriesInParallel_QueryCompression implements Tester {
    private static final String NAME = "Ammolite_MultipleQueriesInParallel_QueryCompression";
    private static final int CHUNK_SIZE = 72;
    private CommandLineProgressBar bar;

    @Override
    public List<SearchResult> test(List<IAtomContainer> queries, IStructDatabase db, 
                                    Iterator<IAtomContainer> targets, List<MolStruct> sTargets, double thresh, double coarseThresh, String name) {
        
        KeyListMap<MolStruct,IAtomContainer> comQueries = DatabaseCompression.compressMoleculeSet(queries, db.getStructFactory());
        bar = new CommandLineProgressBar(name, queries.size());
        
        ExecutorService service = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors());
        
        List<SearchResult> allResults = new LinkedList<SearchResult>();
        
        List<Callable<ResultList>> callChunk = new ArrayList<Callable<ResultList>>(CHUNK_SIZE);
        for(final MolStruct comQuery: comQueries.keySet()){
            if( callChunk.size() == CHUNK_SIZE){
                List<ResultList> calledChunk = ParallelUtils.parallelFullExecution(callChunk, service);
                callChunk.clear();
                for(ResultList rL: calledChunk){
                    allResults.addAll(rL);
                    bar.event();
                }
            }
            
            final List<IAtomContainer> exQueries = comQueries.get(comQuery);
            Callable<ResultList> callable = callableQueries(comQuery, exQueries, sTargets, db, thresh, coarseThresh);
            callChunk.add(callable);
        }
        
        List<ResultList> calledChunk = ParallelUtils.parallelFullExecution(callChunk, service);
        callChunk.clear();
        for(ResultList rL: calledChunk){
            allResults.addAll(rL);
        }
        return allResults;
    }
    
    private Callable<ResultList> callableQueries(final MolStruct comQuery, final List<IAtomContainer> queries, final List<MolStruct> coarseTargets, 
                                                    final IStructDatabase db, final double thresh, final double coarseThresh){
        
        Callable<ResultList> callable = new Callable<ResultList>(){
            
            public ResultList call(){
                ResultList results = new ResultList( queries, getName());
                results.startAllResults();
                // Coarse Search
                List<StructID> coarseHits = new ArrayList<StructID>();
                for(MolStruct coarseTarget: coarseTargets){
                    if( MCS.beatsOverlapThresholdSMSD(coarseTarget, comQuery, coarseThresh)){
                        coarseHits.add(MolUtils.getStructID(coarseTarget));
                    }
                }
                
                for(StructID coarseHitID: coarseHits){
                    for(IAtomContainer target: db.getMatchingMolecules(coarseHitID)){
                        for(IAtomContainer query: queries){
                            int overlap = MCS.getSMSDOverlap(query, target);
                            if( MCSUtils.overlapCoeff(overlap, query, target) >= thresh){
                                SearchMatch match = new SearchMatch(query, target, overlap);
                                results.get(query).addMatch(match);
                            }
                        }
                    }
                }
                results.endAllResults();
                for(SearchResult arb: results){
                    bar.event();
                }
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
