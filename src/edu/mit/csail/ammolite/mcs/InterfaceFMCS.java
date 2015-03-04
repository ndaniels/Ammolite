package edu.mit.csail.ammolite.mcs;

public class InterfaceFMCS {
    
    static {
        System.loadLibrary("fmcs-jni");
    }
    
    private native int mcsSize(String compoundA, String compoundB);
    
    

}
