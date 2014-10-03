package edu.mit.csail.ammolite.tests;

import java.util.ArrayList;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

public class SearchResult {
    private long startTime;
    private long endTime;
    private long duration = -1;
    public IAtomContainer query;
    public String methodName;
    public List<IAtomContainer> matches = new ArrayList<IAtomContainer>();
    public List<Integer> matchSizes = new ArrayList<Integer>();
    
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
    
    public void addMatch(IAtomContainer match, int mcsSize){
        matches.add(match);
        matchSizes.add(mcsSize);
    }
    
    public void addMatch(SearchMatch match){
        if( match.getQuery() != query){
            throw new IllegalArgumentException("Queries do not match");
        }
        matches.add( match.getTarget());
        matchSizes.add( match.getOverlap());
    }
}
