package edu.mit.csail.ammolite.database;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.PubchemID;
import edu.mit.csail.ammolite.utils.SLRUCache;
import edu.mit.csail.ammolite.utils.StructID;

public class CachingStructDatabase extends StructDatabase {
	protected float memoryLoadFactor = 0.2f;
	SLRUCache<PubchemID,IAtomContainer> cache;
	SLRUCache<StructID,List<IAtomContainer>> structCache;
	private int hits = 0;
	private int misses = 0;
	private int sHits = 0;
	private int sMisses = 0;
	
	public CachingStructDatabase(IDatabaseCoreData core){
		super(core);
		cache = new SLRUCache<PubchemID,IAtomContainer>(memoryLoadFactor);
		if( this.isOrganized()){
			structCache = new SLRUCache<StructID,List<IAtomContainer>>(memoryLoadFactor);
		}
	}
	
	@Override
	public IAtomContainer getMolecule(PubchemID pubchemID){
		if(System.currentTimeMillis() % 1000*60*60 == 0){
			System.out.println("Hits: "+hits+" Misses: "+misses);
		}
		if( cache.containsKey( pubchemID)){
			hits++;
			return cache.get( pubchemID);
		}
		misses++;
		IAtomContainer mol = super.getMolecule(pubchemID);
		cache.put(pubchemID, mol);
		return mol;
	}
	
	@Override
	public List<IAtomContainer> getMatchingMolecules(StructID sID){
		if(System.currentTimeMillis() % 1000*60*60 == 0){
			System.out.println("Structure_Hits: "+sHits+" Structure_Misses: "+sMisses);
		}
		
		if(!isOrganized()){
			super.getMatchingMolecules(sID);
		}
		
		if(structCache.containsKey(sID)){
			sHits++;
			return structCache.get(sID);
		}
		sMisses++;
		List<IAtomContainer> l = super.getMatchingMolecules(sID);
		structCache.put(sID, l);
		return l;
	}
	

}
