package subgraphisomorphism;

import org.openscience.cdk.interfaces.IAtom;

import edu.ucla.sspace.graph.GenericGraph;
import edu.ucla.sspace.graph.Edge;

public class GraphMapping extends GenericGraph<Edge>{

	private static final long serialVersionUID = 1L;
	
	public GraphMapping addMatch(int a, int b){
		return this;
	}
	
	public void add(IAtom atom1, IAtom atom2){
		
	}

}
