package edu.mit.csail.ammolite.search;

import edu.mit.csail.ammolite.database.StructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;

public class SearchHandler {
	private SearchType searchType;
	private StructDatabase db;
	
	private enum SearchType{
		LINEAR, CYCLIC, RING 
	}

	public SearchHandler(String databaseFilename, String queryFilename, String outFilename, double threshold, double probability, boolean useTanimoto){
		pickSearchType( threshold,probability, useTanimoto);
		if( searchType != SearchType.LINEAR){
			//db = StructDatabaseDecompressor.decompress(databaseFilename);
		}
		if( searchType == SearchType.CYCLIC){
			IBatchSearcher searcher = new ParallelSearcher();
			searcher.search(databaseFilename, queryFilename, outFilename, threshold, probability, useTanimoto);
		}
		
	}
	
	public void handleSearch(){

	}
	
	private void pickSearchType(double threshold, double probability, boolean useTanimoto){
		searchType = SearchType.CYCLIC;
	}
	
}
