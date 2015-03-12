package edu.mit.csail.ammolite.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

public class Ammolite_Fine_Only implements Tester {
    private static final String NAME = "Ammolite_Tanimoto_Fine_Only";
    private static final int FINE_QUEUE_SIZE = 50000;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors()/2;
        
    public Ammolite_Fine_Only() {}

    @Override
    public void test(List<IAtomContainer> queries, IStructDatabase db,
            Iterator<IAtomContainer> targets, Iterator<IMolStruct> sTargets,
            double thresh, double prob, String name, PrintStream out) {

        System.out.println(NAME);
        SearchResultDocumenter scribe = new SearchResultDocumenter( out);
        
        for(IAtomContainer query: queries){
            
            Collection<StructID> coarseHits = loadCoarseHits();
            
            SearchResult fineResult = fineSearch(query, coarseHits, db, thresh);
            scribe.documentSingleResult(fineResult);
            System.out.println(" Writing fine results...");
            fineResult = null;
        }

    }
    
    private Collection<StructID> loadCoarseHits(){
        Collection<StructID> hits = Collections.synchronizedCollection(new HashSet<StructID>(1000));
        
        try{
            BufferedReader br =  new BufferedReader(new InputStreamReader(System.in));
     
            String input;
     
            while((input=br.readLine())!=null){
                for(String id: input.split(" ")){
                    if(id.matches("[0-9]+_STRUCT")){
                        hits.add(new StructID(id));
                    }
                }
            }
     
        }catch(IOException io){
            io.printStackTrace();
        }
        
        return hits;
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
    
    

    @Override
    public String getName() {
        return NAME;
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
            this.query = query;
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
                    if(MCSUtils.tanimotoCoeff(overlap, target, query) > threshold){
                        result.addMatch(new SearchMatch(query, target, overlap));
                    } 
                    bar.event();
                    target = queue.get();
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            System.out.println("empty queue");
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