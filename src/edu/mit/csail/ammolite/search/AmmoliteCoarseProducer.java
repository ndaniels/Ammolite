package edu.mit.csail.ammolite.search;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

import edu.mit.csail.ammolite.compression.MolStruct;

public class AmmoliteCoarseProducer implements Runnable {

    BlockingQueue< MolStruct> queue;
    Iterator<MolStruct> targets;
    
    public AmmoliteCoarseProducer(Iterator<MolStruct> sTargets, BlockingQueue< MolStruct> queue){
        this.targets = sTargets;
        this.queue = queue;
    }

    @Override
    public void run() {
        MolStruct target;
         while(targets.hasNext()){
            target = targets.next();
            try {
                queue.put( target);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } 
        }
        return;
    }

}
