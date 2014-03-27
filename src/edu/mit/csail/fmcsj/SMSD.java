package edu.mit.csail.fmcsj;

import java.util.List;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.smsd.Isomorphism;
import org.openscience.smsd.interfaces.Algorithm;



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

        comparison = new Isomorphism(smallCompound, bigCompound, Algorithm.DEFAULT, bondSensitive, ringmatch, true);
        comparison.setChemFilters(stereoMatch, fragmentMinimization, energyMinimization);

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

