package edu.mit.csail.ammolite.mcs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.LabeledMolStruct;
import edu.mit.csail.ammolite.compression.LabeledWeightedGraph;
import edu.mit.csail.ammolite.utils.FloydsAlgorithm;

public class StringApproximator {
    
    
    private static Map<String, Integer> getShortestPathKMers(Map<Integer,Map<Integer,String>> shortestPathsMatrix, int n){
        
        
        List<String> shortestPaths = new ArrayList<String>();
        for(int i=0; i<shortestPathsMatrix.size(); i++){
            for(int j=0; j<shortestPathsMatrix.size(); j++){
                shortestPaths.add( shortestPathsMatrix.get(i).get(j));
            }
        }
        
        Map<String, Integer> mers = new HashMap<String,Integer>();
        
        for(String s: shortestPaths){
            for(int i=0; i<s.length()-n+1; i++){
                String mer = s.substring(i,i+n);
                int val = 1;
                if(mers.containsKey(mer)){
                    val = mers.get(mer)+1;
                }
                mers.put(mer, val);
            }
        }
        
        return mers;
    }
    
    private static List<String> getAllPairsOfNeighbours(IAtomContainer mol){
        List<String> neighbours = new ArrayList<String>();
        
        for(IAtom a: mol.atoms()){
            for(IAtom n: mol.getConnectedAtomsList(a)){
                neighbours.add(a.getSymbol() + n.getSymbol());
            }
        }
        return neighbours;
    }
    
    private static Map<String, Integer> buildFrequencyMap(List<String> l){
        Map<String, Integer> m = new HashMap<String,Integer>();
        for(String s:l){
            int val = 1;
            if(m.containsKey(s)){
                val = m.get(s) + 1;
            }
            m.put(s, val);
        }
        return m;
    }
    
    private static List<String> listLabels(IAtomContainer m){
        List<String> labels = new ArrayList<String>(m.getAtomCount());
        for(IAtom atom: m.atoms()){
            labels.add(atom.getSymbol());
        }
        return labels;
    }
    
    private static int countOverlaps(Map<String, Integer> m1, Map<String, Integer> m2){
        int overlaps = 0;
        for(String mer: m1.keySet()){
            if(m2.containsKey(mer)){
                overlaps += Math.min(m1.get(mer), m2.get(mer));
            }
        }
        return overlaps;
    }
    
    public static int upperBoundMCSSize(IAtomContainer m1, IAtomContainer m2){
        Map<String, Integer> labelFreq1 = buildFrequencyMap( listLabels(m1));
        Map<String, Integer> labelFreq2 = buildFrequencyMap( listLabels(m1));
        int labelOverlaps = countOverlaps( labelFreq1, labelFreq2);
        return labelOverlaps;
    }
    
    public static double approximateOverlapCoeff(LabeledMolStruct m1, LabeledMolStruct m2){
        int merSize = 2;
        Map<String, Integer> mers1 = getShortestPathKMers( FloydsAlgorithm.getAllPairsPaths((LabeledWeightedGraph) m1.getGraph()), merSize);
        Map<String, Integer> mers2 = getShortestPathKMers( FloydsAlgorithm.getAllPairsPaths((LabeledWeightedGraph) m2.getGraph()), merSize);
        
        int overlaps = countOverlaps(mers1, mers2);
        
        int numer1 = 0;
        for(String key: mers1.keySet()){
            numer1 += mers1.get(key);
        }
        
        int numer2 = 0;
        for(String key: mers2.keySet()){
            numer2 += mers2.get(key);
        }
        
        int numer = Math.min(numer1*2, numer2*2);
        
        double approxOverlapCoeffRaw = (1.0*overlaps) / numer ;
        
        // Totally Empirical
        if(approxOverlapCoeffRaw < 0.1){
            approxOverlapCoeffRaw *= 9;
        } else {
            approxOverlapCoeffRaw  *= 2;
            approxOverlapCoeffRaw += 0.5;
        }
        
        return approxOverlapCoeffRaw;
    }

}
