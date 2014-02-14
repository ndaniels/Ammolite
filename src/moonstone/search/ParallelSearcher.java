package moonstone.search;

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

import moonstone.compression.StructDatabase;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;

import speedysearch.IteratingSDFReader;
import speedysearch.Logger;

public class ParallelSearcher {
	private int numThreads;
	private StructDatabase db;
	private boolean useTanimoto;
	private static final int BATCH_SIZE = 10;
	
	
	public ParallelSearcher( StructDatabase _db, boolean _useTanimoto){
		numThreads = Runtime.getRuntime().availableProcessors();
		db = _db;
		useTanimoto = _useTanimoto;
	}
	
	public void doQuickSearch( String queryFilename, String outFilename ) throws InterruptedException, ExecutionException, IOException, CDKException{
		int batchSize = BATCH_SIZE*numThreads; // Won't try to read an entire large file at once
		List<IAtomContainer> queries = new ArrayList<IAtomContainer>( batchSize);
		IteratingSDFReader queryFile = new IteratingSDFReader( new FileInputStream( new File( queryFilename)), DefaultChemObjectBuilder.getInstance());
		SDFWriter writer = new SDFWriter(new BufferedWriter( new FileWriter( outFilename + ".sdf" )));
		while( queryFile.hasNext() ){
			int count=0;
			while( queryFile.hasNext() && count < batchSize){
				queries.add( queryFile.next());
				count++;
			}
			for( MoleculeTriple[] tripArray : parallelQuickSearch( queries)){
				Logger.experiment("NumResults: "+tripArray.length);
				for( MoleculeTriple triple : tripArray){
				    writer.write(triple.getQuery());
				    writer.write(triple.getMatch());
				    writer.write(triple.getOverlap().get(0));
				    Logger.experiment(triple.sizes());
				}
			}
		}
		queryFile.close();
		writer.close();
	}
	
	public List<MoleculeTriple[]> parallelQuickSearch(List<IAtomContainer> queries) throws InterruptedException, ExecutionException{
		
		ExecutorService service = Executors.newFixedThreadPool(numThreads);
		List<Future<MoleculeTriple[]>> futures = new ArrayList<Future<MoleculeTriple[]>>();
		
		for( final IAtomContainer query: queries){
			
			Callable<MoleculeTriple[]> callable = new Callable<MoleculeTriple[]>(){
				
				public MoleculeTriple[] call() throws Exception {
					MoleculeSearcher searcher = new MoleculeSearcher( db, useTanimoto);
					return searcher.quickSearch(query);
				}
			};
			futures.add( service.submit( callable));
		}

		
		List<MoleculeTriple[]> results = new ArrayList<MoleculeTriple[]>();
		for(Future<MoleculeTriple[]> future: futures){
			results.add( future.get());
		}
		
		service.shutdown();
		return results;
		
	}
	
	public void doBigSearch( String queryFilename, String outFilename, double threshold ) throws InterruptedException, ExecutionException, IOException, CDKException{
		int batchSize = 10*numThreads; // Won't try to read an entire large file at once
		List<IAtomContainer> queries = new ArrayList<IAtomContainer>( batchSize);
		IteratingSDFReader queryFile = new IteratingSDFReader( new FileInputStream( new File( queryFilename)), DefaultChemObjectBuilder.getInstance());
		SDFWriter writer = new SDFWriter(new BufferedWriter( new FileWriter( outFilename + ".sdf" )));
		while( queryFile.hasNext() ){
			int count=0;
			while( queryFile.hasNext() && count < batchSize){
				queries.add( queryFile.next());
				count++;
			}
			for( MoleculeTriple[] tripArray : parallelBigSearch( queries, threshold)){
				Logger.experiment("NumResults: "+tripArray.length);
				for( MoleculeTriple triple : tripArray){
				    writer.write(triple.getQuery());
				    writer.write(triple.getMatch());
				    writer.write(triple.getOverlap().get(0));
				    Logger.experiment(triple.sizes());
				}
			}
		}
		queryFile.close();
		writer.close();
	}
	
	public List<MoleculeTriple[]> parallelBigSearch(List<IAtomContainer> queries, double threshold) throws InterruptedException, ExecutionException{
		
		ExecutorService service = Executors.newFixedThreadPool(numThreads);
		List<Future<MoleculeTriple[]>> futures = new ArrayList<Future<MoleculeTriple[]>>();
		final double fThresh = threshold;
		
		for( final IAtomContainer query: queries){
			
			Callable<MoleculeTriple[]> callable = new Callable<MoleculeTriple[]>(){
				
				public MoleculeTriple[] call() throws Exception {
					MoleculeSearcher searcher = new MoleculeSearcher( db, useTanimoto);
					return searcher.bigSearch(query, fThresh);
				}
			};
			futures.add( service.submit( callable));
		}
		
		
		List<MoleculeTriple[]> results = new ArrayList<MoleculeTriple[]>();
		for(Future<MoleculeTriple[]> future: futures){
			results.add( future.get());
		}
		
		service.shutdown();
		return results;
		
	}
	
}
