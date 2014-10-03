package edu.mit.csail.ammolite.tests;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.mcs.MCS;

public class Ammolite_SingleThread_SMSD extends AmmoliteTester {
    private static final String NAME = "Ammolite_SingleThread_SMSD";
    
    int getCoarseOverlap(MolStruct query, MolStruct target) {
        return MCS.getSMSDOverlap(target, query);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
