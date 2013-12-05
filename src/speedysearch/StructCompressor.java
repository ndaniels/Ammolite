package speedysearch;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.input.BoundedInputStream;
import org.omg.CORBA.portable.InputStream;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.ucla.sspace.graph.isomorphism.VF2IsomorphismTester;

/*
 * A class for compressing SDF files based on molecules with identical structure
 * 
 */

public class StructCompressor {
	private KeyListMap<Integer, MoleculeStruct> structsByHash = new KeyListMap<Integer,MoleculeStruct>(1000);
	private HashMap<String, FilePair> moleculeLocationsByID = new HashMap<String, FilePair>();
	private MoleculeStructFactory structFactory;
	private int molecules = 0;
	private int structures = 0;
	private int fruitless_comparisons = 0;
	private long runningTime, startTime;

	public void StructCompresser(String folder_name, MoleculeStructFactory _structFactory) throws IOException, CDKException{
		structFactory = _structFactory;
	}
	
	/**
	 * Turns a folder or file name into a list of file names (or just one name)
	 * @param folder_name
	 * @return
	 */
	private File[] getContents(String folder_name ){
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
		return contents;
	}
	
	/**
	 * Scans through an sdf library and compresses it.
	 * 
	 * @param folder_name location of the database to be compressed
	 * @param filename name for the compressed database
	 * @throws IOException
	 * @throws CDKException
	 */
	public void  compress(String folder_name, String filename) throws IOException, CDKException{
		startTime =System.currentTimeMillis();
		File[] contents = getContents(folder_name);

		FileInputStream fs;
		BufferedReader br;
		for(File f: contents){
			fs = new FileInputStream(f);
			br = new BufferedReader( new InputStreamReader(fs ));
			IteratingSDFReader molecule_database =new IteratingSDFReader(
																			br,
																			DefaultChemObjectBuilder.getInstance()
																		);
			System.out.println("Scanning " +  f.getName());
			
			checkDatabaseForIsomorphicStructs( molecule_database, structFactory );
			molecule_database.close();
			
			talk();
		}
		
		produceClusteredDatabase( filename );
		talk();
	}

	private void talk(){
		runningTime = (System.currentTimeMillis() - startTime)/(1000);// Time in seconds
		System.out.println(molecules +" "+structures+" "+runningTime+" "+fruitless_comparisons);
	}
	
	/**
	 * Go through a file looking for matching elements of clusters
	 * 
	 * @param molecule_database
	 * @param structFactory
	 * @throws CDKException
	 */
	private void checkDatabaseForIsomorphicStructs( IteratingSDFReader molecule_database, MoleculeStructFactory structFactory ) throws CDKException{
		
		VF2IsomorphismTester iso_tester = new VF2IsomorphismTester();
        while( molecule_database.hasNext() ){

        	IAtomContainer molecule =  molecule_database.next();       	
        	MoleculeStruct structure = structFactory.makeMoleculeStruct(molecule);
        	molecules++;
        	if( structsByHash.containsKey( structure.hashCode())){
        		List<MoleculeStruct> potential_matches = structsByHash.get( structure.hashCode() );
        		boolean no_match = true;
        		for( IAtomContainer candidate: potential_matches ){
        			if ( structure.isIsomorphic(candidate, iso_tester) ){
        				no_match = false;
        				( (RingStruct) candidate).addID( structure.getID());
        				break;
        			} else {
        				fruitless_comparisons++;
        			}
        		}
        		
        		if( no_match ){
        			structures++;
        			structsByHash.add(structure.hashCode(), structure);
        		} 
        	}
        	else{
        		structures++;
        		structsByHash.add(structure.hashCode(), structure);
        	}
        }
	}
	
	/**
	 * Reads through a file and adds to the location hash. This is a redundant loop but java struggles sometimes.
	 * 
	 * @param f
	 * @throws FileNotFoundException
	 */
	private void findMolLocations(File f) throws FileNotFoundException{

		
	}
	
	/**
	 * Produce the files representing the clustered database.
	 * 
	 * @param filename
	 * @throws CDKException
	 * @throws IOException
	 */
	private void produceClusteredDatabase( String filename ) throws CDKException, IOException{
	
        StructSDFWriter writer = new StructSDFWriter( filename );
        Iterator<List<MoleculeStruct>> lists = structsByHash.values().iterator();
        
       while( lists.hasNext() ){
    	   List<MoleculeStruct> struct_list = lists.next();
    	   
        	for(IAtomContainer struct: struct_list){
        		assert( struct != null);
        		writer.write( struct );
        	}
        }
       
       if (writer != null) {
           writer.close();
       }
	}
	
	private void WriteObjectToFile( String object_filename, Object o){
		object_filename = object_filename + ".ser";
		try{
			OutputStream file = new FileOutputStream(object_filename);
			OutputStream buffer = new BufferedOutputStream( file );
			ObjectOutput output = new ObjectOutputStream( buffer );
			output.writeObject(o);
			output.close();
		}
		catch( IOException ex){
			ex.printStackTrace();
		}
		
	}
}
