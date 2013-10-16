package speedysearch;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.IteratingMDLReader;
import edu.ucla.sspace.graph.isomorphism.VF2IsomorphismTester;

/*
 * A class for compressing SDF files based on molecules with identical structure

 * 
 */

public class StructCompressor {
	private static HashMap<Integer, ArrayList<IAtomContainer> > found_structs;
	private static int molecules = 0;
	private static int structures = 0;
	private static int matches = 0;
	private static int fruitless_comparisons = 0;

	
	public static void  compress(String folder_name) throws IOException, CDKException{
		
		int init_capacity = 1000*1000; // Initial capacity of 1,000,000. Still a lot less than the 60,000,000 molecules
		found_structs = new HashMap<Integer, ArrayList<IAtomContainer>>( init_capacity );
		long startTime =System.currentTimeMillis();
		
		long runningTime;
		
		File directory = new File( folder_name );
		File[] contents = {directory};
		if( directory.isDirectory()){
			contents = directory.listFiles();
		}
		String[] a = folder_name.split("/");
		String filename = a[ a.length - 1];
		if( filename.length()>=4 && filename.substring(filename.length()-4, filename.length()).equals(".sdf")){
			filename = filename.substring(0, filename.length() -4 );
		}
		for(File f: contents){
			IteratingSDFReader molecule_database =new IteratingSDFReader(
																			new FileReader( f ), 
																			DefaultChemObjectBuilder.getInstance()
																		);
			System.out.println("Scanning " +  f.getName());
			checkDatabaseForIsomorphicStructs( molecule_database );
			molecule_database.close();
			System.out.println("Scanned " + molecules +" molecules");
			System.out.println("Found " + structures +" unique structures");
			System.out.println("Found " + matches +" molecules with matching structures");
			runningTime = (System.currentTimeMillis() - startTime)/(1000*60);// Time in minutes
			System.out.println("Running for "+ runningTime + " minutes");
		}
		
		produceClusteredDatabase( filename );
		System.out.println("Total Scanned " + molecules +" molecules");
		System.out.println("Total Found " + structures +" unique structures");
		System.out.println("Total Found " + matches +" molecules with matching structures");
		System.out.println("Compared two non isomporhic molecules "+ fruitless_comparisons +" times"); // This is the slowest part of compression so it's worth reducing these as much as possible
		runningTime = (System.currentTimeMillis() - startTime)/(1000*60);// Time in minutes
		System.out.println("Ran for "+ runningTime + " minutes");
		
		

	}
	
	private static void checkDatabaseForIsomorphicStructs( IteratingSDFReader molecule_database ) throws CDKException{
		VF2IsomorphismTester iso_tester = new VF2IsomorphismTester();
        while( molecule_database.hasNext() ){
        	IAtomContainer molecule =  molecule_database.next();
        	MoleculeStruct structure = new CyclicStruct( molecule );
        	molecules++;
        	if( found_structs.containsKey( structure.hashCode())){
        		ArrayList<IAtomContainer> potential_matches = found_structs.get( structure.hashCode() );
        		boolean no_match = true;
        		for( IAtomContainer candidate: potential_matches ){
        			if ( structure.isIsomorphic(candidate, iso_tester) ){
        				no_match = false;
        				( (CyclicStruct) candidate).addID( structure.getID());
        				break;
        			} else {
        				fruitless_comparisons++;
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
	
        StructSDFWriter writer = new StructSDFWriter( filename );
        Iterator< ArrayList<IAtomContainer> > lists = found_structs.values().iterator();
        
       while( lists.hasNext() ){
    	   ArrayList<IAtomContainer> struct_list = lists.next();
        	for(IAtomContainer struct: struct_list){
        		assert( struct != null);
        		writer.write( struct );
        	}
        }
       
       if (writer != null) {
           writer.close();
       }
	}
}
