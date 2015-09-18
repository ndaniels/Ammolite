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
import edu.mit.csail.ammolite.search.SearchMatch;
import edu.mit.csail.ammolite.utils.MCSUtils;
//import edu.mit.csail.ammolite.utils.SDFUtils;




import org.apache.spark.api.java.function.Function;

public class SMSDSpark {
    
    public static List<SearchMatch> distributedChunkyLinearSearch(IAtomContainer query, Iterator<IAtomContainer> targetIterator, JavaSparkContext ctx, IResultHandler handler, double threshold, int chunkSize){
        List<SearchMatch> resultCollector = new ArrayList<SearchMatch>();
        
        List<IAtomContainer> localTargetChunk = new ArrayList<IAtomContainer>(chunkSize);
        while(targetIterator.hasNext()){
            for(int i=0; i<chunkSize; i++){
                if(targetIterator.hasNext()){
                    localTargetChunk.add(targetIterator.next());
                }
            }
            JavaRDD<IAtomContainer> targetChunk = ctx.parallelize(localTargetChunk,100);
            
            resultCollector.addAll(SMSDSpark.distributedLinearSearch(query, targetChunk, ctx, handler, threshold));
        }
        
        return resultCollector;
    }
    
    public static List<SearchMatch> distributedLinearSearch(final IAtomContainer query, JavaRDD<IAtomContainer> targets, JavaSparkContext ctx, IResultHandler handler, final double threshold){

        final boolean recordStructs = handler.recordingStructures();
        
        JavaRDD<SearchMatch> matchings = targets.map(new Function<IAtomContainer, SearchMatch>(){
            
            public SearchMatch call(IAtomContainer target){
                SearchMatch match = null;
                
                int targetSize = MCSUtils.getAtomCountNoHydrogen(target);
                int querySize = MCSUtils.getAtomCountNoHydrogen(query);
                double smaller = Math.min(targetSize, querySize);
                double larger  = Math.max(targetSize, querySize);
                double upperTanimoto = smaller / larger;
                if( upperTanimoto > threshold){
                    
                    SMSD smsd = new SMSD(target, query);
                    smsd.timedCalculate();
                    int overlap = smsd.size();
                    
                    if(MCSUtils.tanimotoCoeff(overlap, targetSize, querySize) > threshold){
                        match = new SearchMatch(query, target, overlap);
                        if( recordStructs){
                            match.setMCS(smsd.getFirstSolution());
                        }
                    }
                }
                
                if(match == null){
                    match = new SearchMatch(query, target, 0);
                }
                
                return match;
            }
        });
        
        JavaRDD<SearchMatch> filtered = matchings.filter(new Function<SearchMatch, Boolean>(){
            public Boolean call(SearchMatch match){
                if(match.getOverlap() > 0){
                    return true;
                }
                return false;
            }
        });
        
       List<SearchMatch> collected = filtered.collect();
       return collected;

    }

}
