package edu.mit.csail.ammolite.utils;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.mcs.MCS;

public class MCSTableMaker {

    
    public static void printMCSTable(String fileOne, String fileTwo){
        System.out.println("ID1, ID2, Size of 1, Size of 2, MCS Size");
        
        IteratingSDFReader sdfOne = (IteratingSDFReader) SDFUtils.parseSDFOnline(fileOne);
        while( sdfOne.hasNext()){
            IAtomContainer a = sdfOne.next();
            String idA = MolUtils.getPubID(a).toString();
            int sizeA = MolUtils.getAtomCountNoHydrogen(a);
            IteratingSDFReader sdfTwo = (IteratingSDFReader) SDFUtils.parseSDFOnline(fileTwo);
            while( sdfTwo.hasNext()){
                IAtomContainer b = sdfTwo.next();
                String idB = MolUtils.getPubID(b).toString();
                int sizeB = MolUtils.getAtomCountNoHydrogen(b);
                
                int mcs = MCS.getSMSDOverlap(a, b);
                
                System.out.println(idA +", "+ idB+", "+sizeA+", "+sizeB+", "+mcs);
                
            }
        }
    }
}
