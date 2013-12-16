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

import org.apache.commons.io.input.CountingInputStream;
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

	public StructCompressor(MoleculeStructFactory _structFactory) throws IOException, CDKException{
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
		
		for(File f:contents){
			findMolLocations(f);
		}
		
		produceClusteredDatabase( filename );
		talk();
	}

	private void talk(){
		runningTime = (System.currentTimeMillis() - startTime)/(1000);// Time in seconds
		System.out.println(molecules +" "+structures+" "+runningTime+" "+fruitless_comparisons);
	}
	
	public static void mergeDatabases( StructDatabase a, StructDatabase b, String targetname){
		
		if(	!( a.getMoleculeStructFactory().exemplar.getClass().equals( b.getMoleculeStructFactory().exemplar.getClass()))){
			throw new RuntimeException("Databases do not have the same type of compression. Aborting.");
		}
		
		KeyListMap<Integer, MoleculeStruct> newStructsByHash = a.getStructsByHash();
		KeyListMap<Integer, MoleculeStruct> bStructsByHash = b.getStructsByHash();
		VF2IsomorphismTester iso_tester = new VF2IsomorphismTester();
		List<MoleculeStruct> toAdd;
		
		for(int key: bStructsByHash.keySet()){
			
			toAdd = new ArrayList<MoleculeStruct>();
			
			if( newStructsByHash.containsKey(key)){
				
				toAdd = new ArrayList<MoleculeStruct>();
				
				for(MoleculeStruct aStruct: newStructsByHash.get(key)){
					for(MoleculeStruct bStruct: bStructsByHash.get(key)){
						if(aStruct.isIsomorphic(bStruct, iso_tester)){
							for(String id: bStruct.getIDNums()){
								aStruct.addID(id);
							}
							break;
						} else {
							toAdd.add(bStruct);
						}
					}
				}
				
				for(MoleculeStruct m: toAdd){
					newStructsByHash.get(key).add(m);
				}
				
			} else {
				
				newStructsByHash.put(key, bStructsByHash.get(key));			
			}		
		}
		
		HashMap<String, FilePair> newMolLocsByID = a.getFileLocsByID();
		for(String key: b.getFileLocsByID().keySet()){
			newMolLocsByID.put(key, b.getFileLocsByID().get(key));
		}
		
		StructDatabase newDB = new StructDatabase( newStructsByHash, newMolLocsByID, a.getMoleculeStructFactory());
		
		writeObjectToFile(targetname, newDB);
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
        		for( MoleculeStruct candidate: potential_matches ){
        			if ( structure.isIsomorphic(candidate, iso_tester) ){
        				no_match = false;
        				candidate.addID( structure.getID());
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
	 * @throws IOException 
	 */
	private void findMolLocations(File f) throws IOException{
		FileInputStream inStream = new FileInputStream(f);
		CountingInputStream in = new CountingInputStream(inStream);
		
		long pos = 0;
		boolean foundOffset = false;
		int c;

		while( (c=in.read()) != -1){
			
			if(c == '$' && !foundOffset){
				pos = in.getByteCount();
				foundOffset = true;
				
				
			} else if( c == '>') {
				
				StringBuilder sb = new StringBuilder();
				while( (c=in.read()) != '\n'){
					if( c != ' '){
						
						sb.append((char) c);
					}
				}
				
				if( sb.toString().equals("<PUBCHEM_COMPOUND_CID>")){
					sb = new StringBuilder();
					while( (c=in.read()) != '\n'){
						if( c != ' '){
							sb.append((char) c);
						}
					}
					String pubchemID = sb.toString();
					
					moleculeLocationsByID.put(pubchemID, new FilePair(f.getAbsolutePath(), pos));
					foundOffset = false;
					
				}
			}
		}
		
		in.close();
		
		
	}
	
	/**
	 * Produce the files representing the clustered database.
	 * 
	 * @param filename
	 * @throws CDKException
	 * @throws IOException
	 */
	private void produceClusteredDatabase( String name ) throws CDKException, IOException{
		StructDatabase database = new StructDatabase( structsByHash, moleculeLocationsByID, structFactory);
		writeObjectToFile(name, database);
	}
	
	private static void writeObjectToFile(String object_filename, Object o){
		try{
			OutputStream file = new FileOutputStream( object_filename );
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
