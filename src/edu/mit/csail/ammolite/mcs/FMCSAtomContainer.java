package edu.mit.csail.ammolite.mcs;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FMCSAtomContainer extends AtomContainer {
	private Map<IAtom, List<IAtom> > knex = new HashMap<IAtom, List<IAtom> >();
	
	public FMCSAtomContainer(IAtomContainer parent){
		super(parent);
		for(IAtom atom: super.atoms){
			List<IAtom> l = super.getConnectedAtomsList(atom);
			knex.put(atom, l);
		}
	}

	@Override
	public List<IAtom> getConnectedAtomsList(IAtom atom){
		return knex.get(atom);
	}
}
