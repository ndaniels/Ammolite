package edu.mit.csail.ammolite.tests;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.Pair;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.StructID;

public class Ammolite_QuerywiseParallel implements Tester {
    
    private static final String NAME = "Ammolite_QuerywiseParallel_QueryCompression";
    private static final int COARSE_CHUNK_SIZE = 50;
    private static final int FINE_CHUNK_SIZE = 50;
    private static final ExecutorService service = ParallelUtils.buildNewExecutorService();

    public Ammolite_QuerywiseParallel() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void test(List<IAtomContainer> queries,
            IStructDatabase db, Iterator<IAtomContainer> targets,
            List<MolStruct> sTargets, double thresh, double prob, String name, PrintStream  out) {
        SearchResultDocumenter scribe = new SearchResultDocumenter( out);
        
        for(IAtomContainer query: queries){
            MolStruct cQuery = db.makeMoleculeStruct(query);
            Pair<SearchResult, List<StructID>> p = coarseSearch(cQuery, query, sTargets, prob);
            
            SearchResult coarseResult = p.left();
            List<StructID> coarseHits = p.right();
            System.out.println("Writing coarse results...");
            scribe.documentSingleResult(coarseResult);
            // Make sure the JVM automatically garbage collects these bad boys
            p = null;
            coarseResult = null;
            
            SearchResult fineResult = fineSearch(query, coarseHits, db, thresh);
            scribe.documentSingleResult(fineResult);
            System.out.println("Writing fine results...");
            fineResult = null;
        }
        service.shutdown();
    }
    
    private Pair<SearchResult, List<StructID>> coarseSearch(MolStruct cQuery, IAtomContainer query, List<MolStruct> sTargets,        
                                                            double thresh){
        
        CommandLineProgressBar bar = new CommandLineProgressBar(MolUtils.getPubID(query).toString() + "_COARSE", sTargets.size());
        SearchResult coarseResult = new SearchResult(cQuery, getName() + "_COARSE");
        coarseResult.start();
        List<StructID> hits = new ArrayList<StructID>();
   
        List<Callable<Integer>> tests = new ArrayList<Callable<Integer>>(COARSE_CHUNK_SIZE);
        int startOfChunk = 0;
        for(int i=0; i<sTargets.size(); i++){
            MolStruct target = sTargets.get(i);
            tests.add(MCS.getCallableSMSDOperation(cQuery, target));

            if(tests.size() == COARSE_CHUNK_SIZE){
                List<Integer> testResults = ParallelUtils.parallelFullExecution(tests, service);
                for(int j=0; j<tests.size(); j++){
                    int molInd = startOfChunk + j;
                    int overlap = testResults.get(j);
                    MolStruct mol = sTargets.get(molInd);
                    
                    if(MCSUtils.overlapCoeff(overlap, mol, cQuery) > thresh){
                        coarseResult.addMatch(new SearchMatch(cQuery, mol, overlap));
                        hits.add(MolUtils.getStructID(mol));
                    } else {
                        coarseResult.addMiss(new SearchMiss(cQuery, mol, overlap));
                    }
                    bar.event();
                }
                startOfChunk = i+1;
                tests.clear();
            }
        }
        List<Integer> testResults = ParallelUtils.parallelFullExecution(tests, service);
        for(int j=0; j<tests.size(); j++){
            int molInd = startOfChunk + j;
            int overlap = testResults.get(j);
            MolStruct mol = sTargets.get(molInd);
            
            if(MCSUtils.overlapCoeff(overlap, mol, cQuery) > thresh){
                coarseResult.addMatch(new SearchMatch(cQuery, mol, overlap));
                hits.add(MolUtils.getStructID(mol));
            } else {
                coarseResult.addMiss(new SearchMiss(cQuery, mol, overlap));
            }
            bar.event();
        }
        coarseResult.end();
        return new Pair<SearchResult, List<StructID>>(coarseResult,hits);    
    }

    
    private SearchResult fineSearch(IAtomContainer query, List<StructID> coarseHits, IStructDatabase db, double thresh){
        CommandLineProgressBar bar = new CommandLineProgressBar(MolUtils.getPubID(query).toString() + "_FINE", coarseHits.size());
        SearchResult result = new SearchResult(query, getName() + "_FINE");
        result.start();
        List<Callable<List<SearchComparison>>> tests = new ArrayList<Callable<List<SearchComparison>>>(FINE_CHUNK_SIZE);
        for(StructID coarseHit: coarseHits){
            tests.add(getCallableFineTest(query, coarseHit, db, thresh));
            if(tests.size() == FINE_CHUNK_SIZE){
                List<List<SearchComparison>> matches = ParallelUtils.parallelFullExecution(tests);
                for(List<SearchComparison> cList: matches){
                    for(SearchComparison comp: cList){
                        if(comp.isMatch()){
                            result.addMatch((SearchMatch) comp); 
                        } else {
                            result.addMiss((SearchMiss) comp);
                        }
                    }
                    bar.event();
                }
                tests.clear();
            }
        }
        List<List<SearchComparison>> matches = ParallelUtils.parallelFullExecution(tests);
        for(List<SearchComparison> cList: matches){
            for(SearchComparison comp: cList){
                if(comp.isMatch()){
                    result.addMatch((SearchMatch) comp); 
                } else {
                    result.addMiss((SearchMiss) comp);
                }
            }
            bar.event();
        }
        result.end();
        return result;
        
    }
    
    private Callable<List<SearchComparison>> getCallableFineTest(final IAtomContainer query, final StructID coarseHit, final IStructDatabase db, final double thresh){
        return new Callable<List<SearchComparison>>(){

            @Override
            public List<SearchComparison> call() throws Exception {
                List<SearchComparison> searchList = new ArrayList<SearchComparison>();
                List<IAtomContainer> targets = db.getMatchingMolecules(coarseHit);
                for(IAtomContainer target: targets){
                    int overlap = MCS.getSMSDOverlap(target, query);
                    if(MCSUtils.overlapCoeff(overlap, query, target) > thresh){
                        searchList.add(new SearchMatch(query, target, overlap));
                    } else {
                        searchList.add(new SearchMiss(query, target, overlap));
                    }
                }
                return searchList;
            }
            
        };
    }
    
    @Override
    public String getName() {
        return NAME;
    }

}
