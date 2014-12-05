package edu.mit.csail.ammolite.tests;

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
    public String methodName;
    public List<ID> matches = new LinkedList<ID>();
    public List<Integer> matchSizes = new LinkedList<Integer>();
    public List<ID> misses = new LinkedList<ID>();
    public List<Integer> missSizes = new LinkedList<Integer>();
    public List<ID> timeouts = new LinkedList<ID>();
    
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
    
    public synchronized void addMatch(IAtomContainer match, int mcsSize){
        matches.add(MolUtils.getUnknownOrID(match));
        matchSizes.add(mcsSize);
    }
    
    public synchronized void addMatch(SearchMatch match){
        matches.add( MolUtils.getUnknownOrID(match.getTarget()));
        matchSizes.add( match.getOverlap());
    }
    
    public synchronized void addMiss(IAtomContainer miss, int mcsSize){
        misses.add(MolUtils.getUnknownOrID(miss));
        missSizes.add(mcsSize);
    }
    
    public synchronized void addMiss(SearchMiss miss){
        misses.add( MolUtils.getUnknownOrID(miss.getTarget()));
        missSizes.add( miss.getOverlap());
    }
    
    public synchronized void addTimeout(SearchTimeout timeout){
        timeouts.add( MolUtils.getUnknownOrID(timeout.getTarget()));
    }
}
