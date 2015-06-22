package edu.mit.csail.ammolite.search;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import edu.mit.csail.ammolite.utils.ID;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;

public class SimpleResultHandler implements IResultHandler {
    PrintStream stream;
    List<SearchMatch> matches = new LinkedList<SearchMatch>();
    
    public SimpleResultHandler(){
        stream = System.out;
    }

    public SimpleResultHandler(PrintStream _stream){
        stream = _stream;
    }
    
    public void printHeader(){
        stream.println("ID1, ID2, Size1, Size2, Overlap, Tanimoto");
    }

    public void handleCoarse(SearchMatch match) {
        // Do nothing. For now.
        
    }

    public void handleFine(SearchMatch match) {
            matches.add(match);
            
    }
    
    public boolean recordingStructures(){
        return false;
    }
    
    public void finishOneQuery(){
        printHeader();
        for(SearchMatch match: matches){
            stream.print( MolUtils.getPubID( match.query));
            stream.print(", ");
            stream.print( MolUtils.getPubID( match.getTarget()));
            stream.print(", ");
            stream.print(MolUtils.getAtomCountNoHydrogen(match.query));
            stream.print(", ");
            stream.print(MolUtils.getAtomCountNoHydrogen( match.getTarget()));
            stream.print(", ");
            stream.print(match.getOverlap());
            stream.print(", ");
            stream.print(MCSUtils.tanimotoCoeff(match.getOverlap(), match.query, match.getTarget()));
            stream.println();
        }
    }

}
