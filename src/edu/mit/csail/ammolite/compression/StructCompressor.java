package edu.mit.csail.ammolite.compression;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.input.CountingInputStream;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.StructSDFWriter;
import edu.mit.csail.ammolite.database.CompressionType;
import edu.mit.csail.ammolite.database.FilePair;
import edu.mit.csail.ammolite.database.StructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseCompressor;
import edu.mit.csail.ammolite.database.StructDatabaseCoreData;
import edu.mit.csail.ammolite.utils.Logger;
import edu.mit.csail.ammolite.utils.ParallelUtils;
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
	private AtomicInteger fruitless_comparisons = new AtomicInteger(0);
	private int total_comparisons = 0;
	private long runningTime, startTime;

	public StructCompressor(CompressionType compType){
		structFactory = new MoleculeStructFactory( compType);
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
		return contents;
	}
	
	/**
	 * Scans through an sdf library and compresses it.
	 * 
	 * @param folder_name location of the database to be compressed
	 * @param filename name for the compressed database
	 * @throws IOException
	 * @throws CDKException
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public void  compress(String folder_name, String filename) throws IOException, CDKException, InterruptedException, ExecutionException{
		startTime =System.currentTimeMillis();
		File[] contents = getContents(folder_name);

		FileInputStream fs;
		BufferedReader br;
		for(File f: contents){
			long fStart = System.currentTimeMillis();
			
			fs = new FileInputStream(f);
			br = new BufferedReader( new InputStreamReader(fs ));
			IteratingSDFReader molecule_database =new IteratingSDFReader(
																			br,
																			DefaultChemObjectBuilder.getInstance()
																		);
			long setupFinish = System.currentTimeMillis() - fStart;
			Logger.debug("Scanning " +  f.getName());
			
			checkDatabaseForIsomorphicStructs( molecule_database, structFactory );
			long scanFinish = System.currentTimeMillis() - fStart; 
			Logger.debug("Finished Scanning after " +  scanFinish+ " milliseconds");
			
			molecule_database.close();
			br.close();
			fs.close();
			long closeFinish = System.currentTimeMillis() - fStart; 
			Logger.debug("Finished closing files after " +  closeFinish+ " milliseconds");
			
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
		Logger.log("Molecules: "+ molecules +" Representatives: "+structures+" Seconds: "+runningTime,2);
		//Logger.debug(" Fruitless Comparisons: "+fruitless_comparisons+" Hash Table Size: "+structsByHash.size());
	}
	
	private void showTableShape(){
		runningTime = (System.currentTimeMillis() - startTime)/(1000);
		try {
		    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("hashtableshape.txt", true)));
			out.print("Molecules: "+ molecules +" Representatives: "+structures+" Seconds: "+runningTime);
			out.println(" Fruitless Comparisons: "+fruitless_comparisons+" Hash Table Size: "+structsByHash.size());
			
			for(int key : structsByHash.keySet()){
				out.print(structsByHash.get(key).size());
				out.print(" ");
			}
			out.println();
		    out.close();
		} catch (IOException e) {
		    //exception handling left as an exercise for the reader
		}

	}
	
//	public static void mergeDatabases( StructDatabase a, StructDatabase b, String targetname){
//		
//		if(	 !(a.getCompressionType().equals(b.getCompressionType()))){
//			throw new RuntimeException("Databases do not have the same type of compression. Aborting.");
//		}
//		
//		KeyListMap<Integer, MoleculeStruct> newStructsByHash = a.getStructsByHash();
//		KeyListMap<Integer, MoleculeStruct> bStructsByHash = b.getStructsByHash();
//		VF2IsomorphismTester iso_tester = new VF2IsomorphismTester();
//		List<MoleculeStruct> toAdd;
//		
//		for(int key: bStructsByHash.keySet()){
//			
//			toAdd = new ArrayList<MoleculeStruct>();
//			
//			if( newStructsByHash.containsKey(key)){
//				
//				toAdd = new ArrayList<MoleculeStruct>();
//				
//				for(MoleculeStruct aStruct: newStructsByHash.get(key)){
//					for(MoleculeStruct bStruct: bStructsByHash.get(key)){
//						if(aStruct.isIsomorphic(bStruct, iso_tester)){
//							for(String id: bStruct.getIDNums()){
//								aStruct.addID(id);
//							}
//							break;
//						} else {
//							toAdd.add(bStruct);
//						}
//					}
//				}
//				
//				for(MoleculeStruct m: toAdd){
//					newStructsByHash.get(key).add(m);
//				}
//				
//			} else {
//				
//				newStructsByHash.put(key, bStructsByHash.get(key));			
//			}		
//		}
//		
//		HashMap<String, FilePair> newMolLocsByID = a.getFileLocsByID();
//		for(String key: b.getFileLocsByID().keySet()){
//			newMolLocsByID.put(key, b.getFileLocsByID().get(key));
//		}
//		
//		StructDatabaseCoreData newDB = new StructDatabaseCoreData( newStructsByHash, newMolLocsByID, a.getCompressionType());
//		
//		writeObjectToFile(targetname, newDB);
//	}
	
	/**
	 * Go through a file looking for matching elements of clusters
	 * 
	 * @param molecule_database
	 * @param structFactory
	 * @throws CDKException
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private void checkDatabaseForIsomorphicStructs( IteratingSDFReader molecule_database, MoleculeStructFactory structFactory ) throws CDKException, InterruptedException, ExecutionException{
		

        while( molecule_database.hasNext() ){
        	if( molecules % 1000 == 0 ){
        		talk();
        	}
        	
        	IAtomContainer molecule =  molecule_database.next();       	
        	MoleculeStruct structure = structFactory.makeMoleculeStruct(molecule);
        	molecules++;
        	
        	if( structsByHash.containsKey( structure.hashCode())){
        		
        		List<MoleculeStruct> potential_matches = structsByHash.get( structure.hashCode() );
        		boolean match = parrallelIsomorphism( structure, potential_matches);
        		if( !match ){
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
	
	private boolean parrallelIsomorphism(MoleculeStruct structure, List<MoleculeStruct> potentialMatches) throws InterruptedException, ExecutionException{

	    List<Callable<Boolean>> callList = new ArrayList<Callable<Boolean>>(potentialMatches.size());
	    
	    final MoleculeStruct fStruct = structure;
	    
	    for (final MoleculeStruct candidate: potentialMatches) {
	    	
	        Callable<Boolean> callable = new Callable<Boolean>() {
	        	
	            public Boolean call() throws Exception {
	            	VF2IsomorphismTester iso_tester = new VF2IsomorphismTester();
	            	boolean iso = candidate.isIsomorphic(fStruct, iso_tester);
	            	if( iso ){
	            		candidate.addID( fStruct.getID());
	            		return iso;
	            	} else {
	            		fruitless_comparisons.incrementAndGet();
	            	}
	                return null;
	            }
	        };
	        callList.add(callable);

	    }

	    return ParallelUtils.parallelTimedSingleExecution( callList, 60*1000);
	    
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
				pos = in.getByteCount()-1;
				foundOffset = true;
				
				
			} else if( c == '>') {
				
				StringBuilder sb = new StringBuilder();
				while( (c=in.read()) != '\n'){
					if( c != ' '){
						
						sb.append((char) c);
					}
				}
				
				if( sb.toString().equals("<PUBCHEM_COMPOUND_CID>")){
					StringBuilder idGrabber = new StringBuilder();
					while( (c=in.read()) != '\n'){
						if( c != ' '){
							idGrabber.append((char) c);
						}
					}
					String pubchemID = idGrabber.toString();
					
					FilePair myFP =  new FilePair(f.getAbsolutePath(), pos);
					
					moleculeLocationsByID.put(pubchemID, myFP);
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
	private void produceClusteredDatabase( String name ){
		StructDatabaseCoreData database = new StructDatabaseCoreData( structsByHash, moleculeLocationsByID, structFactory.getCompressionType());
		StructDatabaseCompressor.compress(name, database);
	}
	
}
