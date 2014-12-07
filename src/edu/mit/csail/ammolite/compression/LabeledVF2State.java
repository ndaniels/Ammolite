package edu.mit.csail.ammolite.compression;

import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.isomorphism.VF2State;

public class LabeledVF2State extends VF2State {
    protected LabeledWeightedGraph g1;
    protected LabeledWeightedGraph g2;

    
    public LabeledVF2State(VF2State copy) {
        this(((LabeledVF2State) copy).g1, ((LabeledVF2State) copy).g2);
    }

    public LabeledVF2State(LabeledWeightedGraph g1, LabeledWeightedGraph g2) {
        super(g1, g2);
        this.g1 = g1;
        this.g2 = g2;
    } 
    
    @Override
    protected boolean areCompatableVertices(int v1, int v2){
        System.out.print("called: ");
        if(g1.labelOf(v1).equals(g2.labelOf(v2))){
            System.out.println("true");
            return true;
        }
        System.out.println("false");
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
        return g1;
    }
    
    public LabeledWeightedGraph labeledGraph2(){
        return g2;
    }

}
