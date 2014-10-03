package edu.mit.csail.ammolite.tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
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
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.Pair;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.StructID;

public class Ammolite_Unfolded_MultipleQueriesInParallel_QueryCompression implements Tester {

    private static final String NAME = "Ammolite_Unfolded_MultipleQueriesInParallel_QueryCompression";
    
    private static final int CHUNK_SIZE = 1000;
    private static final ExecutorService service = ParallelUtils.buildNewExecutorService();
    
    private Callable<Pair<StructID, List<MolStruct>>> getCoarseCallable(final MolStruct structure, final MolStruct[] queries, final double cThresh){
        Callable<Pair<StructID, List<MolStruct>>> callable = new Callable<Pair<StructID, List<MolStruct>>>(){
            
            public Pair<StructID, List<MolStruct>> call() throws Exception {
                List<MolStruct> out = new LinkedList<MolStruct>();
                for(MolStruct query: queries){
                    if(MCS.beatsOverlapThresholdSMSD(query, structure, cThresh)){
                        out.add(query);
                    }
                }
                return new Pair<StructID, List<MolStruct>>( MolUtils.getStructID(structure), out);
            }
        };
        
        return callable;
    }
    
    private Callable<Pair<IAtomContainer, List<IAtomContainer>>> getFineCallable(final List<IAtomContainer> queries, final IAtomContainer target, final double thresh){
        Callable<Pair<IAtomContainer, List<IAtomContainer>>> callable = new Callable<Pair<IAtomContainer, List<IAtomContainer>>>(){
            
                public Pair<IAtomContainer, List<IAtomContainer>> call() throws Exception {
                    List<IAtomContainer> out = new LinkedList<IAtomContainer>();
                        for(IAtomContainer query: queries){
                            if(MCS.beatsOverlapThresholdSMSD(query, target, thresh)){
                                out.add( target);
                            }
                        }
                    return new Pair<IAtomContainer, List<IAtomContainer>>( target, out);
                }
            };
        
        return callable;
    }

    @Override
    public List<SearchResult> test(List<IAtomContainer> queries,
            IStructDatabase db, Iterator<IAtomContainer> targets,
            List<MolStruct> sTargets, double thresh, double prob,
            String name) {
        
        KeyListMap<MolStruct,IAtomContainer> compressedQueries = DatabaseCompression.compressMoleculeSet(queries, db.getStructFactory());
        CommandLineProgressBar bar = new CommandLineProgressBar(name, queries.size());
        List<SearchResult> allResults = new ArrayList<SearchResult>();
        
        KeyListMap<StructID, MolStruct> coarseResults = new KeyListMap<>(CHUNK_SIZE);

        final MolStruct[] coarseQueries = compressedQueries.keySet().toArray(new MolStruct[0]);
        List<Callable<Pair<StructID, List<MolStruct>>>> coarseChunk = new ArrayList<>(CHUNK_SIZE);
        for(final MolStruct targetStruct: sTargets){
                if( coarseChunk.size() == CHUNK_SIZE){
                    
                   List<Pair<StructID, List<MolStruct>>> called = ParallelUtils.parallelFullExecution(coarseChunk, service);
                    for(int i=0; i<coarseChunk.size(); i++){
                        StructID id = called.get(i).left();
                        List<MolStruct> queriesThatMatchID = called.get(i).right();
                        coarseResults.put(id, queriesThatMatchID);
                    }
                    coarseChunk.clear();
                        
                }
                coarseChunk.add( getCoarseCallable( targetStruct, coarseQueries, prob));
            }
        
            for(StructID id: coarseResults.keySet()){
                List<MolStruct> matchingCoarseQueries = coarseResults.get(id);
                List<IAtomContainer> matchingFineQueries = new ArrayList<>();
                for(MolStruct coarseMatch: matchingCoarseQueries){
                    matchingFineQueries.addAll( compressedQueries.get(coarseMatch));
                }
                
                for(IAtomContainer target: db.getMatchingMolecules(id)){
                    getFineCallable(matchingFineQueries, target, thresh);
                }
                matchingFineQueries.clear();
            }
            
            return null;
        }

    @Override
    public String getName() {
        return NAME;
    }
    
        
        
        
    
    }

