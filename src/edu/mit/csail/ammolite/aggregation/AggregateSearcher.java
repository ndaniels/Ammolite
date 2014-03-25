package edu.mit.csail.ammolite.aggregation;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.Logger;
import edu.mit.csail.ammolite.compression.CyclicStruct;
import edu.mit.csail.ammolite.compression.MoleculeStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;
import edu.mit.csail.ammolite.search.MolTriple;
import edu.mit.csail.ammolite.search.Util;
import edu.mit.csail.fmcsj.FMCS;

public class AggregateSearcher {
	private IStructDatabase db;
	private List<Cluster> cList;
	private static double searchBound;
	private boolean useTanimoto = false;
	private ClusterDatabase cDB;
	
	public AggregateSearcher(String clusterDBName, double _searchBound){
		searchBound = _searchBound;
		cDB = ClusterDatabaseDecompressor.decompress(clusterDBName);
		db = cDB.getDatabase();
		cList = cDB.getClusterList();
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
	public void doSearch( String queryFilename, String outFilename, double threshold , boolean useTanimoto) throws InterruptedException, ExecutionException, IOException, CDKException{


		IteratingSDFReader queryFile = new IteratingSDFReader( new FileInputStream( new File( queryFilename)), DefaultChemObjectBuilder.getInstance());
		SDFWriter writer = new SDFWriter(new BufferedWriter( new FileWriter( outFilename + ".sdf" )));
		
		while( queryFile.hasNext() ){
			IAtomContainer query = db.makeMoleculeStruct(queryFile.next());
			List<MolTriple> results = singleSearch( query,threshold, useTanimoto);
			Logger.experiment("Query ID: "+query.getID()+", Matches: "+results.size());
			StringBuilder sb = new StringBuilder();
			
			for( MolTriple triple : results){
				if( outFilename.equals("DEV-TEST")){
					
					int overlap = triple.getOverlap().get(0).getAtomCount();
					int a = triple.getQuery().getAtomCount();
					int b = triple.getMatch().getAtomCount();
					sb.append( Util.overlapCoeff(overlap, a, b));
					sb.append(" ");
					sb.append( Util.tanimotoCoeff(overlap, a, b));
					sb.append(" ");
					
				} else {
				    writer.write(triple.getQuery());
				    writer.write(triple.getMatch());
				    writer.write(triple.getOverlap().get(0));
				}
			}
			if( outFilename.equals("DEV-TEST")){
				Logger.experiment(sb.toString());
			}
		}
		queryFile.close();
		writer.close();
	}
	
	private List<MolTriple> singleSearch(IAtomContainer query, double threshold, boolean _useTanimoto){
		useTanimoto = _useTanimoto;
		List<IAtomContainer> matches = new ArrayList<IAtomContainer>();
		List<Cluster> myCList = new ArrayList<Cluster>(cList);
		
		while( myCList.size() > 0){
			Cluster c = myCList.get(0);
			if(c.order() == 0){
				matches.add(c.getRep());
			} else if( c.matchesCluster(query, Math.pow( searchBound, c.order()))){
				myCList.addAll(c.getMembers());
			}
			myCList.remove(c);
		}
		List<String> ids = new ArrayList<String>(3 * matches.size());
		for(int i=0;i<matches.size(); i++){
			IAtomContainer match = matches.get(i);
			if( match instanceof MoleculeStruct){
				MoleculeStruct sMatch = (MoleculeStruct) match;
				for(String id: sMatch.getIDNums()){
					ids.add(id);
				}
			} else {
				ids.add(match.getID());
			}
		}
		Logger.debug(ids);
		return thresholdMoleculeMatches(query, ids, threshold);
	}
	
	private List<MolTriple> thresholdMoleculeMatches( IAtomContainer query, List<String>  targetIDs, double threshold){
		IAtomContainer target;
		List<MolTriple> matches = new ArrayList<MolTriple>();
		
		for(String id: targetIDs){
			target = db.getMolecule(id);
			target = new AtomContainer(AtomContainerManipulator.removeHydrogens(target));
			FMCS myMCS = new FMCS(query,target);
			myMCS.calculate();
			double myCoeff = coeff( myMCS.size(), myMCS.getCompoundOne().getAtomCount(), myMCS.getCompoundTwo().getAtomCount());
			//Logger.debug(myCoeff);
			if(myCoeff > threshold){
				matches.add( new MolTriple( myMCS.getSolutions(), query, target));
				
			}
		}
		
		return matches;
	}
	
	private double coeff(int overlap, int a, int b){
		if( useTanimoto){
			return Util.tanimotoCoeff(overlap, a, b);
		}
		return Util.overlapCoeff(overlap, a, b);
		
	}
	
	
	
	

}
