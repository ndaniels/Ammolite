package edu.mit.csail.ammolite.mcs;

public class InterfaceFMCS {
    
    static {
        System.loadLibrary("fmcsjni");
    }
    
    private native int mcsSize(String compoundA, String compoundB);
    
    public int getMCSSize(String sdfA, String sdfB){
        int id = Thread.currentThread().hashCode(); 
        System.out.println("Entering C from "+id);
        int a = this.mcsSize(sdfA, sdfB);
        System.out.println("Returned from C from "+id);
        return a;
    }

}
