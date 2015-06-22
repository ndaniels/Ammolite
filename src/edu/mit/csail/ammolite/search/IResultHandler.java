package edu.mit.csail.ammolite.search;

public interface IResultHandler {
    
    /**
     * @return True iff the structure of overlaps between matching molecules should be recorded
     */
    public boolean recordingStructures();
    
    /**
     * Capture and process a match found in coarse search. Most API applications will not need 
     * this method to do anything.
     * 
     * @param result
     */
    public void handleCoarse(SearchMatch result);
    
    /**
     * Capture and process a match found in fine
     * @param result
     */
    public void handleFine(SearchMatch result);
    public void finishOneQuery();

}
