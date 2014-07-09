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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
import edu.mit.csail.ammolite.utils.SDFUtils;
import edu.ucla.sspace.graph.isomorphism.VF2IsomorphismTester;

/*
 * A class for compressing SDF files based on molecules with identical structure
 * 
 */

public class StructCompressor {
	private KeyListMap<Integer, MolStruct> structsByFingerprint = new KeyListMap<Integer,MolStruct>(1000);
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
	
	public static KeyListMap<MolStruct, IAtomContainer> compressQueries(Collection<IAtomContainer> queries, MoleculeStructFactory sF){
		KeyListMap<Integer, MolStruct> structsByFinger = new KeyListMap<Integer,MolStruct>(queries.size());
		KeyListMap<MolStruct, IAtomContainer> out = new KeyListMap<MolStruct, IAtomContainer>(queries.size());
		for(IAtomContainer q: queries){
			MolStruct sq = sF.makeMoleculeStruct(q);
			int fingerprint = sq.fingerprint();
			if(structsByFinger.containsKey(fingerprint)){
				VF2IsomorphismTester isoTester = new VF2IsomorphismTester();
				boolean foundMatch = false;
				for(MolStruct candidate:structsByFinger.get(fingerprint)){
					boolean iso = candidate.isIsomorphic(sq, isoTester);
					if( iso){
						foundMatch = true;
						out.add(candidate, q);
						break;
					}
				}
				if(!foundMatch){
					structsByFinger.add(fingerprint, sq);
					out.add(sq, q);
				}
				
			} else {
				structsByFinger.add(fingerprint, sq);
				out.add(sq, q);
			}
		}
		return out;
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

		for(File f: contents){
			Iterator<IAtomContainer> molecule_database = SDFUtils.parseSDFOnline(f.getAbsolutePath());
			checkDatabaseForIsomorphicStructs( molecule_database, structFactory );	
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
		//Logger.debug(" Fruitless Comparisons: "+fruitless_comparisons+" Hash Table Size: "+structsByFingerprint.size());
	}
	

	
	/**
	 * Go through a file looking for matching elements of clusters
	 * 
	 * @param molecule_database
	 * @param structFactory
	 * @throws CDKException
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private void checkDatabaseForIsomorphicStructs( Iterator<IAtomContainer> molecule_database, MoleculeStructFactory structFactory ) throws CDKException, InterruptedException, ExecutionException{
		

        while( molecule_database.hasNext() ){
        	
        	IAtomContainer molecule =  molecule_database.next();       	
        	MolStruct structure = structFactory.makeMoleculeStruct(molecule);
        	molecules++;
        	
        	if( structsByFingerprint.containsKey( structure.fingerprint())){
        		
        		List<MolStruct> potentialMatches = structsByFingerprint.get( structure.fingerprint() );
        		boolean match = parrallelIsomorphism( structure, potentialMatches);
        		if( !match ){
        			structures++;
        			structsByFingerprint.add(structure.fingerprint(), structure);
        		} 
        	}
        	else{
        		structures++;
        		structsByFingerprint.add(structure.fingerprint(), structure);
        	}

        }
	}
	
	private boolean parrallelIsomorphism(MolStruct structure, List<MolStruct> potentialMatches) throws InterruptedException, ExecutionException{

	    List<Callable<Boolean>> callList = new ArrayList<Callable<Boolean>>(potentialMatches.size());
	    final MolStruct fStruct = structure;
	    for (final MolStruct candidate: potentialMatches) {
	    	
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
	    Boolean success = ParallelUtils.parallelTimedSingleExecution( callList, 60*1000);
	    if(success == null){
	    	return false;
	    } else {
	    	return true;
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
		StructDatabaseCoreData database = new StructDatabaseCoreData( structsByFingerprint, moleculeLocationsByID, structFactory.getCompressionType());
		StructDatabaseCompressor.compress(name, database);
	}
	
}
