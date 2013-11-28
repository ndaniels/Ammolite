package speedysearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.ucla.sspace.graph.isomorphism.VF2IsomorphismTester;
import fmcs.MCS;

public class MoleculeSearcher {
	private KeyListMap<Integer, MoleculeStruct> structsByHash;
	private KeyListMap<Integer, MoleculeStruct> structsByAtomCount;
	private ArrayList<MoleculeStruct> structsInAtomCountOrder;
	private String database_filename, struct_filename, id_filename;
	private MoleculeStructFactory structFactory;
	
	public MoleculeSearcher(String _database_filename,String  _struct_filename, String _id_filename, MoleculeStruct exemplar) {
		structFactory= new MoleculeStructFactory( exemplar );
		database_filename= _database_filename;
		struct_filename = _struct_filename;
		id_filename = _id_filename;
		this.populateData();
		
	}
	
	private void populateData(){
		structsByHash = new KeyListMap<Integer, MoleculeStruct>(1000);
		structsByAtomCount = new KeyListMap<Integer, MoleculeStruct>(1000);
		structsInAtomCountOrder = new ArrayList<MoleculeStruct>();
		IteratingSDFReader struct_file;
		try {
			struct_file = new IteratingSDFReader( struct_filename );
			
			while(struct_file.hasNext()){
				MoleculeStruct struct = structFactory.makeMoleculeStruct( struct_file.next());
				structsByHash.add(struct.hashCode(), struct);
				structsByAtomCount.add(struct.getAtomCount(), struct);
				structsInAtomCountOrder.add(struct);
			}
			struct_file.close();
		} catch (IOException e) {
			System.out.println("IOException on sdf file while trying to build hash table");
			e.printStackTrace();
		}
		Collections.sort( structsInAtomCountOrder );
	}
	
	public String exactStructureMatch(IAtomContainer query){
		MoleculeStruct sQuery = structFactory.makeMoleculeStruct(query);
		LinkedList<MoleculeStruct> targets = structsByHash.get(sQuery.hashCode());
		VF2IsomorphismTester iso_tester = new VF2IsomorphismTester();
		for(MoleculeStruct t: targets){
			if( sQuery.isIsomorphic(t, iso_tester)){
				return sQuery.getID();
			}
		}
		return "NO_MATCH";
	}
	
	public String bestStructureMatch(IAtomContainer query){
		MoleculeStruct sQuery = structFactory.makeMoleculeStruct(query);
		int bestMatchAtomCount = 0;
		MoleculeStruct bestMatch = null;
		
		for(MoleculeStruct t: structsInAtomCountOrder){
			MCS myMCS = new MCS(sQuery,t);
			myMCS.calculate();
			if(myMCS.size() > bestMatchAtomCount){
				bestMatch = t;
				bestMatchAtomCount = myMCS.size();
			}
		}
		
		return bestMatch.getID();
	}
	
	
	
}
