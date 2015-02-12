package edu.mit.csail.ammolite.mcs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.smsd.AtomAtomMapping;
import org.openscience.smsd.Isomorphism;
import org.openscience.smsd.interfaces.Algorithm;

import edu.mit.csail.ammolite.mcs.AbstractMCS;

public class SMSD extends AbstractMCS {
	protected Isomorphism comparison = null;

	public SMSD(IAtomContainer _compoundOne, IAtomContainer _compoundTwo) {
		super(_compoundOne, _compoundTwo);

	}

	@Override
	protected void myCalculate() {
		boolean bondSensitive = false;
        boolean ringmatch = false;
        
        boolean stereoMatch = true;
        boolean fragmentMinimization = true;
        boolean energyMinimization = true;

        
        
        comparison = new Isomorphism(smallCompound, bigCompound, Algorithm.DEFAULT, bondSensitive, ringmatch);
        //comparison.setChemFilters(stereoMatch, fragmentMinimization, energyMinimization);     
		
	}

	@Override
	protected int mySize() {
		if( comparison == null){
			throw new RuntimeException("SMSD-INTERFACE: comparison is null");
		}
		
		AtomAtomMapping aaMap = comparison.getFirstAtomMapping();
//		Map<IAtom,IAtom>  mappingsByAtom  = aaMap.getMappingsByAtoms();
		
		return aaMap.getCount();

	}
	
	public IAtomContainer myGetFirstSolution(){
//		try {
////			return comparison.getFirstAtomMapping().getCommonFragment();
//		} catch (CloneNotSupportedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			System.exit(1);
//		}
		return null;
	}

	@Override
	public List<IAtomContainer> myGetSolutions() {
		List<IAtomContainer> out = new ArrayList<IAtomContainer>(1);
		out.add(getFirstSolution());
		return out;
		
	}

}

