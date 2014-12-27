package edu.mit.csail.ammolite.search;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

import edu.mit.csail.ammolite.compression.IMolStruct;

public class AmmoliteCoarseProducer implements Runnable {

    BlockingQueue<IMolStruct> queue;
    Iterator<IMolStruct> targets;
    
    public AmmoliteCoarseProducer(Iterator<IMolStruct> sTargets, BlockingQueue<IMolStruct> queue){
        this.targets = sTargets;
        this.queue = queue;
    }

    @Override
    public void run() {
        IMolStruct target;
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
