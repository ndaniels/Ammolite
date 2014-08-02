package edu.mit.csail.ammolite.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.utils.Logger;

public class BigStructDatabase extends StructDatabase{
	private Map<String, IAtomContainer> idToMolecule = new HashMap<String, IAtomContainer>();
	/**
	 * For testing, like structdatabase but searches an entire file for the appropriate molecule.
	 * @param coredata
	 */
	public BigStructDatabase( StructDatabaseCoreData coredata){
		super(coredata);
	}
	
	public void preloadMolecules(){
		System.out.println("Preloading molecules into memory...");
		Set<String> ids = sdfFiles.getMoleculeIDs();
		System.out.println("Fetched list of ids.");
		for(String pubID: ids){
			IAtomContainer mol = sdfFiles.getMol(pubID);
			idToMolecule.put(pubID, mol);
		}
		System.out.println("Loaded molecules.");
	}
	
	@Override
	public IAtomContainer getMolecule(String pubchemID){
		if(idToMolecule.containsKey(pubchemID)){
			return idToMolecule.get(pubchemID);
		}
		return super.getMolecule(pubchemID);
	}
	
	public Collection<IAtomContainer> getMolecules(){
		return idToMolecule.values();
	}

}
