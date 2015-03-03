package edu.mit.csail.ammolite.search;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import edu.mit.csail.ammolite.compression.IMolStruct;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.StructID;

public class AmmoliteCoarseConsumer implements Runnable {

    BlockingQueue<IMolStruct> queue;
    IMolStruct query;
    Collection<StructID> hits;
    CommandLineProgressBar bar;
    double threshold;
    
    public AmmoliteCoarseConsumer(IMolStruct cQuery, BlockingQueue<IMolStruct> queue, Collection<StructID> hits, CommandLineProgressBar bar, double threshold){
        this.query = cQuery;
        this.queue = queue;
        this.threshold = threshold;
        this.hits = hits;
        this.bar = bar;
    }

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()){
            try {
                IMolStruct target = queue.poll(500, TimeUnit.MILLISECONDS);
                
                if(target != null){
                    int overlap = MCS.getSMSDOverlap(target, query);
                    if(MCSUtils.overlapCoeff(overlap, target, query) > threshold){
                        hits.add(MolUtils.getStructID(target));
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


