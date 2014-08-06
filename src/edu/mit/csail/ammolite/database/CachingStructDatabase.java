package edu.mit.csail.ammolite.database;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.SLRUCache;

public class CachingStructDatabase extends StructDatabase {
	protected float memoryLoadFactor = 0.2f;
	SLRUCache<String,IAtomContainer> cache;
	
	public CachingStructDatabase(IDatabaseCoreData core){
		super(core);
		cache = new SLRUCache<String,IAtomContainer>(memoryLoadFactor);
	}
	
	@Override
	public IAtomContainer getMolecule(String pubchemID){
		if( cache.containsKey( pubchemID)){
			return cache.get(pubchemID);
		}
		
		IAtomContainer mol = super.getMolecule(pubchemID);
		cache.put(pubchemID, mol);
		return mol;
	}

}
