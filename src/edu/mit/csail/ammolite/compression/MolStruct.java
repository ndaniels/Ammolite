package edu.mit.csail.ammolite.compression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
public class MolStruct extends AtomContainer implements Serializable, IAtomContainer {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5426987516210898334L;
	protected int fingerprint;
	protected ArrayList<String> mol_ids;
	protected SparseUndirectedGraph graph;
	protected HashMap<IAtom,Integer> atomsToNodes = new HashMap<IAtom, Integer>();
	protected HashMap<Integer, IAtom> nodesToAtoms = new HashMap<Integer, IAtom>();
	protected HashMap<IBond,Edge> bondsToEdges = new HashMap<IBond, Edge>();
	protected HashMap<Edge, IBond> edgesToBonds = new HashMap<Edge, IBond>();
	
	public MolStruct(){
		
	}
	
	
	public MolStruct( IAtomContainer base )
	{	
		super( new AtomContainer(AtomContainerManipulator.removeHydrogens(base)) );
		mol_ids = new ArrayList<String>();
		
		Iterator<IAtom> atoms = this.atoms().iterator();
		while( atoms.hasNext() ){
			IAtom atom = atoms.next();
			atom.setAtomTypeName("S");
			atom.setSymbol("C");
		}
		Iterator<IBond> bonds = this.bonds().iterator();
		while( bonds.hasNext() ){
			IBond bond = bonds.next();
			bond.setOrder(IBond.Order.SINGLE);
		}
		
		makeGraph(this);

		setFingerprint();
		
		String id = (String) base.getProperty("PUBCHEM_COMPOUND_CID");
		this.setID( id);
		this.mol_ids.add( id );
	}
	
	public Object getProperty(Object description){
		if( description.equals("PUBCHEM_COMPOUND_CID")){
			return this.getID();
		}
		return super.getProperty(description);
	}
	
	@Override
	public void removeAtom(IAtom atom){
		graph.remove( atomsToNodes.get(atom));
		super.removeAtom(atom);
	}
	
	@Override
	public void removeBond(IBond bond){
		graph.remove(bondsToEdges.get(bond));
		super.removeBond(bond);
	}
	
	@Override
	public void addAtom(IAtom atom){
		int node = this.atomCount;
		nodesToAtoms.put(node, atom);
		atomsToNodes.put(atom, node);
		graph.add(node);
		super.addAtom(atom);
	}
	

	protected void setFingerprint(){
		int h = this.atomCount;
		h += 1000 * 1000 * this.bondCount;
		
		int[] degree = new int[this.atoms.length];
		int i=0;
		for(IAtom atom: this.atoms){
			degree[i] = this.getConnectedAtomsCount(atom);
			i++;
		}
		Arrays.sort(degree);
		int bound = degree.length;
		int maxBound = 6;
		if(bound > maxBound){
			bound = maxBound;// Max int32 is a 10 digit number so this is very unlikely to overflow.
		}
		for(int j=0; j<bound; j++){
			h += Math.pow(10, j) * degree[ degree.length - 1 - j];
		}
		
		this.fingerprint = h;

	}
			

	
	private void makeGraph(IAtomContainer base){
		graph = new SparseUndirectedGraph();
		for(int i=0; i<base.getAtomCount(); i++){
			graph.add(i);
			atomsToNodes.put(base.getAtom(i), i);
			nodesToAtoms.put(i, base.getAtom(i));
			for(int j=0; j<i; j++){
				IBond bond = base.getBond(base.getAtom(i), base.getAtom(j));
				if(  bond != null){
					Edge newEdge = new SimpleEdge(i,j);
					bondsToEdges.put(bond, newEdge);
					edgesToBonds.put(newEdge, bond);
					graph.add( newEdge);
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
	
	public SparseUndirectedGraph getGraph(){
		if( this.graph == null){
			makeGraph( this );
		}
		return this.graph;
	}
	
	public boolean isIsomorphic(IAtomContainer that, AbstractIsomorphismTester iso_tester){
		if(!(that instanceof MolStruct)){
			that = new MolStruct( that );
		}
		MolStruct that_struct = (MolStruct) that;
		return iso_tester.areIsomorphic(this.getGraph(), that_struct.getGraph() );
	}
	
	public int fingerprint(){
		return fingerprint;
	}

}