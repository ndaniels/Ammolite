package edu.mit.csail.ammolite.compression;

import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.isomorphism.AbstractIsomorphismTester;
import edu.ucla.sspace.graph.isomorphism.State;
import edu.ucla.sspace.graph.isomorphism.VF2State;

public class LabeledVF2IsomorphismTester extends AbstractIsomorphismTester {

    public LabeledVF2IsomorphismTester() {
        // TODO Auto-generated constructor stub
    }

    @Override
    protected State makeInitialState(Graph<? extends Edge> arg0,
            Graph<? extends Edge> arg1) {
        if(arg0 instanceof LabeledWeightedGraph && arg1 instanceof LabeledWeightedGraph){
            System.out.println("LABELED");
            return new LabeledVF2State((LabeledWeightedGraph) arg0, (LabeledWeightedGraph) arg1);
        }
        return new VF2State(arg0, arg1);
    }

}
