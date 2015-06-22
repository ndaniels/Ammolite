package edu.mit.csail.ammolite.mcs;

public class InterfaceFMCS {
    
    static {
        System.loadLibrary("fmcsjni");
    }
    
    private native int mcsSize(String compoundA, String compoundB);
    
    public int getMCSSize(String sdfA, String sdfB){
        int a = this.mcsSize(sdfA, sdfB);

        return a;
    }

}
