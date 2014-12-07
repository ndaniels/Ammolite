package edu.mit.csail.ammolite.compression;

import java.util.HashMap;
import java.util.Map;

import edu.ucla.sspace.graph.SparseWeightedGraph;
import edu.ucla.sspace.graph.WeightedEdge;

public class LabeledWeightedGraph extends SparseWeightedGraph {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Map<Integer, String> labels = new HashMap<Integer, String>();

    public LabeledWeightedGraph() {
        // TODO Auto-generated constructor stub
    }

    public boolean add(int arg0, String label) {
        labels.put(arg0, label);
        return super.add(arg0);
    }


    @Override
    public void clear() {
        labels.clear();
        super.clear();
    }
    
    public String labelOf(int arg0){
        return labels.get(arg0);
    }

}
