package edu.mit.csail.ammolite.compression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.SimpleEdge;
import edu.ucla.sspace.graph.SparseUndirectedGraph;

public class CyclicStruct extends MoleculeStruct implements Serializable{

	public CyclicStruct(){
		
	}
	
	public CyclicStruct(IAtomContainer base) {
		super(base);
		removeOnePrimeCarbons();
		setHash();
		rebaseGraph();
		
	}
	
	protected void rebaseGraph(){
		SparseUndirectedGraph newGraph = new SparseUndirectedGraph();
		
		int outer = 0;
		for(int i: graph.vertices()){
			newGraph.add(outer);
			int inner = 0;
			for(int j: graph.vertices()){
				newGraph.add(inner);
				if(graph.contains(i, j))
					newGraph.add(new SimpleEdge(outer,inner));
				inner++;
			}
			outer++;
		}
		graph = newGraph;
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
