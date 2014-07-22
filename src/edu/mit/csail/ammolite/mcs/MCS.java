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
		return getIsoRankOverlap(a,b);
	}
	
	
	public static boolean beatsOverlapThreshold(IAtomContainer a, IAtomContainer b, double threshold){
		
		if(a instanceof MolStruct && b instanceof MolStruct){
			int isoOverlap = getIsoRankOverlap((MolStruct) a, (MolStruct) b);
			double isoCoeff = MCSUtils.overlapCoeff(isoOverlap, a.getAtomCount(), b.getAtomCount());
			if( isoCoeff < threshold){
				return false;
			}
		}
		

		int standardOverlap = getSMSDOverlap(a,b);
		double standardCoeff = MCSUtils.overlapCoeff(standardOverlap, a, b);
		if( standardCoeff >= threshold){
			return true;
		}
		return false;
		
	}
	
	public static boolean beatsOverlapThresholdIsoRank(MolStruct a, MolStruct b, double threshold){
		int isoOverlap = getIsoRankOverlap(a, b);
		double isoCoeff = MCSUtils.overlapCoeff(isoOverlap, a, b);
		if( isoCoeff < threshold){
			return false;
		}
		return true;
	}
	
	public static boolean beatsOverlapThresholdSMSD(IAtomContainer a, IAtomContainer b, double threshold){
		double overlapCoeff = MCSUtils.overlapCoeff(getSMSDOverlap(a,b), a, b);
		if(overlapCoeff >= threshold )
			return true;
		return false;
	}
	
	public static boolean beatsOverlapThresholdFMCS(IAtomContainer a, IAtomContainer b, double threshold){
		double overlapCoeff = MCSUtils.overlapCoeff(getFMCSOverlap(a,b), a, b);
		if(overlapCoeff >= threshold )
			return true;
		return false;
	}
	
	public static int getIsoRankOverlap(MolStruct a, MolStruct b){
		IsoRank iso = new IsoRank(a,b);
		iso.calculate();
		return iso.size();
	}
	
	
	public static int getSMSDOverlap(IAtomContainer a, IAtomContainer b){
		SMSD smsd = new SMSD(a,b);
		smsd.timedCalculate(2000);
		return smsd.size();
	}
	
	public static IAtomContainer getMCS(IAtomContainer a, IAtomContainer b){		
		SMSD smsd = new SMSD(a,b);
		smsd.timedCalculate(2000);
		return smsd.getFirstSolution();
}
	
	public static int getFMCSOverlap(IAtomContainer a, IAtomContainer b){
		FMCS fmcs = new FMCS(a,b);
		fmcs.timedCalculate(2000);
		return fmcs.size();
	}

	public static Callable<Boolean> getCallableIsoRankTest(MolStruct a, MolStruct b, double threshold){
		final MolStruct fa = a;
		final MolStruct fb = b;
		final double fThresh = threshold;
		
		Callable<Boolean> callable = new Callable<Boolean>(){
			
			public Boolean call() throws Exception {
				return beatsOverlapThresholdIsoRank(fa, fb, fThresh);
			}
		};
		
		return callable;
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

}
