package edu.mit.csail.ammolite.mcs;

import java.util.concurrent.Callable;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.utils.MCSUtils;

public class MCS {
	

	
	public static boolean beatsOverlapThresholdSMSD(IAtomContainer a, IAtomContainer b, double threshold){
		double overlapCoeff = MCSUtils.overlapCoeff(getSMSDOverlap(a,b), a, b);
		if(overlapCoeff >= threshold )
			return true;
		return false;
	}
	
	
	
	
	public static int getSMSDOverlap(IAtomContainer a, IAtomContainer b){
		SMSD smsd = new SMSD(a,b);
		smsd.calculate();
		return smsd.size();
	}
	
	public static int getTimedSMSDOverlap(IAtomContainer a, IAtomContainer b){
        SMSD smsd = new SMSD(a,b);
        smsd.timedCalculate(2000);
        return smsd.size();
    }
	
	public static Callable<Boolean> getCallableSMSDTest(IAtomContainer a, IAtomContainer b, double threshold){
		final IAtomContainer fa = a;
		final IAtomContainer fb = b;
		final double fThresh = threshold;
		
		Callable<Boolean> callable = new Callable<Boolean>(){
			
			public Boolean call() throws Exception {
				return beatsOverlapThresholdSMSD(fa, fb, fThresh);
			}
		};
		
		return callable;
	}
	
   public static Callable<Integer> getCallableSMSDOperation(IAtomContainer a, IAtomContainer b){
        final IAtomContainer fa = a;
        final IAtomContainer fb = b;
        
        Callable<Integer> callable = new Callable<Integer>(){
            
            public Integer call() throws Exception {
                return getSMSDOverlap(fa, fb);
            }
        };
        
        return callable;
    }

}
