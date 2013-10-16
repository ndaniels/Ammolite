package speedysearch;

import java.util.ArrayList;
import java.util.Iterator;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.SparseUndirectedGraph;
import edu.ucla.sspace.graph.SimpleEdge;
import edu.ucla.sspace.graph.Graph;

import edu.ucla.sspace.graph.isomorphism.AbstractIsomorphismTester;


/**
 * A class representing the structure of a molecule.
 * All bonds and atoms are identical and generic.
 * 
 * @author  David Danko
 */
public class MoleculeStruct extends AtomContainer
{
	protected int hash_code;
	
	private static final long serialVersionUID = 1L;
	protected ArrayList<String> mol_ids;
	protected SparseUndirectedGraph graph;

	public MoleculeStruct( IAtomContainer base )
	{	
		super( new AtomContainer(AtomContainerManipulator.removeHydrogens(base)) );
		mol_ids = new ArrayList<String>();
		
		Iterator<IAtom> atoms = this.atoms().iterator();
		while( atoms.hasNext() ){
			IAtom atom = atoms.next();
			atom.setAtomTypeName("S");
			atom.setSymbol("C");
		}

		makeGraph(this);

		setHash();
		this.setID((String) base.getProperty("PUBCHEM_COMPOUND_CID"));
		this.setProperty("PUBCHEM_COMPOUND_CID", this.getID());

		this.mol_ids.add( getID() );

	}

	
	protected void setHash(){
		int max_atom_count = 0;
		int min_atom_count = 1000;
		Iterator<IBond> bonds = this.bonds().iterator();
		while( bonds.hasNext() ){
			IBond bond = bonds.next();
			if(bond.getAtomCount() > max_atom_count){
				max_atom_count = bond.getAtomCount();
			
			}
			if(bond.getAtomCount() < min_atom_count){
				min_atom_count = bond.getAtomCount();
			}
			bond.setOrder(IBond.Order.SINGLE);
		}
		
		hash_code = 1000000 * max_atom_count + 10000 * min_atom_count + 100 * this.bondCount + this.atomCount;
	}
	
	private void makeGraph(IAtomContainer base){
		graph = new SparseUndirectedGraph();
		for(int i=0; i<base.getAtomCount(); i++){
			graph.add(i);
			for(int j=0; j<i; j++){
				if( base.getBond(base.getAtom(i), base.getAtom(j)) != null){
					graph.add( new SimpleEdge(i,j) );
				}
			}
		}
	}
	
	public void addID(String id){
		mol_ids.add(id);
	}
	 
	public String[] getIDNums(){
		String[] a = {"plop"};
		return mol_ids.toArray(a);
		
	}
	
	public Graph<Edge> getGraph(){
		if( this.graph == null){
			makeGraph( this );
		}
		return this.graph;
	}
	
	public boolean isIsomorphic(IAtomContainer that, AbstractIsomorphismTester iso_tester){
		if(!(that instanceof MoleculeStruct)){
			that = new MoleculeStruct( that );
		}
		MoleculeStruct that_struct = (MoleculeStruct) that;
		return iso_tester.areIsomorphic(this.getGraph(), that_struct.getGraph() );
	}
	

	
	@Override
	public int hashCode(){
		return hash_code;
	}
}