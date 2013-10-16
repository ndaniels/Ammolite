package speedysearch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.ucla.sspace.graph.isomorphism.VF2IsomorphismTester;

public class StructFinder {
	
	private HashMap<Integer, ArrayList<CyclicStruct> > hashed_structs;
	private String struct_filename, id_filename;

	public StructFinder(String _struct_filename, String _id_filename) {
		struct_filename = _struct_filename;
		id_filename = _id_filename;
	}
	
	public String exactQuery(String query_filename){
		String result = null;
		IteratingSDFReader query_file;
		try {
			query_file = new IteratingSDFReader( struct_filename );
			IAtomContainer query = query_file.next();
			query_file.close();
			result = exactQuery( query );
		} catch (IOException e) {
			System.out.println("IOException on query sdf file while trying to find query molecule");
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Looks for an exact structural match to a given molecule.
	 * 
	 * Not a Maximal Common Subgraph.
	 * 
	 * @param query
	 * @return
	 */
	public String exactQuery(IAtomContainer query){
		if(hashed_structs == null){
			populateHash();
		}
		CyclicStruct qStruct = new CyclicStruct(query);
		if(!hashed_structs.containsKey(qStruct.hashCode())){
			return "No matches.";
		}
		VF2IsomorphismTester iso_tester = new VF2IsomorphismTester();
		for(CyclicStruct el: hashed_structs.get(qStruct.hashCode()) ){
			if( qStruct.isIsomorphic(el, iso_tester)){
				System.out.println(el.getID());
				return getMatchingIDs(id_filename, el.getID());
			}
		}
		return "No matches.";
	}
	/**
	 * Searches a .st.meta file line by line for a PubChem compound ID. 
	 * 
	 * If it finds the ID it returns the entire line.
	 * 
	 * @param struct_filename
	 * @param struct_id
	 * @return
	 * @throws IOException
	 */
	static public String getMatchingIDs(String _id_filename, String struct_id){
		try{
			BufferedReader structs = new BufferedReader(new FileReader(new File(_id_filename)));
			String line;
			while((line = structs.readLine()) != null){
				if( line.contains(struct_id) ){
					break;
				}
			}
			structs.close();
			return line;
		} catch( IOException e){
			System.out.println("IOException on meta file while trying to match ID");
			e.printStackTrace();
		}
		return null;
	}
	
	private void populateHash(){
		hashed_structs = new HashMap<Integer, ArrayList<CyclicStruct> >(1000*1000);
		IteratingSDFReader struct_file;
		try {
			struct_file = new IteratingSDFReader( struct_filename );
			
			while(struct_file.hasNext()){
				CyclicStruct struct = new CyclicStruct( struct_file.next());
				if(hashed_structs.containsKey( struct.hashCode())){
					hashed_structs.get(struct.hashCode()).add(struct);
				} else {
					hashed_structs.put(struct.hashCode(), new ArrayList<CyclicStruct>());
					hashed_structs.get(struct.hashCode()).add(struct);
				}
			}
			struct_file.close();
		} catch (IOException e) {
			System.out.println("IOException on sdf file while trying to build hash table");
			e.printStackTrace();
		}

	}

}
