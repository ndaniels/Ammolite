package edu.mit.csail.ammolite.search;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.utils.StructID;

public class AmmoliteFineProducer implements Runnable {
    BlockingQueue< IAtomContainer> queue;
    Collection<StructID> hits;
    IStructDatabase db;
    
    public AmmoliteFineProducer(Collection<StructID> hits, IStructDatabase db, BlockingQueue< IAtomContainer> queue){
        this.hits = hits;
        this.queue = queue;
        this.db = db;
    }

    @Override
    public void run() {
        for(StructID hit: hits){
            for(IAtomContainer target: db.getMatchingMolecules(hit)){
                try {
                    queue.put( target);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return;
    } 
}
