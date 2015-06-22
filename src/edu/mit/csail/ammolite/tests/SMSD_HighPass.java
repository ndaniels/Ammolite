package edu.mit.csail.ammolite.tests;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.IMolStruct;
import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.mcs.SMSD;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.SDFMultiParser;
import edu.mit.csail.ammolite.utils.SDFUtils;

public class SMSD_HighPass implements Tester{
    private static final String NAME = "SMSD_HighPass";
    private static final int QUEUE_SIZE = 50000;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors()/2;
    private static final ExecutorService ecs = ParallelUtils.buildNewExecutorService(NUM_THREADS);
        
    public SMSD_HighPass() {}

    @Override
    public void test(List<IAtomContainer> queries, IStructDatabase db,
            Iterator<IAtomContainer> targets, Iterator<IMolStruct> sTargets,
            double thresh, double prob, String name, PrintStream out) {
        
        SearchResultDocumenter scribe = new SearchResultDocumenter( out);
        List<String> sdfFiles = db.getSourceFiles().getFilepaths();

        for(IAtomContainer query: queries){
          
            Iterator<IAtomContainer> realTargets = targets = SDFUtils.parseSDFSetOnline(sdfFiles);
            SearchResult result = search(query, realTargets, thresh, db.numMols());
            System.out.println(" Writing results...");
            scribe.documentSingleResult(result);
            result = null;
        }

    }
    

    
    private SearchResult search(IAtomContainer query, Iterator<IAtomContainer> targets, double thresh, int numMols){
        
        CommandLineProgressBar bar = new CommandLineProgressBar(MolUtils.getPubID(query).toString(), numMols);
        SearchResult result = new SearchResult(query, getName());
        result.start();
        
        BlockingQueue<IAtomContainer> queue = new ArrayBlockingQueue<IAtomContainer>(QUEUE_SIZE,false);
        Mediator<IAtomContainer> mediator = new Mediator<IAtomContainer>(queue);

        Thread producer = new Thread( new Producer(targets, mediator));
        producer.start();
        
        List<Thread> consumers = new ArrayList<Thread>(NUM_THREADS);
        for(int i=0; i<NUM_THREADS; i++){
            Thread t = new Thread( new Consumer(query, mediator, result, bar, thresh));
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
            System.out.println("Failed from search call");
            e.printStackTrace();
        }
        result.end();
        return result;
    }
    
    


    @Override
    public String getName() {
        return NAME;
    }
    
    
    private static class Producer implements Runnable {
        Mediator< IAtomContainer> queue;
        Iterator<IAtomContainer> targets;
        
        public Producer(Iterator<IAtomContainer> targets, Mediator< IAtomContainer> queue){
            this.queue = queue;
            this.targets = targets;
        }

        @Override
        public void run() {
            IAtomContainer target;
            while(targets.hasNext()){
                target = targets.next();
                try {
                    queue.put(target);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return;
        } 
    }
    

    
    private class Consumer implements Runnable {
        Mediator<IAtomContainer> queue;
        IAtomContainer query;
        SearchResult result;
        CommandLineProgressBar bar;
        double threshold;
        
        public Consumer(IAtomContainer query, Mediator<IAtomContainer> queue, SearchResult result, CommandLineProgressBar bar, double threshold){
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
                    if(target != null){
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
                                result.addMatch(new SearchMatch(query, target, overlap));
                            }
                        }
                        bar.event();
                        target = queue.get();
                    }
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