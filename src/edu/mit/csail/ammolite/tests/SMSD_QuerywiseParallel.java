package edu.mit.csail.ammolite.tests;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.IMolStruct;
import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.SDFMultiParser;
import edu.mit.csail.ammolite.utils.SDFUtils;

public class SMSD_QuerywiseParallel implements Tester{
    private static final String NAME = "SMSD_QuerywiseParallel_Timeout";
    private static final int QUEUE_SIZE = 500;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors()/2;
        
    public SMSD_QuerywiseParallel() {}

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

        Thread producer = new Thread( new Producer(targets, queue));
        producer.start();
        
        List<Thread> consumers = new ArrayList<Thread>(NUM_THREADS);
        for(int i=0; i<NUM_THREADS; i++){
            Thread t = new Thread( new Consumer(query, queue, result, bar, thresh));
            consumers.add(t);
            t.start();
        }
        
        try {
            producer.join();
            while(queue.size() > 0){
                if(Math.random() < 0.01){
                    System.out.println( "\n [Diagnostic] Queue Size:"+queue.size()+"\n");
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
        BlockingQueue< IAtomContainer> queue;
        Iterator<IAtomContainer> targets;
        
        public Producer(Iterator<IAtomContainer> targets, BlockingQueue< IAtomContainer> queue){
            this.queue = queue;
            this.targets = targets;
        }

        @Override
        public void run() {
            IAtomContainer target;
            while(targets.hasNext()){
                target = targets.next();
                try {
                    queue.offer(target, 1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return;
        } 
    }
    

    
    private class Consumer implements Runnable {
        BlockingQueue<IAtomContainer> queue;
        IAtomContainer query;
        SearchResult result;
        CommandLineProgressBar bar;
        double threshold;
        
        public Consumer(IAtomContainer query, BlockingQueue<IAtomContainer> queue, SearchResult result, CommandLineProgressBar bar, double threshold){
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
                    IAtomContainer target = queue.poll(500, TimeUnit.MILLISECONDS);
                    if(target != null){
                        int overlap = MCS.getSMSDOverlap(target, query);
                        if(MCSUtils.overlapCoeff(overlap, target, query) > threshold){
                            result.addMatch(new SearchMatch(query, target, overlap));
                        } else {
                            result.addMiss(new SearchMiss(query, target, overlap));
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
