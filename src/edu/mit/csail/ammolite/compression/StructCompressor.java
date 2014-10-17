package edu.mit.csail.ammolite.compression;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
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
	private ExecutorService exService;
	CommandLineProgressBar progressBar;

	public StructCompressor(CompressionType compType){
		structFactory = new MoleculeStructFactory( compType);
	}
	
	
	public void  compress(List<String> filenames, String filename) throws IOException, CDKException, InterruptedException, ExecutionException{
	    compress(filenames, filename, -1);
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
	public void  compress(List<String> filenames, String filename, int numThreads) throws IOException, CDKException, InterruptedException, ExecutionException{
		startTime =System.currentTimeMillis();
		int numMols = 0;
		for(String name: filenames){
		    numMols += SDFUtils.estimateNumMolsInSDF(name);
		}
		System.out.println("Compressing approximatley "+String.format("%,d", numMols)+" molecules.");
	    progressBar = new CommandLineProgressBar("Matching Structures", numMols);
		List<String> absoluteFilenames = new ArrayList<String>();
		List<File> files = FileUtils.openFiles(filenames);
		if( numThreads > 0){
		    exService = ParallelUtils.buildNewExecutorService(numThreads);
		} else {
		    int defaultThreads = Runtime.getRuntime().availableProcessors() / 2;
		    exService = ParallelUtils.buildNewExecutorService(defaultThreads);
		}
		
		for(File f: files){
			absoluteFilenames.add(f.getPath());
			Iterator<IAtomContainer> molecule_database = SDFUtils.parseSDFOnline(f.getAbsolutePath());
			checkDatabaseForIsomorphicStructs( molecule_database, structFactory);	
		}
		System.out.print("Collating sdf files... ");
		sdfFiles = new SDFSet(absoluteFilenames);
		System.out.println("Done.");
		System.out.print("Serializing database... ");
		produceClusteredDatabase( filename );
        System.out.println("Done.");
        System.out.print("Shutting down threads... ");
		exService.shutdown();
		System.out.println("Done.");
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
	private void checkDatabaseForIsomorphicStructs( Iterator<IAtomContainer> molecule_database, MoleculeStructFactory structFactory) throws CDKException, InterruptedException, ExecutionException{

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
        	progressBar.event();
        }
	}
	
	private boolean linearIsomorphism(MolStruct structure, List<MolStruct> potentialMatches){
	    VF2IsomorphismTester isoTester = new VF2IsomorphismTester();
	    for(MolStruct candidate: potentialMatches){
	        boolean iso = candidate.isIsomorphic(structure, isoTester);
	        if(iso){
	            candidate.addID( MolUtils.getPubID(structure));
                return iso;
	        }
	    }
	    return false;
	}
	
	/**
	 * Checks whether a given structure is isomorphic to any structure in a list.
	 * 
	 * If an isomorphic structure is found the given structure's corresponding PubChem ID 
	 * is added to the list of ids to the isomorphic structure
	 * 
	 * @param structure
	 * @param potentialMatches
	 * @return true if a match was found
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
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
	    Boolean success = ParallelUtils.parallelTimedSingleExecution( callList, 60*1000, exService);
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
		StructDatabaseCompressor.compressGeneric(name, database);
	}
	
	
	
}
