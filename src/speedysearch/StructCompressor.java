package speedysearch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.iterator.IteratingMDLReader;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;

/*
 * A class for compressing SDF files based on molecules with identical structure
 * 
 * TODO: only opens first molecule in a file
 * 
 * TODO: does not find isomorphisms in identical structures, even identical molecules
 * 
 */

public class StructCompressor {
	private static HashMap<Integer, ArrayList<IAtomContainer> > found_structs;
	private static int molecules = 0;
	private static int structures = 0;
	private static int matches = 0;
	
	public static void  compress(String mol_db_folder_name) throws IOException, CDKException{
		
		int init_capacity = 1000*1000; // Initial capacity of 1,000,000. Still a lot less than the 60,000,000 molecules
		found_structs = new HashMap<Integer, ArrayList<IAtomContainer>>( init_capacity );
		
		File directory = new File( mol_db_folder_name );
		File[] contents = {directory};
		if( directory.isDirectory()){
			contents = directory.listFiles();
		}
		
		for(File f: contents){
			IteratingMDLReader molecule_database =new IteratingMDLReader(
																			new FileReader( f ), 
																			DefaultChemObjectBuilder.getInstance()
																		);
			System.out.println("Scanning " +  f.getName());
			checkDatabaseForIsomorphicStructs( molecule_database );
			molecule_database.close();
		}
		
		String out_filename = "compressed_structs.sdf";
		produceClusteredDatabase( out_filename );
		System.out.println("Scanned " + molecules +" molecules");
		System.out.println("Found " + structures +" unique structures");
		System.out.println("Found " + matches +" molecules with matching structures");
		
		

	}
	
	private static void checkDatabaseForIsomorphicStructs( IteratingMDLReader molecule_database ) throws CDKException{
        while( molecule_database.hasNext() ){
        	IAtomContainer molecule =  molecule_database.next();
        	MoleculeStruct structure = new MoleculeStruct( molecule );
        	molecules++;
        	if( found_structs.containsKey( structure.hashCode())){
        		ArrayList<IAtomContainer> potential_matches = found_structs.get( structure.hashCode() );
        		boolean no_match = true;
        		for( IAtomContainer candidate: potential_matches ){
        			if ( UniversalIsomorphismTester.isIsomorph(structure, candidate) ){
        				no_match = false;
        				break;
        			}
        		}
        		if( no_match ){
        			structures++;
        			potential_matches.add( structure );
        		} else{
        			matches++;
        		}
        	}
        	else{
        		structures++;
        		ArrayList<IAtomContainer> list = new ArrayList<IAtomContainer>(2);
        		list.add( structure );
        		found_structs.put(structure.hashCode(), list);
        	}
        }
	}
	
	private static void produceClusteredDatabase( String filename ) throws CDKException, IOException{
	
        SDFWriter writer = new SDFWriter( new BufferedWriter( new FileWriter( filename )) );
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
