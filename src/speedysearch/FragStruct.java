package speedysearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import edu.ucla.sspace.graph.Edge;

public class FragStruct extends CyclicStruct {
	
	public FragStruct(){
		
	}
	
	public FragStruct(IAtomContainer base) {
		super(base);
		removeOnePrimeCarbons();
		if( getGraph().order() > 0){
			removeAcyclicEdges();
			//removeZeroPrimeCarbons();
		}
		setHash();
	}
	
	protected void removeZeroPrimeCarbons(){
		ArrayList<IAtom> atomsToRemove = new ArrayList<IAtom>();
		for(int v: getGraph().vertices()){
			if(getGraph().degree(v) == 0){
				atomsToRemove.add( nodesToAtoms.get(v));
			}
		}
		for(IAtom atom: atomsToRemove){
			removeAtom(atom);
		}

	}
	


	
	protected void removeAcyclicEdges(){
		Stack<Integer> stack = new Stack<Integer>();
		Map<Edge,Boolean> edgesInCycle = new HashMap<Edge,Boolean>();

		Map<Integer,Integer> visitedVertices = new HashMap<Integer, Integer>();
		Map<Integer,Boolean> exploredVertices = new HashMap<Integer, Boolean>();
		Map<Edge,String> edgeLabels = new HashMap<Edge, String>();
		
		stack.push(getGraph().vertices().toPrimitiveArray()[0]);
		visitedVertices.put(stack.peek(), -1);
		while( ! stack.empty() ){
			boolean newVisit = false;
			int t = stack.peek();

			for(Edge e: getGraph().getAdjacencyList(t)){
				if(!edgeLabels.containsKey(e)){
					int w = e.from();
					if( w == t){
						w = e.to();
					}
					
					if(!visitedVertices.containsKey(w) && !exploredVertices.containsKey(w)){
						newVisit = true;
						edgeLabels.put(e, "tree-edge");
						visitedVertices.put(w, t);
						stack.push(w);
						break;
						
					} else if( visitedVertices.containsKey(w) || exploredVertices.containsKey(w)){
						edgeLabels.put(e, "back-edge");
						
						//Backtrack
						int goalVertex = e.to();
						if(goalVertex == w){
							goalVertex = e.from();
						}
						
						int current = w;
						int predecessor = t;
						boolean leaving = false;
						do{
							
							for( Edge c: getGraph().getEdges(current, predecessor)){
								if( ! edgesInCycle.containsKey(c)){
									edgesInCycle.put(c, true);
								} else if(edgesInCycle.containsKey(c) || !exploredVertices.containsKey(current)){
									leaving = true;
									break;
								}
							
							}
							current = predecessor;
							if(visitedVertices.containsKey(predecessor)){
								predecessor = visitedVertices.get(predecessor);
							} else{
								break;
							}
						}while(current != w && !leaving);
					} else{
						edgeLabels.put(e, "forward-edge"); // Should not occur in an undirected graph
						exploredVertices.put(t, true);
						stack.pop();
					}
				}	
			}
			if(!newVisit){
				exploredVertices.put(t, true);
				stack.pop();
			}
		}
		
		
		ArrayList<IBond> bondsToRemove = new ArrayList<IBond>();
		for(Edge e: graph.edges()){
			if( ! edgesInCycle.containsKey(e) && edgeLabels.containsKey(e)){

				IBond b = this.edgesToBonds.get(e);
				if(!bondsToRemove.contains(b)){
					bondsToRemove.add(b);
				}
			}
		}
		for(IBond bond: bondsToRemove){
			removeBond(bond);
		}

	}
	
}
