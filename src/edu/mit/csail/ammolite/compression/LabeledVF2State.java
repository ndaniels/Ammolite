package edu.mit.csail.ammolite.compression;

import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.isomorphism.VF2State;

public class LabeledVF2State extends VF2State {
    protected LabeledWeightedGraph lg1;
    protected LabeledWeightedGraph lg2;

    
    public LabeledVF2State(VF2State copy) {
        super(copy);
        this.lg1 = ((LabeledVF2State) copy).labeledGraph1();
        this.lg2 = ((LabeledVF2State) copy).labeledGraph2();
    }

    public LabeledVF2State(LabeledWeightedGraph g1, LabeledWeightedGraph g2) {
        super(g1, g2);
        this.lg1 = g1;
        this.lg2 = g2;
    } 
    
    @Override
    protected boolean areCompatableVertices(int v1, int v2){
        //System.out.println(lg1.labelOf(v1) +" "+lg2.labelOf(v2));
        if(lg1.labelOf(v1).equals(lg2.labelOf(v2))){
            return true;
        }
        return false;
    }
    
    @Override
    public boolean isFeasiblePair(int node1, int node2) {
        if( areCompatableVertices(node1, node2)){
            return super.isFeasiblePair(node1, node2);
        }
        return false;
    }
    
    public LabeledWeightedGraph labeledGraph1(){
        return lg1;
    }
    
    public LabeledWeightedGraph labeledGraph2(){
        return lg2;
    }
    
    @Override
    public VF2State copy(){
        return new LabeledVF2State(this);
    }

}
