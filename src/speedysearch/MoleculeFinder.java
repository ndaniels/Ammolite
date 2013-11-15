package speedysearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.ucla.sspace.graph.isomorphism.VF2IsomorphismTester;

public class MoleculeFinder implements IMoleculeFinder {
	
	private HashMap<Integer, ArrayList<CyclicStruct> > hashed_structs;
	private String struct_filename, id_filename;

	public MoleculeFinder() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public ArrayList<IAtomContainer> exactStructuralMatches(IAtomContainer query) {
		String idString = exactStructuralMatchIDs( query );
		String[] ids = idString.split(" ");
		ArrayList<IAtomContainer> result = new ArrayList<IAtomContainer>( ids.length );
		
		for(String id: ids){
			result.add(this.loadMolecule(id));
		}
		return result;
	}

	@Override
	public ArrayList<IAtomContainer> exactStructuralMatches(String filename) {
		return exactStructuralMatches(this.loadMoleculeFile(filename));
	}

	@Override
	public String exactStructuralMatchIDs(IAtomContainer query) {
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

	@Override
	public String exactStructuralMatchIDs(String filename) {
		return exactStructuralMatchIDs( this.loadMoleculeFile(filename));
	}

	@Override
	public ArrayList<IAtomContainer> maximalCommonStructures(IAtomContainer query) {
		String idString = maximalCommonStructureIDs( query );
		String[] ids = idString.split(" ");
		ArrayList<IAtomContainer> result = new ArrayList<IAtomContainer>( ids.length );
		
		for(String id: ids){
			result.add(this.loadMolecule(id));
		}
		return result;
	}

	@Override
	public ArrayList<IAtomContainer> maximalCommonStructures(String filename) {
		return maximalCommonStructures( this.loadMoleculeFile(filename));
	}

	@Override
	public String maximalCommonStructureIDs(IAtomContainer query) {
		if(hashed_structs == null){
			populateHash();
		} 
		return null;
	}

	@Override
	public String maximalCommonStructureIDs(String filename) {
		return maximalCommonStructureIDs( this.loadMoleculeFile(filename) );
	}

	@Override
	public IAtomContainer exactMolecularMatch(IAtomContainer query, ArrayList<IAtomContainer> set) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String exactMolecularMatchID(IAtomContainer query, ArrayList<IAtomContainer> set) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<IAtomContainer> maximalCommonMolecules(IAtomContainer query, ArrayList<IAtomContainer> set) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String maximalCommonMoleculeIDs(IAtomContainer query,ArrayList<IAtomContainer> set) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadDatabase(String filename) {
		// TODO Auto-generated method stub

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
	
	private IAtomContainer loadMolecule(String id){
		return null;
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
	private String getMatchingIDs(String _id_filename, String struct_id){
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
	private IAtomContainer loadMoleculeFile(String filename){
		MoleculeStruct struct = null;
		try {
			IteratingSDFReader query_file = new IteratingSDFReader(filename);
			IAtomContainer query = query_file.next();
			query_file.close();
			struct = new MoleculeStruct(query);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return struct;
	}

}
