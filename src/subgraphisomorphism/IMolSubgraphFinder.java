package subgraphisomorphism;

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtomContainer;

public interface IMolSubgraphFinder {
	
	public IAtomContainer maxCommonSubstructure(IAtomContainer query, IAtomContainer target);
	
	public ArrayList<IAtomContainer> getMaxCommonSubstructures(IAtomContainer query, ArrayList<IAtomContainer> targets);
}
