package edu.mit.csail.ammolite.search;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.MCSUtils;

public class AmmoliteFineConsumer implements Runnable {
    BlockingQueue<IAtomContainer> queue;
    Collection<IAtomContainer> matches;
    IAtomContainer query;
    CommandLineProgressBar bar;
    double threshold;
    
    public AmmoliteFineConsumer(IAtomContainer query, BlockingQueue<IAtomContainer> queue, Collection<IAtomContainer> matches, CommandLineProgressBar bar, double threshold){
        this.query = query;
        this.queue = queue;
        this.matches = matches;
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
                        matches.add(target);
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
