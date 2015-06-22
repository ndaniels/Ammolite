package edu.mit.csail.ammolite.search;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.ID;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.PubchemID;

public class SearchResult {
    private long startTime;
    private long endTime;
    private long duration = -1;
    public IAtomContainer query;
    public IAtomContainer target = null;
    public IAtomContainer mcs = null;
    public String methodName;
    public List<SearchMatch> matches = new LinkedList<SearchMatch>();

    
    public SearchResult(IAtomContainer q, String _methodName){
        query = q;
        methodName = _methodName;
    }
    
    public void start(){
        start(System.currentTimeMillis());
    }
    
    public void start(long _startTime){
        startTime = _startTime;
    }
    
    public void end(){
        end( System.currentTimeMillis());
    }
    
    public void end(long _endTime){
        endTime = _endTime;
        if(duration < 0){
            duration = 0;
        }
        duration += endTime - startTime;
    }
    
    public void setDuration(long _duration){
        duration = _duration;
    }
    
    public long time(){
        if(duration < 0){
            duration = endTime - startTime;
        }
        return duration;
    }
        
    public synchronized void addMatch(SearchMatch match){
        matches.add( match);

    }
    
    
}
