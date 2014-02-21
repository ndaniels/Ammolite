package edu.mit.csail.ammolite.search;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;

import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.Logger;
import edu.mit.csail.ammolite.database.StructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;

public class ParallelSearcher implements IBatchSearcher{
	private static int numThreads = Runtime.getRuntime().availableProcessors();;
	private static StructDatabase db;
	private static boolean useTanimoto;
	private static final int BATCH_SIZE = 10*numThreads;
	

	/**
	 * API level interface that handles file IO. 
	 * 
	 * Could eventually be moved to SearchHandler class.
	 * 
	 * @param databaseFilename
	 * @param queryFilename
	 * @param outFilename
	 * @param threshold
	 * @param probability
	 * @param _useTanimoto
	 */
	public void search(String databaseFilename, String queryFilename, String outFilename, double threshold, double probability, boolean _useTanimoto){
		db = StructDatabaseDecompressor.decompress(databaseFilename);
		useTanimoto = _useTanimoto;
		try {
			doSearch( queryFilename, outFilename, threshold, probability);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} 
		
	}
	
	public List<MolTriple[]> search(StructDatabase _db, List<IAtomContainer> queries, double threshold, double probability, boolean _useTanimoto){
		db = _db;
		useTanimoto = _useTanimoto;
		try {
			return parallelSearch(queries, threshold, probability);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		return null; 
	}
	
	/**
	 * Does all the file handling  for search.
	 * 
	 * @param queryFilename
	 * @param outFilename
	 * @param threshold
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 * @throws CDKException
	 */
	private void doSearch( String queryFilename, String outFilename, double threshold , double probability) throws InterruptedException, ExecutionException, IOException, CDKException{

		List<IAtomContainer> queries = new ArrayList<IAtomContainer>( BATCH_SIZE);
		IteratingSDFReader queryFile = new IteratingSDFReader( new FileInputStream( new File( queryFilename)), DefaultChemObjectBuilder.getInstance());
		SDFWriter writer = new SDFWriter(new BufferedWriter( new FileWriter( outFilename + ".sdf" )));
		while( queryFile.hasNext() ){
			int count=0;
			while( queryFile.hasNext() && count < BATCH_SIZE){
				queries.add( queryFile.next());
				count++;
			}
			for( MolTriple[] tripArray : parallelSearch( queries, threshold, probability)){
				Logger.experiment("NumResults: "+tripArray.length);
				if( outFilename.equals("DEV-TEST")){
					StringBuilder sb = new StringBuilder();
					for( MolTriple triple : tripArray){
						int overlap = triple.getOverlap().get(0).getAtomCount();
						int a = triple.getQuery().getAtomCount();
						int b = triple.getMatch().getAtomCount();
						sb.append( Util.overlapCoeff(overlap, a, b));
						sb.append(" ");
						sb.append( Util.tanimotoCoeff(overlap, a, b));
						sb.append(" ");
					}
					Logger.experiment(sb.toString());
				} else {
					for( MolTriple triple : tripArray){
					    writer.write(triple.getQuery());
					    writer.write(triple.getMatch());
					    writer.write(triple.getOverlap().get(0));
					}
				}
			}
		}
		queryFile.close();
		writer.close();
	}
	
	/**
	 * Uses a parallel for loop to do search several individual searches simultaneously.
	 * 
	 * @param queries
	 * @param threshold
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private List<MolTriple[]> parallelSearch(List<IAtomContainer> queries, double threshold, double probability) throws InterruptedException, ExecutionException{
		edu.mit.csail.ammolite.Logger.debug("Searching for "+queries.size()+" queries with threshold "+threshold+" and probability "+probability);
		ExecutorService service = Executors.newFixedThreadPool(numThreads);
		List<Future<MolTriple[]>> futures = new ArrayList<Future<MolTriple[]>>();
		final double fThresh = threshold;
		final double fProb = probability;
		
		for( final IAtomContainer query: queries){
			
			Callable<MolTriple[]> callable = new Callable<MolTriple[]>(){
				
				public MolTriple[] call() throws Exception {
					IMolSearcher searcher = new MolSearcher( db, useTanimoto);
					return searcher.search(query, fThresh, fProb);
				}
			};
			futures.add( service.submit( callable));
		}
		
		
		List<MolTriple[]> results = new ArrayList<MolTriple[]>();
		for(Future<MolTriple[]> future: futures){
			results.add( future.get());
		}
		
		service.shutdown();
		return results;
		
	}
	

	
}
