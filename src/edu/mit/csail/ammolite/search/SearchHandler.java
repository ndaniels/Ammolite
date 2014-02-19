package edu.mit.csail.ammolite.search;

import edu.mit.csail.ammolite.database.StructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;

public class SearchHandler {
	private SearchType searchType;
	private StructDatabase db;
	
	private enum SearchType{
		LINEAR, CYCLIC, RING 
	}

	public SearchHandler(String databaseFilename, String queryFilename, double threshold, double probability, boolean useTanimoto){
		pickSearchType( threshold,probability, useTanimoto);
		if( searchType != SearchType.LINEAR){
			db = StructDatabaseDecompressor.decompress(databaseFilename);
		}
		
	}
	
	public void handleSearch(){
		return;
	}
	
	private void pickSearchType(double threshold, double probability, boolean useTanimoto){
		searchType = SearchType.RING;
	}
	
}
