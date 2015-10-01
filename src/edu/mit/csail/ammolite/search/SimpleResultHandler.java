package edu.mit.csail.ammolite.search;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import edu.mit.csail.ammolite.utils.ID;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;

public class SimpleResultHandler implements IResultHandler {
    PrintStream stream;
    List<ISearchMatch> matches = new LinkedList<ISearchMatch>();
    
    public SimpleResultHandler(){
        stream = System.out;
    }

    public SimpleResultHandler(PrintStream _stream){
        stream = _stream;
    }
    
    public void printHeader(){
        stream.println("ID1, ID2, Size1, Size2, Overlap, Tanimoto");
    }

    public void handleCoarse(ISearchMatch match) {
        // Do nothing. For now.
        
    }

    public void handleFine(ISearchMatch match) {
            matches.add(match);
            
    }
    
    public boolean recordingStructures(){
        return false;
    }
    
    public void finishOneQuery(){
        printHeader();
        for(ISearchMatch match: matches){
            stream.print( match.getQueryID());
            stream.print(", ");
            stream.print( match.getTargetID());
            stream.print(", ");
            stream.print(match.getQuerySize());
            stream.print(", ");
            stream.print(match.getTargetSize());
            stream.print(", ");
            stream.print(match.getOverlap());
            stream.print(", ");
            stream.print(MCSUtils.tanimotoCoeff(match.getOverlap(), match.getQuerySize(), match.getTargetSize()));
            stream.println();
        }
    }

}
