package edu.mit.csail.fmcsj;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

public abstract class AbstractMCS {
	protected IAtomContainer smallCompound;
	protected IAtomContainer bigCompound;
	
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
	
	abstract public long calculate();
	abstract public int size();
	abstract public List<IAtomContainer> getSolutions();
	
	public IAtomContainer getCompoundOne(){
		return smallCompound;
	}
	
	public IAtomContainer getCompoundTwo(){
		return bigCompound;
	}
	
}
