package edu.mit.csail.ammolite.search;

import java.io.PrintStream;
import java.util.ArrayList;
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

public class SMSDSearcher {
    private static final int QUEUE_SIZE = 50000;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors()/2;
    IResultHandler resultHandler = null;
    
        
    public SMSDSearcher() {}

    public void search(IAtomContainer query, List<String> sdfFiles, double thresh, IResultHandler resultHandler) {
            int approxNumTargets = 0;
            for(String sdfFile: sdfFiles){
                approxNumTargets += SDFUtils.estimateNumMolsInSDF(sdfFile);
            }
            Iterator<IAtomContainer> targets = SDFUtils.parseSDFSetOnline(sdfFiles);
            this.resultHandler = resultHandler;
            
            search(query, targets, thresh, approxNumTargets);
    }
    
    public void search(IAtomContainer query, Iterator<IAtomContainer> targets, double thresh, IResultHandler resultHandler){
        this.resultHandler = resultHandler;
        search(query, targets, thresh, 0);
    }
    
    public void search(IAtomContainer query, Iterator<IAtomContainer> targets, double thresh, int numMols){

        ExecutorService ecs = ParallelUtils.buildNewExecutorService(NUM_THREADS);
        CommandLineProgressBar bar = new CommandLineProgressBar(MolUtils.getPubID(query).toString(), numMols);
        
        BlockingQueue<IAtomContainer> queue = new ArrayBlockingQueue<IAtomContainer>(QUEUE_SIZE,false);
        Mediator<IAtomContainer> mediator = new Mediator<IAtomContainer>( queue);
        Future<?> producerStatus = ecs.submit( new Producer(targets, mediator));
        List<Future<?>> consumers = new ArrayList<Future<?>>(NUM_THREADS);
        try {
            Thread.sleep(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(int i=0; i<NUM_THREADS; i++){
            consumers.add( ecs.submit( new Consumer(query, mediator, bar, thresh)));
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
        CommandLineProgressBar bar;
        double threshold;
        
        public Consumer(IAtomContainer query, Mediator<IAtomContainer> queue, CommandLineProgressBar bar, double threshold){
            this.query = query;
            this.queue = queue;
            this.threshold = threshold;
            this.bar = bar;
        }

        @Override
        public void run() {
            try{
                IAtomContainer target = queue.get();
                while(queue.adding && target != null){
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
