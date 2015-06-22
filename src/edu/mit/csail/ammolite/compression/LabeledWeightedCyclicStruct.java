package edu.mit.csail.ammolite.compression;

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

public class LabeledWeightedCyclicStruct extends LabeledWeightedMolStruct {

    public LabeledWeightedCyclicStruct() {
        // TODO Auto-generated constructor stub
    }

    public LabeledWeightedCyclicStruct(IAtomContainer base) {
        super(base);
        this.removeOnePrimeCarbons();
        this.makeGraph();
        this.setFingerprint();
    }

    protected void removeOnePrimeCarbons(){
        if(graph.size() +1 == graph.order() ){
            this.removeAllBonds();
            graph.clearEdges();
        } // If a molecule is a tree (most alkanes, etc) represent it only is a disperse set of points.
        else {
            ArrayList<IAtom> toRemove = new ArrayList<IAtom>();
            ArrayList<IBond> bondsToRemove = new ArrayList<IBond>();
            do {
                for(IAtom atom: toRemove){
                    removeAtom(atom);
                }

                for(IBond bond: bondsToRemove){
                    removeBond(bond);
                }
                
                toRemove.clear();
                bondsToRemove.clear();
                
                for(int i: graph.vertices()){
                    if(graph.degree(i) == 1){
                        toRemove.add(nodesToAtoms.get(i));
                        bondsToRemove.addAll(getConnectedBondsList( nodesToAtoms.get(i)));
                    }
                }

                
            } while(toRemove.size() != 0);
        }
        if(graph.order() != this.getAtomCount()){
            System.out.println("!!! mismatch !!!");
        }
    }
}
