package edu.mit.csail.ammolite.tests;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.smsd.Isomorphism;
import org.openscience.smsd.interfaces.Algorithm;

import edu.mit.csail.ammolite.compression.IMolStruct;
import edu.mit.csail.ammolite.compression.LabeledMolStruct;
import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.mcs.StringApproximator;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.Pair;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.StructID;

public class Ammolite_QuerywiseParallel_2 implements Tester {
    private static final String NAME = "Ammolite";
    private static final int COARSE_QUEUE_SIZE = 1000;
    private static final int FINE_QUEUE_SIZE = 1000;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors()/2;
        
    public Ammolite_QuerywiseParallel_2() {}

    @Override
    public void test(List<IAtomContainer> queries, IStructDatabase db,
            Iterator<IAtomContainer> targets, Iterator<IMolStruct> sTargets,
            double thresh, double prob, String name, PrintStream out) {

        System.out.println(NAME);
        SearchResultDocumenter scribe = new SearchResultDocumenter( out);
        
        for(IAtomContainer query: queries){
            IMolStruct cQuery = db.makeMoleculeStruct(query);
            Pair<SearchResult, Collection<StructID>> p = coarseSearch(cQuery, query, db.iterator(), db.numReps(), prob);
            
            SearchResult coarseResult = p.left();
            Collection<StructID> coarseHits = p.right();
            System.out.println("Writing coarse results...");
            scribe.documentSingleResult(coarseResult);
            // Make sure the JVM automatically garbage collects these bad boys
            p = null;
            coarseResult = null;
            
            SearchResult fineResult = fineSearch(query, coarseHits, db, thresh);
            scribe.documentSingleResult(fineResult);
            System.out.println("Writing fine results...");
            fineResult = null;
        }

    }
    

    
    private SearchResult fineSearch(IAtomContainer query, Collection<StructID> coarseHits, IStructDatabase db, double thresh){
        ExecutorService ecs = ParallelUtils.buildNewExecutorService(NUM_THREADS);
        CommandLineProgressBar bar = new CommandLineProgressBar(MolUtils.getPubID(query).toString(), coarseHits.size()*12);
        SearchResult result = new SearchResult(query, getName());
        result.start();
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
            consumers.add( ecs.submit(new FineConsumer(query, mediator, result, bar, thresh)));
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
        result.end();
        return result;
    }
    
    
    private Pair<SearchResult, Collection<StructID>> coarseSearch(IMolStruct cQuery, IAtomContainer query, Iterator<IMolStruct> sTargets, int numReps, double thresh){
        ExecutorService ecs = ParallelUtils.buildNewExecutorService(NUM_THREADS);
        CommandLineProgressBar bar = new CommandLineProgressBar(MolUtils.getPubID(query).toString() + "_COARSE", numReps);
        SearchResult coarseResult = new SearchResult(cQuery, getName() + "_COARSE");
        coarseResult.start();
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
            consumers.add( ecs.submit( new CoarseConsumer(cQuery, mediator, coarseResult, hits, bar, thresh)));
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
        coarseResult.end();
        return new Pair<SearchResult, Collection<StructID>>(coarseResult, hits);
    }

    @Override
    public String getName() {
        return NAME;
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
        SearchResult result;
        Collection<StructID> hits;
        CommandLineProgressBar bar;
        double threshold;
        
        public CoarseConsumer(IMolStruct cQuery, Mediator<IMolStruct> queue, SearchResult result, Collection<StructID> hits, CommandLineProgressBar bar, double threshold){
            try {
                this.query = (IAtomContainer) cQuery.clone();
            } catch (CloneNotSupportedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this.queue = queue;
            this.result = result;
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
                        int overlap = MCS.getSMSDOverlap(target, query);
                        double overlapCoeff = MCSUtils.overlapCoeff(overlap, target, query);
                        if(overlapCoeff > threshold){
                            result.addMatch(new SearchMatch(query, target, overlap));
                            hits.add(MolUtils.getStructID(target));
                        } else {
                            result.addMiss(new SearchMiss(query, target, overlap));
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
        SearchResult result;
        CommandLineProgressBar bar;
        double threshold;
        
        public FineConsumer(IAtomContainer query, Mediator<IAtomContainer> queue, SearchResult result, CommandLineProgressBar bar, double threshold){
            //try {
                this.query = (IAtomContainer) query;
//            } catch (CloneNotSupportedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
            this.queue = queue;
            this.result = result;
            this.threshold = threshold;
            this.bar = bar;
        }

        @Override
        public void run() {
            try{
                IAtomContainer target = queue.get();
                while(queue.adding || target != null){
                    int overlap = MCS.getSMSDOverlap(target, query);
                    if(MCSUtils.overlapCoeff(overlap, target, query) > threshold){
                        result.addMatch(new SearchMatch(query, target, overlap));
                    } else {
                        result.addMiss(new SearchMiss(query, target, overlap));
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
