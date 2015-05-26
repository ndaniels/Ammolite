package edu.mit.csail.ammolite.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.IMolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.mcs.SMSD;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.Pair;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.StructID;

public class Ammolite{
    private static final String COARSE_NAME = "Ammolite_Coarse";
    private static final String NAME = "Ammolite";
    private static final int COARSE_QUEUE_SIZE = 1000;
    private static final int FINE_QUEUE_SIZE = 50000;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors()/2;
    private IResultHandler resultHandler;
        
    public Ammolite() {}

    public void search(IAtomContainer query, IStructDatabase db, double fineThresh, double coarseThresh, IResultHandler resultHandler) {
        System.out.println(NAME);
        
        this.resultHandler = resultHandler;
        IMolStruct cQuery = db.makeMoleculeStruct(query);
        
        Collection<StructID> coarseHits = coarseSearch(cQuery, query, db.iterator(), db.numReps(), coarseThresh);
        fineSearch(query, coarseHits, db, fineThresh);

    }
    

    
    private void fineSearch(IAtomContainer query, Collection<StructID> coarseHits, IStructDatabase db, double thresh){
        ExecutorService ecs = ParallelUtils.buildNewExecutorService(NUM_THREADS);
        CommandLineProgressBar bar = new CommandLineProgressBar(MolUtils.getPubID(query).toString(), db.countFineHits(coarseHits));
        
        BlockingQueue<IAtomContainer> queue = new ArrayBlockingQueue<IAtomContainer>(FINE_QUEUE_SIZE,false);
        Mediator<IAtomContainer> mediator = new Mediator<IAtomContainer>( queue);
        Future<?> producerStatus = ecs.submit(new FineProducer(coarseHits, db, mediator));
        List<Future<?>> consumers = new ArrayList<Future<?>>(NUM_THREADS);
        try {
            Thread.sleep(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(int i=0; i<NUM_THREADS; i++){
            consumers.add( ecs.submit(new FineConsumer(query, mediator, bar, thresh)));
        }
        try {
            producerStatus.get();
            for(Future<?> f: consumers){
                f.get();
            }
        } catch (InterruptedException ie){ 
            ie.printStackTrace();
        } catch(ExecutionException ee) {
            ee.printStackTrace();
        }
        ecs.shutdown();
        bar.done();
    }
    
    
    private Collection<StructID> coarseSearch(IMolStruct cQuery, IAtomContainer query, Iterator<IMolStruct> sTargets, int numReps, double thresh){
        ExecutorService ecs = ParallelUtils.buildNewExecutorService(NUM_THREADS);
        CommandLineProgressBar bar = new CommandLineProgressBar(MolUtils.getPubID(query).toString() + "_COARSE", numReps);

        BlockingQueue<IMolStruct> queue = new ArrayBlockingQueue<IMolStruct>(COARSE_QUEUE_SIZE,false);
        Mediator<IMolStruct> mediator = new Mediator<IMolStruct>(queue);
        Future<?> producerStatus = ecs.submit( new CoarseProducer(sTargets, mediator));
        List<Future<?>> consumers = new ArrayList<Future<?>>(NUM_THREADS);
        try {
            Thread.sleep(0);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Collection<StructID> hits = Collections.synchronizedCollection(new HashSet<StructID>(1000));
        for(int i=0; i<NUM_THREADS; i++){
            consumers.add( ecs.submit( new CoarseConsumer(cQuery, mediator, hits, bar, thresh)));
        }
        try {
            producerStatus.get();
            for(Future<?> f: consumers){
                f.get();
            }
        } catch (InterruptedException ie){ 
            ie.printStackTrace();
        } catch(ExecutionException ee) {
            ee.printStackTrace();
        }
        ecs.shutdown();
        bar.done();
        return hits;
    }


    
    //////////////////////////////////////////////////////////////////////
    // COARSE
    //////////////////////////////////////////////////////////////////////
    
    private class CoarseProducer implements Runnable {
        Mediator<IMolStruct> queue;
        Iterator<IMolStruct> targets;
        
        public CoarseProducer(Iterator<IMolStruct> sTargets, Mediator<IMolStruct> queue){
              this.targets = sTargets;
            this.queue = queue;
        }

        @Override
        public void run() {
            IMolStruct target;
             while(targets.hasNext()){
                target = targets.next();
                try {
                    queue.put(target);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } 
            }
            queue.adding = false; 
            return;
        } 
    }

    
    private class CoarseConsumer implements Runnable {
        Mediator<IMolStruct> queue;
        IAtomContainer query;
        Collection<StructID> hits;
        CommandLineProgressBar bar;
        double threshold;
        
        public CoarseConsumer(IMolStruct cQuery, Mediator<IMolStruct> queue,  Collection<StructID> hits, CommandLineProgressBar bar, double threshold){
            try {
                this.query = (IAtomContainer) cQuery.clone();
            } catch (CloneNotSupportedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this.queue = queue;
            this.threshold = threshold;
            this.hits = hits;
            this.bar = bar;
        }

        @Override
        public void run() {
            try{
                IMolStruct target = queue.get();
                while(queue.adding || target != null){
                    if(target != null){
                        int targetSize = MCSUtils.getAtomCountNoHydrogen(target);
                        int querySize = MCSUtils.getAtomCountNoHydrogen(query);
                        double smaller = Math.min(targetSize, querySize);
                        double larger  = Math.max(targetSize, querySize);
                        double upperTanimoto = smaller / larger;
                        if( upperTanimoto > threshold){
                            int overlap = MCS.getSMSDOverlap(target, query);
                            double overlapCoeff = MCSUtils.tanimotoCoeff(overlap, targetSize, querySize);
                            if(overlapCoeff > threshold){
                                resultHandler.handleCoarse(new SearchMatch(query, target, overlap));
                                hits.add(MolUtils.getStructID(target));
                            } 
                        }
                    } 
                    bar.event();
                    target = queue.get();
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            return;  
        }
        
    }
    
    //////////////////////////////////////////////////////////////////////
    // FINE
    //////////////////////////////////////////////////////////////////////
    
    private class FineProducer implements Runnable {
        Mediator< IAtomContainer> queue;
        Collection<StructID> hits;
        IStructDatabase db;
        
        public FineProducer(Collection<StructID> hits, IStructDatabase db, Mediator< IAtomContainer> queue){
            this.hits = hits;
            this.queue = queue;
            this.db = db;
        }

        @Override
        public void run() {
            for(StructID hit: hits){
                for(IAtomContainer target: db.getMatchingMolecules(hit)){
                    try {
                        queue.put(target);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            queue.adding = false;
            return;
        } 
    }
    
    private class FineConsumer implements Runnable {
        Mediator<IAtomContainer> queue;
        IAtomContainer query;
        CommandLineProgressBar bar;
        double threshold;
        
        public FineConsumer(IAtomContainer query, Mediator<IAtomContainer> queue, CommandLineProgressBar bar, double threshold){
            this.query = (IAtomContainer) query;
            this.queue = queue;
            this.threshold = threshold;
            this.bar = bar;
        }

        @Override
        public void run() {
            try{
                IAtomContainer target = queue.get();
                while(queue.adding || target != null){
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
                            SearchMatch match = new SearchMatch(query, target, overlap);
                            if( resultHandler.recordingStructures()){
                                match.setMCS(smsd.getFirstSolution());
                            }
                            resultHandler.handleFine(match);
                        }
                    }
                    bar.event();
                    target = queue.get();
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            return;
            
        }
        
    }
    
    private class Mediator<T> {
        private BlockingQueue<T> queue;
        public boolean adding = true;
        
        public Mediator(BlockingQueue<T> queue){
            this.queue = queue;
        }
        
        public void put(T data) throws InterruptedException{
            queue.put(data);
        }
        
        public T get() throws InterruptedException{
            return this.queue.poll(1, TimeUnit.SECONDS);
        }
        
    }

}
