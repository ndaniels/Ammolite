package edu.mit.csail.ammolite.spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;



//import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.IMolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.mcs.SMSD;
import edu.mit.csail.ammolite.search.IResultHandler;
import edu.mit.csail.ammolite.search.ISearchMatch;
import edu.mit.csail.ammolite.search.SearchMatch;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.SDFUtils;
import edu.mit.csail.ammolite.utils.StructID;




public class AmmoliteSpark {

    
   public static List<ISearchMatch> distributedAmmoliteSearch(IAtomContainer query, IStructDatabase db, JavaSparkContext ctx, IResultHandler handler, double fineThresh, double coarseThresh, int chunkSize){
        
        IMolStruct coarseQuery = db.makeMoleculeStruct(query);
        
        List<String> structFilepaths = db.getStructFilepaths();
        Iterator<IAtomContainer> coarseTargetIterator = SDFUtils.parseSDFSetOnline(structFilepaths);

        List<ISearchMatch> coarseMatches = SMSDSpark.distributedChunkyLinearSearch(coarseQuery, coarseTargetIterator, ctx, handler, coarseThresh, chunkSize);

        List<StructID> matchingCoarseIDs = new ArrayList<StructID>();
        for(ISearchMatch match: coarseMatches){
            handler.handleCoarse(match);
            matchingCoarseIDs.add(MolUtils.getStructID(match.getTarget()));
        }       

        List<IAtomContainer> localFineTargets = new ArrayList<IAtomContainer>();
        for(StructID id: matchingCoarseIDs){
            localFineTargets.addAll(db.getMatchingMolecules(id));
        }
        JavaRDD<IAtomContainer> fineTargets = ctx.parallelize(localFineTargets,100);
        return null; //SMSDSpark.distributedLinearSearch(query, fineTargets, ctx, handler, fineThresh);
    }
    
 

}