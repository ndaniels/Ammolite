package edu.mit.csail.ammolite.mcs;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.mcs.SMSD;

public class MCSFinder extends SMSD {

	public MCSFinder(IAtomContainer _compoundOne, IAtomContainer _compoundTwo) {
		super(_compoundOne, _compoundTwo);
	}

}
