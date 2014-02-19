package edu.mit.csail.ammolite.search;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

public interface IMolSearcher {

	public MolTriple[] search(IAtomContainer query, double threshold, double probability);
}
