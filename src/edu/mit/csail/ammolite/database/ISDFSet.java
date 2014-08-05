package edu.mit.csail.ammolite.database;

import java.util.List;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

public interface ISDFSet {
	
	public List<IAtomContainer> getAllMolecules();
	public List<String> getFilenames();
	public Set<String> getMoleculeIDs();
	public IAtomContainer getMol(String pubID);

}
