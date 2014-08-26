package edu.mit.csail.ammolite.utils;

import java.util.List;

import org.openscience.cdk.ChemObject;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObject;

import edu.mit.csail.ammolite.aggregation.Cluster;

public class MolTriple {

	public MolTriple(List<IAtomContainer> solutions, IAtomContainer query,
			IAtomContainer target) {
		// TODO Auto-generated constructor stub
	}

	public IAtomContainer getQuery() {
		throw new UnsupportedOperationException();
	}

	public IAtomContainer getMatch() {
		throw new UnsupportedOperationException();
	}

	public List<IAtomContainer> getOverlap() {
		throw new UnsupportedOperationException();
	}

}
