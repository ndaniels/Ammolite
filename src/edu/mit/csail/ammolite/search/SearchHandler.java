package edu.mit.csail.ammolite.search;

import edu.mit.csail.ammolite.database.StructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;

public class SearchHandler {
	private SearchType searchType;
	
	private enum SearchType{
		LINEAR, CYCLIC, RING 
	}

	public SearchHandler(String databaseFilename, String queryFilename, String outFilename, double threshold, double probability, boolean useTanimoto){
		pickSearchType( threshold,probability, useTanimoto);
		IBatchSearcher searcher = null;
		if( searchType == SearchType.LINEAR){
			searcher = new LinearSearcher();
		}
		else if( searchType == SearchType.CYCLIC){
			searcher = new ParallelSearcher();
		}
		searcher.search(databaseFilename, queryFilename, outFilename, threshold, probability, useTanimoto);
		
	}
	
	public void handleSearch(){

	}
	
	private void pickSearchType(double threshold, double probability, boolean useTanimoto){
		if( probability > 0.98){
			searchType = SearchType.LINEAR;
		} else {
			searchType = SearchType.CYCLIC;
		}
	}
	
}
