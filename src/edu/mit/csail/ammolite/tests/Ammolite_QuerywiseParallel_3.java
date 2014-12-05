package edu.mit.csail.ammolite.tests;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.mcs.SMSD;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.Pair;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.StructID;

public class Ammolite_QuerywiseParallel_3 implements Tester {
    private static final String NAME = "Ammolite_QuerywiseParallel_Built_In_Timeout";
    private static final int COARSE_QUEUE_SIZE = 250;
    private static final int FINE_QUEUE_SIZE = 250;
    private static final int NUM_CONSUMERS = 2;
    private long TIMEOUT;
    private static final long DEF_TIMEOUT = 2000;
    private static final ExecutorService service = ParallelUtils.buildNewExecutorService();
        
    public Ammolite_QuerywiseParallel_3() {
        Scanner scanner = new Scanner( System.in );
        System.out.print( "Timeout in millis: " );
        String input = scanner.nextLine();
        try{
            TIMEOUT = Integer.parseInt(input);
        } catch (NumberFormatException ignore){
            TIMEOUT = DEF_TIMEOUT;
        }

        scanner.close();
        
    }

    @Override
    public void test(List<IAtomContainer> queries, IStructDatabase db,
            Iterator<IAtomContainer> targets, Iterator<MolStruct> sTargets,
            double thresh, double prob, String name, PrintStream out) {

        System.out.println("Using timeout of "+TIMEOUT);
        out.println("Using timeout of "+TIMEOUT);
        SearchResultDocumenter scribe = new SearchResultDocumenter( out);
        
        for(IAtomContainer query: queries){
            MolStruct cQuery = db.makeMoleculeStruct(query);
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
        service.shutdown();

    }
    

    
    private SearchResult fineSearch(IAtomContainer query, Collection<StructID> coarseHits, IStructDatabase db, double thresh){
        CommandLineProgressBar bar = new CommandLineProgressBar(MolUtils.getPubID(query).toString(), coarseHits.size()*12);
        SearchResult result = new SearchResult(query, getName());
        result.start();
        BlockingQueue<Future<SMSD>> queue = new ArrayBlockingQueue<Future<SMSD>>(FINE_QUEUE_SIZE,false);

        Thread producer = new Thread( new FineProducer(query, coarseHits, db, queue));
        producer.start();
        List<Thread> consumers = new ArrayList<Thread>(NUM_CONSUMERS);
        for(int i=0; i<NUM_CONSUMERS; i++){
            Thread t = new Thread( new FineConsumer(query, queue, result, bar, thresh));
            consumers.add(t);
            t.start();
        }
        try {
            producer.join();
            while(queue.size() > 0){
                if(Math.random() < 0.01){
                    System.out.println( "\n [Diagnostic] Queue Size:"+queue.size());
                }
                Thread.sleep(1000);
            }
            for(Thread t: consumers){
                t.interrupt();
            }
            for(Thread t: consumers){
                t.join();
            }
        } catch (InterruptedException e) {
            System.out.println("Fail from fine call");
            e.printStackTrace();
        }
        result.end();
        return result;
    }
    
    
    private Pair<SearchResult, Collection<StructID>> coarseSearch(MolStruct cQuery, IAtomContainer query, Iterator<MolStruct> sTargets, int numReps, double thresh){
        CommandLineProgressBar bar = new CommandLineProgressBar(MolUtils.getPubID(query).toString() + "_COARSE", numReps);
        SearchResult coarseResult = new SearchResult(cQuery, getName() + "_COARSE");
        coarseResult.start();
        BlockingQueue<Future<SMSD>> queue = new ArrayBlockingQueue<Future<SMSD>>(COARSE_QUEUE_SIZE,false);
        Collection<StructID> hits = Collections.synchronizedCollection(new HashSet<StructID>());
        Thread producer = new Thread( new CoarseProducer(cQuery, sTargets, queue));
        producer.start();
        List<Thread> consumers = new ArrayList<Thread>(NUM_CONSUMERS);
        for(int i=0; i<NUM_CONSUMERS; i++){
            Thread t = new Thread( new CoarseConsumer(cQuery, queue, coarseResult, hits, bar, thresh));
            consumers.add(t);
            t.start();
        }
        try {
            producer.join();
            while(queue.size() > 0){
                if(Math.random() < 0.01){
                    System.out.println( "\n [Diagnostic] Queue Size:"+queue.size());
                }
                Thread.sleep(1000);
            }
            for(Thread t: consumers){
                t.interrupt();
            }
            for(Thread t: consumers){
                t.join();
            }
        } catch (InterruptedException e) {
            System.out.println("Fail from coarse call");
            e.printStackTrace();
        }
        coarseResult.end();
        return new Pair<SearchResult, Collection<StructID>>(coarseResult, hits);
    }

    @Override
    public String getName() {
        return NAME;
    }
    
    private class CoarseProducer implements Runnable {
        BlockingQueue<Future<SMSD>> queue;
        Iterator<MolStruct> targets;
        MolStruct query;
        
        public CoarseProducer(MolStruct query, Iterator<MolStruct> sTargets, BlockingQueue<Future<SMSD>> queue){
              this.targets = sTargets;
            this.queue = queue;
            this.query = query;
        }

        @Override
        public void run() {
             while(targets.hasNext()){
                final MolStruct  target = targets.next();
                Future<SMSD> future = service.submit(new Callable<SMSD>(){
                    public SMSD call(){
                        SMSD smsd = new SMSD(query, target);
                        smsd.calculate();
                        return smsd;
                    }
                });
                try {
                    queue.offer(future, 1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } 
            }
            return;
        } 
    }
    
    private class FineProducer implements Runnable {
        BlockingQueue<Future<SMSD>> queue;
        Collection<StructID> hits;
        IStructDatabase db;
        IAtomContainer query;
        
        public FineProducer(IAtomContainer query, Collection<StructID> hits, IStructDatabase db, BlockingQueue<Future<SMSD>> queue){
            this.hits = hits;
            this.queue = queue;
            this.db = db;
            this.query = query;
        }

        @Override
        public void run() {
            for(StructID hit: hits){
                for(IAtomContainer target: db.getMatchingMolecules(hit)){
                    final IAtomContainer  fTarget = target;
                    Future<SMSD> future = service.submit(new Callable<SMSD>(){
                        public SMSD call(){
                            SMSD smsd = new SMSD(query, fTarget);
                            smsd.calculate();
                            return smsd;
                        }
                    });
                    try {
                        queue.offer(future, 1, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return;
        } 
    }
    
    private class CoarseConsumer implements Runnable {
        BlockingQueue<Future<SMSD>> queue;
        MolStruct query;
        SearchResult result;
        Collection<StructID> hits;
        CommandLineProgressBar bar;
        double threshold;
        
        public CoarseConsumer(MolStruct cQuery, BlockingQueue<Future<SMSD>> queue, SearchResult result, Collection<StructID> hits, CommandLineProgressBar bar, double threshold){
            this.query = cQuery;
            this.queue = queue;
            this.result = result;
            this.threshold = threshold;
            this.hits = hits;
            this.bar = bar;
        }

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    
                    Future<SMSD> future = queue.poll(500, TimeUnit.MILLISECONDS);
                    if(future != null){
                        SMSD smsd = null;
                        boolean timedOut = false;
                        try {
                            smsd = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
                        } catch (TimeoutException e) {
                            timedOut = true;
                        } catch (ExecutionException e){
                            timedOut = true;
                        }
                        

                        
                        if(!timedOut){
                            IAtomContainer target = smsd.getCompoundTwo();
                            if(target == query){
                                target = smsd.getCompoundOne();
                            }
                            
                            int overlap = smsd.size();
                            if(MCSUtils.overlapCoeff(overlap, target, query) > threshold){
                                result.addMatch(new SearchMatch(query, target, overlap));
                            } else {
                                result.addMiss(new SearchMiss(query, target, overlap));
                            }
                        } else {
                            //result.addTimeout(new SearchTimeout(query, target));
                        }
                        bar.event();
                    }
                } catch (InterruptedException end) {
                    break;
                }
            }
            return;
            
        }
        
    }
    
    private class FineConsumer implements Runnable {
        BlockingQueue<Future<SMSD>> queue;
        IAtomContainer query;
        SearchResult result;
        CommandLineProgressBar bar;
        double threshold;
        
        public FineConsumer(IAtomContainer query, BlockingQueue<Future<SMSD>> queue, SearchResult result, CommandLineProgressBar bar, double threshold){
            this.query = query;
            this.queue = queue;
            this.result = result;
            this.threshold = threshold;
            this.bar = bar;
        }

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    Future<SMSD> future = queue.poll(500, TimeUnit.MILLISECONDS);
                    if(future != null){
                        SMSD smsd = null;
                        
                        boolean timedOut = false;
                        try {
                            smsd = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
                        } catch (TimeoutException e) {
                            timedOut = true;
                        } catch (ExecutionException e){
                            timedOut = true;
                        }
                        
                        if(!timedOut){
                            IAtomContainer target = smsd.getCompoundTwo();
                            if(target == query){
                                target = smsd.getCompoundOne();
                            }
                            
                            int overlap = smsd.size();
                            if(MCSUtils.overlapCoeff(overlap, target, query) > threshold){
                                result.addMatch(new SearchMatch(query, target, overlap));
                            } else {
                                result.addMiss(new SearchMiss(query, target, overlap));
                            }
                        } else {
                            //result.addTimeout(new SearchTimeout(query, target));
                        }
                        bar.event();
                    }
                } catch (InterruptedException end) {
                    break;
                }
            }
            return;
            
        }
        
    }

}
