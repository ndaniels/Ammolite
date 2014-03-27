package edu.mit.csail.fmcsj;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

public abstract class AbstractMCS {
	protected IAtomContainer smallCompound;
	protected IAtomContainer bigCompound;
	private boolean calculated = false;
	
	public AbstractMCS(IAtomContainer _compoundOne, IAtomContainer _compoundTwo){
		// Compound One is the smaller of the two compounds, by definition.
		if( _compoundOne.getAtomCount() < _compoundTwo.getAtomCount() ){
			smallCompound = _compoundOne;
			bigCompound = _compoundTwo;
		} else {
			smallCompound = _compoundTwo;
			bigCompound = _compoundOne;
		}
	}
	
	abstract protected void myCalculate();
	abstract protected int mySize();
	abstract public List<IAtomContainer> getSolutions();
	
	public long calculate(){
		long startTime = System.currentTimeMillis();
		myCalculate();
		calculated = true;
		return System.currentTimeMillis() - startTime;
	}
	
	public int size(){
		if( calculated){
			return mySize();
		}
		throw new UnsupportedOperationException("Must call calculate before calling size");
	}
	
	public IAtomContainer getCompoundOne(){
		return smallCompound;
	}
	
	public IAtomContainer getCompoundTwo(){
		return bigCompound;
	}
	
}
