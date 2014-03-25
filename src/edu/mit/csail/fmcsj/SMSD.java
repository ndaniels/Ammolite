package edu.mit.csail.fmcsj;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smsd.Isomorphism;
import org.openscience.cdk.smsd.interfaces.Algorithm;

public class SMSD extends AbstractMCS {
	Isomorphism smsd = null;

	public SMSD(IAtomContainer _compoundOne, IAtomContainer _compoundTwo) {
		super(_compoundOne, _compoundTwo);

	}

	@Override
	public long calculate() {
		return 0;
		
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public List<IAtomContainer> getSolutions() {
		// TODO Auto-generated method stub
		return null;
	}

}

