package edu.mit.csail.ammolite.database;

import java.util.List;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.PubchemID;

public interface ISDFSet {
	
	public List<IAtomContainer> getAllMolecules();
	public List<String> getFilenames();
	public Set<PubchemID> getMoleculeIDs();
	public IAtomContainer getMol(PubchemID pubID);
	public boolean isOrganized();

}

