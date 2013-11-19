package speedysearch;

import java.util.ArrayList;
import java.util.Iterator;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import edu.ucla.sspace.graph.Edge;

public class CyclicStruct extends MoleculeStruct {

	private static final long serialVersionUID = 1L;

	public CyclicStruct(){
		
	}
	
	public CyclicStruct(IAtomContainer base) {
		super(base);
		removeOnePrimeCarbons();
		setHash();
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
				toRemove.clear();
				bondsToRemove.clear();
				
				for( int i=0; i<getAtomCount(); i++){
					if(graph.degree(i) == 1){
						toRemove.add(getAtom(i));
						bondsToRemove.addAll(getConnectedBondsList(getAtom(i)));
						graph.remove(i);
					}
				}
				
				for(IAtom atom: toRemove){
					removeAtom(atom);
				}
				for(IBond bond: bondsToRemove){
					removeBond(bond);
				}

				
			} while(toRemove.size() != 0);
		}
	}

}
