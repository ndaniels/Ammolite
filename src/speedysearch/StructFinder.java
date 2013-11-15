package speedysearch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import cmd.InputHandler;
import cmd.OutputHandler;
import cmd.SMSDcmd;
import cmd.ArgumentHandler;
import edu.ucla.sspace.graph.isomorphism.VF2IsomorphismTester;
import fmcs.MCS;

import org.openscience.smsd.AtomAtomMapping;
import org.openscience.smsd.algorithm.vflib.VF2MCS;

public class StructFinder {
	
	private HashMap<Integer, ArrayList<CyclicStruct> > hashed_structs;
	private String struct_filename, id_filename;

	public StructFinder(String _struct_filename, String _id_filename) {
		struct_filename = _struct_filename;
		id_filename = _id_filename;
	}
	
	public static  void testMCS(String query_filename, String target_filename){
		IAtomContainer query = StructFinder.convertToAtomContainer(query_filename);
		query = AtomContainerManipulator.removeHydrogens(query);
		IAtomContainer target = StructFinder.convertToAtomContainer(target_filename);
		target = AtomContainerManipulator.removeHydrogens(target);
		MoleculeStruct q = new MoleculeStruct(query);
		System.out.println(q.graph);
		MoleculeStruct t = new MoleculeStruct(target);
		System.out.println(t.graph);
		MCS myTestMCS = new MCS(query,target);
		myTestMCS.calculate();

		for(IAtomContainer sol: myTestMCS.getSolutions()){
			MoleculeStruct m = new MoleculeStruct(sol);
			System.out.println(m.graph);
		}
	}
	
	public void mcsQuery(String query_filename){
		IAtomContainer query = this.convertToCyclicStruct(query_filename);
		IAtomContainer target;
		IteratingSDFReader struct_file = null;
		try {
			struct_file = new IteratingSDFReader( struct_filename );
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		VF2MCS matcher;
		while(struct_file.hasNext()){
			target = new CyclicStruct( struct_file.next());
			matcher = new VF2MCS(query, target, true, true);
			AtomAtomMapping mapping = matcher.getFirstAtomMapping();
			System.out.println(mapping);
		}
	}
	
	private static IAtomContainer convertToAtomContainer(String filename){
		IAtomContainer mol = null;
		try {
			IteratingSDFReader query_file = new IteratingSDFReader(filename);
			IAtomContainer query = query_file.next();
			query_file.close();
			mol = new AtomContainer(query);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mol;
	}
	
	private CyclicStruct convertToCyclicStruct(String query_filename){
		CyclicStruct struct = null;
		try {
			IteratingSDFReader query_file = new IteratingSDFReader(query_filename);
			IAtomContainer query = query_file.next();
			query_file.close();
			struct = new CyclicStruct(query);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return struct;
	}
	
	private String compressQuery(String query_filename){
		String struct_filename = "STRUCT_"+query_filename;
		
		try{
		CyclicStruct struct = convertToCyclicStruct(query_filename);
		SDFWriter sdfwriter = new SDFWriter(new BufferedWriter( new FileWriter( struct_filename )));
		sdfwriter.write(struct);
		sdfwriter.close();
		} catch( IOException e){
			e.printStackTrace();
		} catch( CDKException c){
			c.printStackTrace();
		}
		return struct_filename;
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
