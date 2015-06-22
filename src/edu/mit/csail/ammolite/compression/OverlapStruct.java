package edu.mit.csail.ammolite.compression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IBond.Order;
import org.openscience.cdk.interfaces.IBond.Stereo;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IChemObjectChangeEvent;
import org.openscience.cdk.interfaces.IChemObjectListener;
import org.openscience.cdk.interfaces.IElectronContainer;
import org.openscience.cdk.interfaces.ILonePair;
import org.openscience.cdk.interfaces.ISingleElectron;
import org.openscience.cdk.interfaces.IStereoElement;

import edu.mit.csail.ammolite.utils.FloydsAlgorithm;
import edu.mit.csail.ammolite.utils.Pair;
import edu.mit.csail.ammolite.utils.PubchemID;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.isomorphism.AbstractIsomorphismTester;
import edu.ucla.sspace.matrix.Matrix;

public class OverlapStruct extends LabeledMolStruct implements IMolStruct  {
    
    private static final long serialVersionUID = 1L;
    private  double MIN_OVERLAP;
    
    public OverlapStruct(IAtomContainer base, double minOverlap ){
        super(base);
        this.MIN_OVERLAP = minOverlap;
        modifyGraph();
    }
    
    
    protected void modifyGraph(){
        removeShallowNodes( sortAveConnections( getNodesWithAveConnections()));
    }
    
    protected List<Pair<Integer,Double>> getNodesWithAveConnections(){
        assert this.getAtomCount() == this.getGraph().order();
        Matrix allPairsDistance = FloydsAlgorithm.computeAllPairsDistance(getGraph());
        List<Pair<Integer,Double>> aveNodes = new ArrayList<Pair<Integer,Double>>();
        //int numNodes = this.getAtomCount();
        int numNodes = 1;
        
        for(Integer node: this.getGraph().vertices()){
            double[]  pathLengths = allPairsDistance.getRow(node);
            double totalPathLength = 0;
            for(double pathLength: pathLengths){
                if(!Double.isNaN(pathLength) && !Double.isInfinite(pathLength)){
                    totalPathLength += pathLength;
                }
            }
            double avePathLength = totalPathLength / numNodes;
            aveNodes.add( new Pair<Integer,Double>(node, avePathLength));
        }
        return aveNodes;
    }
    
    protected List<Pair<Integer,Double>> sortAveConnections(List<Pair<Integer,Double>> aveNodes){
        
        Collections.sort(aveNodes, new Comparator<Pair<Integer,Double>>(){

            @Override
            public int compare(Pair<Integer, Double> p1, Pair<Integer, Double> p2) {
                if(Math.abs(p2.right() - p1.right()) < 0.000001){
                    return 0;
                } else if(p1.right() < p2.right()){
                    return -1;
                } else {
                    return 1;
                }
            }

        });
        return aveNodes;
    }
    
    protected void removeShallowNodes(List<Pair<Integer,Double>> sortedAveNodes){
        int atDepth = 1;
        double nodesKept = 0.0;
        int originalNodes = this.getAtomCount();
        boolean removeRest = false;
        if(sortedAveNodes.size() > 0){
            double currentDepth = sortedAveNodes.get(0).right();
            
            for(Pair<Integer,Double> aveNode: sortedAveNodes){
                if(removeRest){
                    IAtom atom = this.nodesToAtoms.get(aveNode.left());
                    this.removeAtom( atom);
                    for(IBond bond: getConnectedBondsList( atom)){
                        removeBond(bond);
                    }
                    
                } else if(Math.abs(aveNode.right() - currentDepth) > 0.000001 ){
                    atDepth++;
                    currentDepth = aveNode.right();
                    if( nodesKept/originalNodes >= MIN_OVERLAP){
                        IAtom atom = this.nodesToAtoms.get(aveNode.left());
                        this.removeAtom( atom);
                        for(IBond bond: getConnectedBondsList( atom)){
                            removeBond(bond);
                        }
                        removeRest = true;
                        //System.out.println("Started removing nodes at depth "+atDepth);
                    } else {
                        nodesKept += 1.0;
                    }
                } else {
                   nodesKept += 1.0;
                }
                
            }
        } else {
            System.out.println("Zero Length Node List");
        }
        //System.out.println("Kept "+nodesKept+" of "+originalNodes+" --> "+nodesKept/originalNodes);
    }

}

