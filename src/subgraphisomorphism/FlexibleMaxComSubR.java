package subgraphisomorphism;

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.rosuda.JRI.Rengine;

public class FlexibleMaxComSubR implements IMolSubgraphFinder {

	@Override
	public IAtomContainer maxCommonSubstructure(IAtomContainer query, IAtomContainer target) {
		Rengine r = new Rengine();
		r.
		
		return null;
	}

	@Override
	public ArrayList<IAtomContainer> getMaxCommonSubstructures(IAtomContainer query, ArrayList<IAtomContainer> targets) {
		ArrayList<IAtomContainer> out = new ArrayList<IAtomContainer>(targets.size());
		for(IAtomContainer target: targets){
			out.add( maxCommonSubstructure(query, target) );
		}
		return out;
	}

}
