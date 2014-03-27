package edu.mit.csail.fmcsj;

import java.io.IOException;
import java.util.List;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smsd.Isomorphism;
import org.openscience.cdk.smsd.interfaces.Algorithm;



public class SMSD extends AbstractMCS {
	Isomorphism comparison = null;

	public SMSD(IAtomContainer _compoundOne, IAtomContainer _compoundTwo) {
		super(_compoundOne, _compoundTwo);

	}

	@Override
	protected void myCalculate() {
		boolean bondSensitive = true;
        boolean ringmatch = false;
        boolean stereoMatch = true;
        boolean fragmentMinimization = true;
        boolean energyMinimization = true;

      //Bond Sensitive is set true
        comparison = new Isomorphism(Algorithm.DEFAULT, true);
        // set molecules, remove hydrogens, clean and configure molecule
        try {
			comparison.init(smallCompound, bigCompound, true, true);
		} catch (CDKException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
        comparison.setChemFilters(true, true, true);
       
		
	}

	@Override
	protected int mySize() {
		double t = 0.0;
		try {
			t = comparison.getTanimotoAtomSimilarity();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		int a = smallCompound.getAtomCount();
		int b = bigCompound.getAtomCount();
		double mcsSize = t * (a+b) / (t+1) ;
		return (int) (mcsSize + 0.5);

	}

	@Override
	public List<IAtomContainer> getSolutions() {
		throw new UnsupportedOperationException();
	}

}

