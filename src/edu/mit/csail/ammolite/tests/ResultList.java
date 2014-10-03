package edu.mit.csail.ammolite.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.interfaces.IAtomContainer;

public class ResultList extends ArrayList<SearchResult> {
    
    private Map<IAtomContainer, Integer> locationMap = new HashMap<>();
    
    public ResultList(int size){
        super( size);
    }
    
    public ResultList(){
        super();
    }
    
    public ResultList(List<IAtomContainer> queries, String name){
        this(queries.size());
        for(IAtomContainer query: queries){
            locationMap.put(query, this.size());
            this.add(new SearchResult(query, name));
        }
    }
    
    public SearchResult get(IAtomContainer query){
        return this.get(locationMap.get(query));
    }
    
    public void startAllResults(){
        for(SearchResult r: this){
            r.start();
        }
    }
    
    public void endAllResults(){
        for(SearchResult r: this){
            r.end();
        }
    }

}
