package edu.mit.csail.ammolite.database;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.SLRUCache;

public class CachingStructDatabase extends StructDatabase {
	protected float memoryLoadFactor = 0.2f;
	SLRUCache<String,IAtomContainer> cache;
	private int hits = 0;;
	private int misses = 0;
	
	public CachingStructDatabase(IDatabaseCoreData core){
		super(core);
		cache = new SLRUCache<String,IAtomContainer>(memoryLoadFactor);
	}
	
	@Override
	public IAtomContainer getMolecule(String pubchemID){
		if(System.currentTimeMillis() % 1000*60*60 == 0){
			System.out.println("Hits: "+hits+" Misses: "+misses);
		}
		if( cache.containsKey( pubchemID)){
			hits++;
			return cache.get(pubchemID);
		}
		misses++;
		IAtomContainer mol = super.getMolecule(pubchemID);
		cache.put(pubchemID, mol);
		return mol;
	}
	

}
