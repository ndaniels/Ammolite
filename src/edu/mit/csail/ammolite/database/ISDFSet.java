package edu.mit.csail.ammolite.database;

import java.util.List;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.AmmoliteID;
import edu.mit.csail.ammolite.utils.PubchemID;

public interface ISDFSet {
	
	public List<IAtomContainer> getAllMolecules();
	public List<String> getFilenames();
	public List<String> getFilepaths();
	public Set<AmmoliteID> getMoleculeIDs();
	public IAtomContainer getMol(AmmoliteID ammID);
	public boolean isOrganized();
	public String listSourceFiles();

}

