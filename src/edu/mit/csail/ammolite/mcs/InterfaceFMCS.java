package edu.mit.csail.ammolite.mcs;

public class InterfaceFMCS {
    
    static {
        System.loadLibrary("fmcsjni");
    }
    
    private native int mcsSize(String compoundA, String compoundB);
    
    public int getMCSSize(String sdfA, String sdfB){
        System.out.println("Entering C");
        int a = this.mcsSize(sdfA, sdfB);
        System.out.println("Returned from C");
        return a;
    }

}
