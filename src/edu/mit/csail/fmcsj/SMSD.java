package edu.mit.csail.fmcsj;

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
	public long calculate() {
		long startTime = System.currentTimeMillis();

		boolean bondSensitive = true;
        boolean ringmatch = false;
        boolean stereoMatch = true;
        boolean fragmentMinimization = true;
        boolean energyMinimization = true;

      //Bond Sensitive is set true
        Isomorphism comparison = new Isomorphism(Algorithm.DEFAULT, true);
        // set molecules, remove hydrogens, clean and configure molecule
        try {
			comparison.init(smallCompound, bigCompound, true, true);
		} catch (CDKException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        comparison.setChemFilters(true, true, true);
        long runTime = System.currentTimeMillis() - startTime;
		return runTime;
		
	}

	@Override
	public int size() {
		return comparison.getFirstAtomMapping().getCount();

	}

	@Override
	public List<IAtomContainer> getSolutions() {
		throw new UnsupportedOperationException();
	}

}

