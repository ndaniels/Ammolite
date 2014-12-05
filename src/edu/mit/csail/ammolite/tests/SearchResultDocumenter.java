package edu.mit.csail.ammolite.tests;

import java.io.PrintStream;
import java.util.List;
import edu.mit.csail.ammolite.utils.MolUtils;

import edu.mit.csail.ammolite.utils.ID;

public class SearchResultDocumenter {

    protected PrintStream out;
    
    public SearchResultDocumenter(PrintStream out) {
        this.out = out;
    }
    
    public void documentAllResults(List<SearchResult> results){
        for(SearchResult r: results){
          documentSingleResult( r);
        }
    }
    
    public void documentSingleResult(SearchResult result){
        out.println("START_QUERY "+result.query.getProperty("PUBCHEM_COMPOUND_CID"));
        out.println("START_METHOD "+result.methodName);
        out.println("time: "+result.time());


        
        docHits(result);
        docMisses(result);
        docTimeouts(result);
      

        out.println("END_METHOD");
        out.println("END_QUERY");
    }
    
    
    private void docHits(SearchResult result){
        out.print("matches: ");
        for(ID match: result.matches){
            out.print(match);
            out.print(" ");
        }
        out.println("\nSTART_DETAILED_MATCHES");
        for(int i=0; i< result.matches.size(); i++){
            ID match = result.matches.get(i);
            int matchSize = result.matchSizes.get(i);
            out.print(match);
            out.print(" ");
            out.println(matchSize);
            
        }
        out.println("END_DETAILED_MATCHES");
    }
    
    private void docMisses(SearchResult result){
        out.print("\n misses: ");
        for(ID miss: result.misses){
            out.print(miss);
            out.print(" ");
        }
        out.println("\nSTART_DETAILED_MISSES");
        for(int i=0; i< result.misses.size(); i++){
            ID miss = result.misses.get(i);
            int missSize = result.missSizes.get(i);
            out.print(miss);
            out.print(" ");
            out.println(missSize);

        }
        out.println("END_DETAILED_MISSES");
    }
    
    private void docTimeouts(SearchResult result){
        out.print("\n timeouts: ");
        for(ID timeout: result.timeouts){
            out.print(timeout);
            out.print(" ");
        }
        out.println("\nSTART_DETAILED_TIMEOUTS");
        for(int i=0; i< result.timeouts.size(); i++){
            ID timeout = result.timeouts.get(i);
            out.print(timeout);

        }
        out.println("END_DETAILED_TIMEOUTS");
    }
    
    public void close(){
        out.close();
    }

}
