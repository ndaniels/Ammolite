package speedysearch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.ucla.sspace.graph.isomorphism.VF2IsomorphismTester;
import fmcs.MCS;

public class MoleculeSearcher {
	private MoleculeStructFactory structFactory;
	private StructDatabase db;
	
	public MoleculeSearcher( StructDatabase _db, MoleculeStructFactory _structFactory) {
		structFactory= _structFactory;
		db = _db;
		
	}
	
	
	public String[] exactStructureMatch(IAtomContainer query){
		MoleculeStruct sQuery = structFactory.makeMoleculeStruct(query);
		List<MoleculeStruct> targets = db.getStructsByHash( sQuery.hashCode() );
		VF2IsomorphismTester iso_tester = new VF2IsomorphismTester();
		for(MoleculeStruct t: targets){
			if( sQuery.isIsomorphic(t, iso_tester)){
				return t.getIDNums();
			}
		}
		return null;
	}
	
	public String[] bestStructureMatches(IAtomContainer query){
		MoleculeStruct sQuery = structFactory.makeMoleculeStruct(query);
		int bestMatchAtomCount = 0;
		List<MoleculeStruct> bestMatches = new ArrayList<MoleculeStruct>();
		Iterator<MoleculeStruct> structs = db.iterator();
		MoleculeStruct target;
		while( structs.hasNext() ){
			
			target = structs.next();
			
			MCS myMCS = new MCS(sQuery,target);
			myMCS.calculate();
			if(myMCS.size() > bestMatchAtomCount){
				bestMatches.clear();
				bestMatches.add(target);
				bestMatchAtomCount = myMCS.size();
			} else if( myMCS.size() == bestMatchAtomCount){
				bestMatches.add(target);
			}
		}
		
		String[] ids = new String[ bestMatches.size()];
		
		for(int i=0;i<bestMatches.size(); i++){
			ids[i] = bestMatches.get(i).getID();
		}
		
		return ids;
	}
	
	public IAtomContainer[] bestMoleculeMatches(IAtomContainer query, String[] targetIDs){
		IAtomContainer target;
		int bestMatchAtomCount = 0;
		List<IAtomContainer> bestMatches = new ArrayList<IAtomContainer>();
		
		for(String id: targetIDs){
			target = db.getMolecule(id);
			MCS myMCS = new MCS(query,target);
			myMCS.calculate();
			
			if(myMCS.size() > bestMatchAtomCount){
				bestMatches.clear();
				bestMatches.add(target);
				bestMatchAtomCount = myMCS.size();
				
			} else if( myMCS.size() == bestMatchAtomCount){
				bestMatches.add(target);
				
			}
		}
		
		return bestMatches.toArray(new IAtomContainer[0]);
	}
	
	public IAtomContainer[] hybridSearch(IAtomContainer query){
		String[] structMatches = new String[1];
		structMatches[0] = exactStructureMatch( query );
		if( structMatches[0].equals(no_match)){
			structMatches = bestStructureMatches( query );
		}
		return bestMoleculeMatches(query, structMatches);	
	}	
}
