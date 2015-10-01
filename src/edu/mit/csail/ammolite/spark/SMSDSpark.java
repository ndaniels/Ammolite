package edu.mit.csail.ammolite.spark;

//import org.apache.spark.SparkConf;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.mcs.SMSD;
import edu.mit.csail.ammolite.search.IResultHandler;
import edu.mit.csail.ammolite.search.ISearchMatch;
import edu.mit.csail.ammolite.search.MinimalSearchMatch;
import edu.mit.csail.ammolite.search.SearchMatch;
import edu.mit.csail.ammolite.utils.MCSUtils;
//import edu.mit.csail.ammolite.utils.SDFUtils;







import org.apache.spark.api.java.function.Function;
import org.apache.spark.broadcast.Broadcast;

public class SMSDSpark {
    
    private static final int MOLS_PER_TASK = 5;
    
    public static List<ISearchMatch> distributedChunkyLinearSearch(IAtomContainer query, Iterator<IAtomContainer> targetIterator, JavaSparkContext ctx, IResultHandler handler, double threshold, int chunkSize){
        JavaRDD<ISearchMatch> resultCollector = ctx.parallelize( new ArrayList<ISearchMatch>());
        Broadcast<IAtomContainer> broadcastQuery = ctx.broadcast(query);
        
        List<IAtomContainer> localTargetChunk = new ArrayList<IAtomContainer>(chunkSize);
        while(targetIterator.hasNext()){
            for(int i=0; i<chunkSize; i++){
                if(targetIterator.hasNext()){
                    localTargetChunk.add(targetIterator.next());
                }
            }
            JavaRDD<IAtomContainer> targetChunk = ctx.parallelize(localTargetChunk, chunkSize / MOLS_PER_TASK);
            
            resultCollector = resultCollector.union( distributedLinearSearch(broadcastQuery, targetChunk, ctx, handler, threshold));
        }
        
        return resultCollector.collect();
    }
    
    public static JavaRDD<ISearchMatch> distributedLinearSearch(final Broadcast<IAtomContainer> query, JavaRDD<IAtomContainer> targets, JavaSparkContext ctx, IResultHandler handler, final double threshold){

        final Broadcast<Boolean> recordStructs = ctx.broadcast( handler.recordingStructures());
        
        
        JavaRDD<ISearchMatch> matchings = targets.map(new Function<IAtomContainer, ISearchMatch>(){
            
            public ISearchMatch call(IAtomContainer target){
                ISearchMatch match = null;
                
                int targetSize = MCSUtils.getAtomCountNoHydrogen(target);
                int querySize = MCSUtils.getAtomCountNoHydrogen(query.value());
                double smaller = Math.min(targetSize, querySize);
                double larger  = Math.max(targetSize, querySize);
                double upperTanimoto = smaller / larger;
                if( upperTanimoto > threshold){
                    
                    SMSD smsd = new SMSD(target, query.value());
                    smsd.timedCalculate();
                    int overlap = smsd.size();
                    
                    if(MCSUtils.tanimotoCoeff(overlap, targetSize, querySize) > threshold){
                        match = new MinimalSearchMatch(query.value(), target, overlap);
                        if( recordStructs.value()){
                            match = new SearchMatch(query.value(), target, overlap);
                            match.setMCS(smsd.getFirstSolution());
                        }
                    }
                }
                
                if(match == null){
                    match = new MinimalSearchMatch(query.value(), target, 0);
                }
                
                return match;
            }
        });
        
        JavaRDD<ISearchMatch> filtered = matchings.filter(new Function<ISearchMatch, Boolean>(){
            public Boolean call(ISearchMatch match){
                if(match.getOverlap() > 0){
                    return true;
                }
                return false;
            }
        });
        
       return filtered;
       
//       List<SearchMatch> collected = filtered.collect();
//       return collected;

    }

}
