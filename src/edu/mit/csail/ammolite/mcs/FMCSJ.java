package edu.mit.csail.ammolite.mcs;

public class FMCSJ {
    
    static {
        System.loadLibrary("fmcs-jni");
    }
    
    private native int mcsSize(String compoundA, String compoundB);
    
    

}
