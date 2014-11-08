package edu.mit.csail.ammolite.search;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;
import edu.mit.csail.ammolite.utils.SDFUtils;

public class AmmoliteSearcher {

    
    public static void search(String queryFilename, String dbFilename, double threshold, int numThreads){
        searchSingly(queryFilename, dbFilename, threshold, numThreads);
    }
    
    
    private static void batchSearch(String queryFilename, String dbFilename, double threshold, int numThreads){
        
    }
    

    /**
     * Sets up and starts an Ammolite search. Search proceeds one query at a time.
     * 
     * For large numbers of queries batch search should generally be used.
     * 
     * @param queryFilename
     * @param dbFilename
     * @param threshold the minimum overlap coefficient for a match
     * @param numThreads
     */
    private static void searchSingly(String queryFilename, String dbFilename, double threshold, int numThreads){
        
        int roughNumQueries = SDFUtils.estimateNumMolsInSDF(queryFilename);
        
        IStructDatabase db = StructDatabaseDecompressor.decompress(dbFilename);
        IteratingSDFReader queries = (IteratingSDFReader) SDFUtils.parseSDFOnline(queryFilename);
        BlockingQueue<IAtomContainer> queryQueue = new ArrayBlockingQueue<IAtomContainer>(roughNumQueries);
        BlockingQueue<AmmoliteResult> results = new ArrayBlockingQueue<AmmoliteResult>(roughNumQueries);
        double coarseThreshold = threshold;
        
        QueryProducer queryProducer = new QueryProducer(queries, queryQueue);
        SingleSearcher searcher = new SingleSearcher(queryQueue, results, db, numThreads, coarseThreshold, threshold);
        ResultConsumer resultConsumer = new ResultConsumer( results);
        
        Thread queryThread  = new Thread(queryProducer);
        Thread searchThread = new Thread(searcher);
        Thread resultThread = new Thread(resultConsumer);
        
        queryThread.start();
        searchThread.start();
        resultThread.start();
        
        try {
            queryThread.join();
            searchThread.join();
            resultThread.interrupt();
            resultThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        try {
            queries.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
 
    private static void writeResultToFile(AmmoliteResult result){
        throw new UnsupportedOperationException();
    }
    
    /**
     * Simple runnable which iterates through queries and writes 
     * them to a queue.
     * 
     * For most cases of Ammolite this thread will spend most 
     * of its time blocked.
     * 
     * If interrupted this class does not continue to run.
     * 
     * @author dcdanko
     *
     */
    private static class QueryProducer implements Runnable {
        private BlockingQueue<IAtomContainer> queryQueue;
        private Iterator<IAtomContainer> queries;
        
        public QueryProducer(Iterator<IAtomContainer> queries, BlockingQueue<IAtomContainer> queryQueue){
            this.queries = queries;
            this.queryQueue = queryQueue;
        }
        
        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted() && queries.hasNext()){
                IAtomContainer query = queries.next();
                try {
                    queryQueue.put(query);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Simple runnable which removes results from a queue and handles them.
     * 
     * For most cases of Ammolite this thread will spend most 
     * of its time blocked.
     * 
     * If interrupted this class will handle all remaining results
     * in the queue. This is because Ammolite results represent a significant 
     * investment of computing power. 
     * 
     * @author dcdanko
     *
     */
    private static class ResultConsumer implements Runnable {
        private BlockingQueue<AmmoliteResult> results;
        
        public ResultConsumer(BlockingQueue<AmmoliteResult> results){
            this.results = results;
        }

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted() || !results.isEmpty()){
                AmmoliteResult result = null;
                try {
                    result = results.poll(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if( result != null){
                    writeResultToFile(result);
                }
            }
            
        }
        
    }
}
