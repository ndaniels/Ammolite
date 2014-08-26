package edu.mit.csail.ammolite.compression;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.database.CompressionType;
import edu.mit.csail.ammolite.database.IDatabaseCoreData;
import edu.mit.csail.ammolite.database.SDFSet;
import edu.mit.csail.ammolite.database.StructDatabaseCompressor;
import edu.mit.csail.ammolite.database.StructDatabaseCoreData;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.FileUtils;
import edu.mit.csail.ammolite.utils.Logger;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.SDFUtils;
import edu.ucla.sspace.graph.isomorphism.VF2IsomorphismTester;

/*
 * A class for compressing SDF files based on molecules with identical structure
 * 
 */

public class StructCompressor {
	private KeyListMap<Integer, MolStruct> structsByFingerprint = new KeyListMap<Integer,MolStruct>(1000);
	private SDFSet sdfFiles;
	private MoleculeStructFactory structFactory;
	private int molecules = 0;
	private int structures = 0;
	private AtomicInteger fruitless_comparisons = new AtomicInteger(0);
	private int total_comparisons = 0;
	private long runningTime, startTime;

	public StructCompressor(CompressionType compType){
		structFactory = new MoleculeStructFactory( compType);
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
	public void  compress(String inName, String filename) throws IOException, CDKException, InterruptedException, ExecutionException{
		startTime =System.currentTimeMillis();
		//File[] contents = FileUtils.expandWildcard(inName);
		File[] contents = FileUtils.getContents(inName);
		List<String> filenames = new ArrayList<String>();
		CommandLineProgressBar progressBar = new CommandLineProgressBar("Matching Structures", contents.length);
		for(File f: contents){
			filenames.add(f.getAbsolutePath());
			Iterator<IAtomContainer> molecule_database = SDFUtils.parseSDFOnline(f.getAbsolutePath());
			checkDatabaseForIsomorphicStructs( molecule_database, structFactory );	
			talk();
			progressBar.event();
		}
		sdfFiles = new SDFSet(filenames);
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
	            		candidate.addID( MolUtils.getPubID(fStruct));
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
	 * Produce the files representing the clustered database.
	 * 
	 * @param filename
	 * @throws CDKException
	 * @throws IOException
	 */
	private void produceClusteredDatabase( String name ){
		IDatabaseCoreData database = new StructDatabaseCoreData( structsByFingerprint, sdfFiles, structFactory.getCompressionType(), name);
		StructDatabaseCompressor.compress(name, database);
	}
	
}
