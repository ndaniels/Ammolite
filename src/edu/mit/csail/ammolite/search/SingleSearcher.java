package edu.mit.csail.ammolite.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.IMolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.StructID;

public class SingleSearcher implements Runnable {
    private BlockingQueue<IAtomContainer> queries;
    private BlockingQueue<AmmoliteResult> results;
    private IStructDatabase db;
    
    private final int NUM_THREADS;
    private final int COARSE_QUEUE_SIZE;
    private static final int DEFAULT_COARSE_QUEUE_SIZE = 1000;
    private final int FINE_QUEUE_SIZE;
    private static final int DEFAULT_FINE_QUEUE_SIZE = 150;
    private final double COARSE_THRESH;
    private final double FINE_THRESH;
    

    
    public SingleSearcher( BlockingQueue<IAtomContainer> queries, 
                                BlockingQueue<AmmoliteResult> results, 
                                IStructDatabase db, 
                                int numThreads, double coarseThresh, double fineThresh) {
        this( queries, results, db, numThreads, 
              DEFAULT_COARSE_QUEUE_SIZE, DEFAULT_FINE_QUEUE_SIZE, 
                     coarseThresh, fineThresh);
        }
    
    public SingleSearcher( BlockingQueue<IAtomContainer> queries, 
                                BlockingQueue<AmmoliteResult> results, 
                                IStructDatabase db,
                                int numThreads, int coarseQueueSize, int fineQueueSize, 
                                double coarseThresh, double fineThresh) {
        
        this.NUM_THREADS = numThreads;
        this.COARSE_QUEUE_SIZE = coarseQueueSize;
        this.FINE_QUEUE_SIZE = fineQueueSize;
        this.COARSE_THRESH = coarseThresh;
        this.FINE_THRESH = fineThresh;
        this.queries = queries;
        this.results = results;
        this.db = db;
        
        }

    @Override
    public void run() {
        while( !Thread.currentThread().isInterrupted()){
            IAtomContainer query;
            try {
                query = queries.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                break;
            }
            if(query != null){
                Collection<IAtomContainer> matches = searchForMatches( query);
                AmmoliteResult result = new AmmoliteResult(query, matches);
                try {
                    results.put(result);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    
    public Collection<IAtomContainer> searchForMatches(IAtomContainer query){
        IMolStruct cQuery = db.makeMoleculeStruct(query);
        Collection<StructID> coarseHits = coarseSearch(cQuery);
        Collection<IAtomContainer> matches = fineSearch(query, coarseHits);
        return matches;
    }
    
    private Collection<StructID> coarseSearch(IMolStruct cQuery){
        CommandLineProgressBar bar = new CommandLineProgressBar(MolUtils.getStructID(cQuery).toString(), db.numReps());
        
        BlockingQueue<IMolStruct> queue = new ArrayBlockingQueue<IMolStruct>(COARSE_QUEUE_SIZE,false);
        Collection<StructID> hits = Collections.synchronizedCollection(new HashSet<StructID>());
        
        Thread producer = new Thread( new AmmoliteCoarseProducer(db.iterator(), queue));
        producer.start();
        
        List<Thread> consumers = new ArrayList<Thread>(NUM_THREADS);
        for(int i=0; i<NUM_THREADS; i++){
            Thread t = new Thread( new AmmoliteCoarseConsumer(cQuery, queue, hits, bar, COARSE_THRESH));
            consumers.add(t);
            t.start();
        }
        try {
            producer.join();
            while(queue.size() > 0){
                Thread.sleep(1000);
            }
            for(Thread t: consumers){
                t.interrupt();
            }
            for(Thread t: consumers){
                t.join();
            }
        } catch (InterruptedException e) {
            System.out.println("Exception from coarse call");
            e.printStackTrace();
        }

        return hits;
    }
    
    private Collection<IAtomContainer> fineSearch(IAtomContainer query, Collection<StructID> coarseHits){
        CommandLineProgressBar bar = new CommandLineProgressBar(MolUtils.getPubID(query).toString(), coarseHits.size()*12);
        
        BlockingQueue<IAtomContainer> queue = new ArrayBlockingQueue<IAtomContainer>(FINE_QUEUE_SIZE,false);
        Collection<IAtomContainer> matches = Collections.synchronizedCollection(new ArrayList<IAtomContainer>());
        
        Thread producer = new Thread( new AmmoliteFineProducer(coarseHits, db, queue));
        producer.start();
        List<Thread> consumers = new ArrayList<Thread>(NUM_THREADS);
        for(int i=0; i<NUM_THREADS; i++){
            Thread t = new Thread( new AmmoliteFineConsumer(query, queue, matches, bar, FINE_THRESH));
            consumers.add(t);
            t.start();
        }
        try {
            producer.join();
            while(queue.size() > 0){
                Thread.sleep(1000);
            }
            for(Thread t: consumers){
                t.interrupt();
            }
            for(Thread t: consumers){
                t.join();
            }
        } catch (InterruptedException e) {
            System.out.println("Exception from fine call");
            e.printStackTrace();
        }
        return matches;
    }
    
    
    
    
    
    

}
