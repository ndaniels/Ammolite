package edu.mit.csail.ammolite.search;

public interface IResultHandler {
    
    public boolean recordingStructures();
    public void handleCoarse(SearchMatch result);
    public void handleFine(SearchMatch result);
    public void finishOneQuery();

}
