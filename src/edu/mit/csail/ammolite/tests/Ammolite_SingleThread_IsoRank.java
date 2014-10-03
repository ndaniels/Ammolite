package edu.mit.csail.ammolite.tests;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.tests.AmmoliteTester;

public class Ammolite_SingleThread_IsoRank extends AmmoliteTester {
    private static final String NAME = "Ammolite_SingleThread_IsoRank";
    
    int getCoarseOverlap(MolStruct query, MolStruct target) {
        return MCS.getIsoRankOverlap(target, query);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
