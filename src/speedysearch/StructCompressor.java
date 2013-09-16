package speedysearch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.isomorphism.matchers.RGroup;

public class StructCompressor {
	private Hashtable<IAtomContainer, ArrayList<IAtomContainer> > found_structs;
	
	public StructCompressor(String mol_db_folder_name) throws IOException, CDKException{
		
		int init_capacity = 1000*1000; // Initial capacity of 1,000,000. Still a lot less than the 60,000,000 molecules
		found_structs = new Hashtable<IAtomContainer, ArrayList<IAtomContainer>>( init_capacity );
		
		File directory = new File( mol_db_folder_name );
		File[] contents = directory.listFiles();
		
		for(File f: contents){
			IteratingSDFReader molecule_database =new IteratingSDFReader(
																			new FileReader( f ), 
																			DefaultChemObjectBuilder.getInstance()
																		);
			checkDatabaseForIsomorphicStructs( molecule_database );
			molecule_database.close();
		}
		
		String out_filename = "compressed_struct_" + mol_db_folder_name;
		produceClusteredDatabase( out_filename );
		
		

	}
	
	private void checkDatabaseForIsomorphicStructs( IteratingSDFReader molecule_database ){
		RGroup r = new RGroup();
        while( molecule_database.hasNext() ){
        	IAtomContainer molecule = molecule_database.next();
        	MoleculeStruct structure = new MoleculeStruct( molecule );
        	if( found_structs.containsKey( structure ) ){
        		ArrayList<IAtomContainer> potential_matches = found_structs.get( structure );
        		boolean no_match = true;
        		for( IAtomContainer candidate: potential_matches ){
        			if ( structure.isIsomorphic( candidate ) ){
        				no_match = false;
        				break;
        			}
        		}
        		if( no_match ){
        			potential_matches.add( structure );
        		}
        	}
        	else{
        		ArrayList<IAtomContainer> list = new ArrayList<IAtomContainer>(2);
        		list.add( structure );
        		found_structs.put(structure, list);
        	}
        }
	}
	
	private void produceClusteredDatabase( String filename ) throws CDKException, IOException{
		String out_filename = "Struct_" + filename;
        SDFWriter writer = new SDFWriter( new BufferedWriter( new FileWriter( out_filename )) );
        Iterator< ArrayList<IAtomContainer> > lists = found_structs.values().iterator();
        
       while( lists.hasNext() ){
    	   ArrayList<IAtomContainer> struct_list = lists.next();
        	for(IAtomContainer struct: struct_list){
        		if( !(struct instanceof MoleculeStruct) ){ // Shouldn't happen but robust coding...
        			struct = new MoleculeStruct( struct );
        		}
        		writer.write( struct );
        	}
        }
       
       if (writer != null) {
           writer.close();
       }
	}
}
