package subgraphisomorphism;

import speedysearch.MoleculeStruct;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.util.primitive.IntSet;

import java.util.Collection;

import org.openscience.cdk.interfaces.IAtom;

public class MoleculeSubgraphIsomorphismFinder {
	int currSubstructureNum = 0;
	int substructureNumLimit = 0;
	
	public MoleculeSubgraphIsomorphismFinder() {
		// TODO Auto-generated constructor stub
	}
	
	public MoleculeStruct maximalCommonSubgraph(MoleculeStruct a, MoleculeStruct b){
		
	}
	
	private GraphMapping match(Graph a, Graph b, GraphMapping m, Collection<Integer> t){
		if( upperBound(a, b, m) < m.order() ){
			return m;
		} else {
		
			while( true){
				int v1 = a.order() - m.order();
				t.add( v1 );
				if( v1 == 0 ){
					updateCandidate(m);
					return m;
				} else{
					IntSet v2 = b.vertices();
					v2.removeAll(m.vertices());
					for( int i: v2){
						if( compatible(v1,i)){
							m.addMatch(v1,i);
						}
						match(a, b, m, t);
					}
				}
			}
		}
	}

	private int upperBound( Graph a, Graph b, GraphMapping currentMapping){
		// TODO
		return null;
	}
	
	private void updateCandidate( GraphMapping m){
		// TODO
	}
	
	private void grow(MoleculeStruct a, MoleculeStruct b, GraphMapping currentMapping) {
        int[] atomListOneCopy = a.getGraph().vertices().toPrimitiveArray();
        int[] atomListTwoCopy = b.getGraph().vertices().toPrimitiveArray();
        int currentBound = currentMapping.size();
            
        if(currentBound < size()) {
            return;
        }
        
        while(true) {
            
            if (a.getGraph().order() == 0 || b.getGraph().order() == 0) {
                boundary();
                return;
            }
            
            IAtom atom1 = a.getFirstAtom();
            		
            for (IAtom atom2: b.atoms()) {
                
                
                boolean introducedNewComponent = compatible(atom1, atom2);
                
                if ( introducedNewComponent ) {
                	++currSubstructureNum;
                }
                	
                if (!(currSubstructureNum > substructureNumLimit)) {
                    
                    currentMapping.add(atom1, atom2);
                    
                    a.removeAtom(atom1);
                    b.removeAtom(atom2);
                    
                    grow(atomListOneCopy, atomListTwo);
                    
                    atomListTwo.push_back(atomListTwoPtr[i]);
                    currentMapping.pop_back();
                }
                
                if (introducedNewComponent) {
                    --currSubstructureNum;
                }
                        
                    
                }
            }

	
	private boolean compatible(int a, int b){
		// TODO
		return null;
	}
}
