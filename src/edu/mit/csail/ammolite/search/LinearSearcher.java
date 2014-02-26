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
import edu.mit.csail.fmcsj.MCS;

public class LinearSearcher implements IBatchSearcher {
	private static int numThreads = Runtime.getRuntime().availableProcessors();
	private boolean useTanimoto;
	private static final int BATCH_SIZE = 1;// 10*numThreads;

	@Override
	public void search(String databaseFilename, String queryFilename,
			String outFilename, double threshold, double probability,
			boolean _useTanimoto) {
		useTanimoto = _useTanimoto;
		
		useTanimoto = _useTanimoto;
		try {
			doSearch( databaseFilename, queryFilename, outFilename, threshold);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}

	}
	
	private void doSearch(String dbFilename, String queryFilename, 
							String outFilename, double threshold) throws IOException, InterruptedException, ExecutionException, CDKException{
		IteratingSDFReader db = openSDF( dbFilename);
		IteratingSDFReader queryFile = openSDF( queryFilename);
		List<IAtomContainer> queries = new ArrayList<IAtomContainer>(BATCH_SIZE);
		SDFWriter writer = new SDFWriter(new BufferedWriter( new FileWriter( outFilename + ".sdf" )));
		while( queryFile.hasNext() ){
			int count=0;
			while( queryFile.hasNext() && count < BATCH_SIZE){
				queries.add( queryFile.next());
				count++;
			}
			for( MolTriple[] tripArray : parallelLinearSearch( queries, db, threshold)){
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
		db.close();
	}
	
	private List<MolTriple[]> parallelLinearSearch(List<IAtomContainer> queries, IteratingSDFReader db, double threshold) throws InterruptedException, ExecutionException{
		edu.mit.csail.ammolite.Logger.debug("Searching for "+queries.size()+" queries with threshold "+threshold+" linearly");
		ExecutorService service = Executors.newFixedThreadPool(numThreads);
		List<Future<MolTriple[]>> futures = new ArrayList<Future<MolTriple[]>>();
		final double fThresh = threshold;
		final IteratingSDFReader fdb = db;
		
		for( final IAtomContainer query: queries){
			
			Callable<MolTriple[]> callable = new Callable<MolTriple[]>(){
				
				public MolTriple[] call() throws Exception {
					IAtomContainer target;
					List<MolTriple> results = new ArrayList<MolTriple>();
					while( fdb.hasNext()){
						target = fdb.next();
						MCS myMCS = new MCS( query, target);
						myMCS.calculate();
						if( (useTanimoto && fThresh <= Util.tanimotoCoeff(myMCS.size(), query.getAtomCount(), target.getAtomCount())
							|| ( fThresh <= Util.overlapCoeff(myMCS.size(), query.getAtomCount(), target.getAtomCount()))	)){
							results.add(new MolTriple(myMCS.getSolutions(), query, target));
						}
					}
					return results.toArray(new MolTriple[0]);
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
	
	private IteratingSDFReader openSDF(String filename){
		try {
			return new IteratingSDFReader( new FileInputStream( new File( filename)), DefaultChemObjectBuilder.getInstance());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			edu.mit.csail.ammolite.Logger.error("SDF file not found");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	@Override
	public List<MolTriple[]> search(StructDatabase _db,
			List<IAtomContainer> queries, double threshold,
			double repThreshold, boolean _useTanimoto) {
		throw new UnsupportedOperationException();
		
	}

}
