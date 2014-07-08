package edu.mit.csail.ammolite.mcs;

import java.util.concurrent.Callable;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import edu.mit.csail.ammolite.compression.MoleculeStruct;

public class MCS {
	
	public static int getSizeOfOverlap(IAtomContainer a, IAtomContainer b){
		return getSMSDOverlap(a,b);
	}
	
	public static int getSizeOfOverlap(MoleculeStruct a, MoleculeStruct b){
		return getIsoRankOverlap(a,b);
	}
	
	private static int getAtomCountNoHydrogen(IAtomContainer mol){
		return AtomContainerManipulator.removeHydrogens(mol).getAtomCount();
	}
	
	public static boolean beatsOverlapThreshold(IAtomContainer a, IAtomContainer b, double threshold){
		
		if(a instanceof MoleculeStruct && b instanceof MoleculeStruct){
			int isoOverlap = getIsoRankOverlap((MoleculeStruct) a, (MoleculeStruct) b);
			double isoCoeff = overlapCoeff(isoOverlap, a.getAtomCount(), b.getAtomCount());
			if( isoCoeff < threshold){
				return false;
			}
		}
		

		int standardOverlap = getSMSDOverlap(a,b);
		double standardCoeff = overlapCoeff(standardOverlap, a, b);
		if( standardCoeff >= threshold){
			return true;
		}
		return false;
		
	}
	
	public static boolean beatsOverlapThresholdIsoRank(MoleculeStruct a, MoleculeStruct b, double threshold){
		int isoOverlap = getIsoRankOverlap(a, b);
		double isoCoeff = overlapCoeff(isoOverlap, a, b);
		if( isoCoeff < threshold){
			return false;
		}
		return true;
	}
	
	public static boolean beatsOverlapThresholdSMSD(IAtomContainer a, IAtomContainer b, double threshold){
		double overlapCoeff = overlapCoeff(getSMSDOverlap(a,b), a, b);
		if(overlapCoeff >= threshold )
			return true;
		return false;
	}
	
	public static boolean beatsOverlapThresholdFMCS(IAtomContainer a, IAtomContainer b, double threshold){
		double overlapCoeff = overlapCoeff(getFMCSOverlap(a,b), a, b);
		if(overlapCoeff >= threshold )
			return true;
		return false;
	}
	
	private static int getIsoRankOverlap(MoleculeStruct a, MoleculeStruct b){
		IsoRank iso = new IsoRank(a,b);
		iso.calculate();
		return iso.size();
	}
	
	
	private static int getSMSDOverlap(IAtomContainer a, IAtomContainer b){
		SMSD smsd = new SMSD(a,b);
		smsd.timedCalculate(2000);
		return smsd.size();
	}
	
	private static int getFMCSOverlap(IAtomContainer a, IAtomContainer b){
		FMCS fmcs = new FMCS(a,b);
		fmcs.timedCalculate(2000);
		return fmcs.size();
	}
	
	public static double overlapCoeff(int overlap, IAtomContainer a, IAtomContainer b){
		return overlapCoeff(overlap, getAtomCountNoHydrogen(a), getAtomCountNoHydrogen(b));
	}
	
	public static double overlapCoeff(int overlap, int a, int b){
		if( a < b){
			return ( 1.0*overlap) / a;
		}
		return ( 1.0*overlap) / b;
	}
	
	public static Callable<Boolean> getCallableThresholdTest(IAtomContainer a, IAtomContainer b, double threshold){
		final IAtomContainer fa = a;
		final IAtomContainer fb = b;
		final double fThresh = threshold;
		
		Callable<Boolean> callable = new Callable<Boolean>(){
			
			public Boolean call() throws Exception {
				return beatsOverlapThreshold(fa, fb, fThresh);
			}
		};
		
		return callable;
	}

}
