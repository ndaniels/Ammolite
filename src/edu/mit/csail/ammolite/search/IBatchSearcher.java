package edu.mit.csail.ammolite.search;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.database.StructDatabase;

public interface IBatchSearcher {
	void search(String databaseFilename, String queryFilename, String outFilename, 
						double threshold, double repThreshold, boolean _useTanimoto);
	
	List<MolTriple[]> search(StructDatabase _db, List<IAtomContainer> queries, 
						double threshold, double repThreshold, boolean _useTanimoto);
}
