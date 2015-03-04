package edu.mit.csail.ammolite.mcs;

import java.util.concurrent.Callable;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.utils.MCSUtils;

public class MCS {
	
	public static int getSizeOfOverlap(IAtomContainer a, IAtomContainer b){
		return getSMSDOverlap(a,b);
	}
	
	public static int getSizeOfOverlap(MolStruct a, MolStruct b){
		return 0;
	}
	
	
	public static boolean beatsOverlapThreshold(IAtomContainer a, IAtomContainer b, double threshold){
		
		
		return false;
		
	}
	

	
	public static boolean beatsOverlapThresholdSMSD(IAtomContainer a, IAtomContainer b, double threshold){
		double overlapCoeff = MCSUtils.overlapCoeff(getSMSDOverlap(a,b), a, b);
		if(overlapCoeff >= threshold )
			return true;
		return false;
	}
	
	
	public static int getFMCSOverlap(IAtomContainer a, IAtomContainer b){
	    return -48;
	}

	
	
	public static int getSMSDOverlap(IAtomContainer a, IAtomContainer b){
		return getTimedSMSDOverlap(a,b,5*60*1000);
	}
	
	public static int getTimedSMSDOverlap(IAtomContainer a, IAtomContainer b, long timeout){
        SMSD smsd = new SMSD(a,b);
        smsd.timedCalculate(timeout);
        return smsd.size();
    }
	
	@Deprecated
	public static IAtomContainer getMCS(IAtomContainer a, IAtomContainer b){		
		SMSD smsd = new SMSD(a,b);
		smsd.timedCalculate(2000);
		return smsd.getFirstSolution();
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
	
   public static Callable<MCSOperation> getCallableSMSDOperation(IAtomContainer query, IAtomContainer target){
        final IAtomContainer fa = query;
        final IAtomContainer fb = target;
        
        Callable<MCSOperation> callable = new Callable<MCSOperation>(){
            
            public MCSOperation call() throws Exception {
                MCSOperation op = new MCSOperation();
                op.query = fa;
                op.target = fb;
                op.overlap = getSMSDOverlap(fa, fb);
                return op;
            }
        };
        
        return callable;
    }

}
