package edu.mit.csail.ammolite.search;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;


public class MolTriple {
	private List<IAtomContainer> overlaps;
	private IAtomContainer query;
	private IAtomContainer match;
	
	public MolTriple(List<IAtomContainer> _overlaps, IAtomContainer _query, IAtomContainer _match){
		overlaps = _overlaps;
		query = _query;
		match = _match;
	}
	
	public List<IAtomContainer> getOverlap(){
		return overlaps;
	}
	
	public IAtomContainer getQuery(){
		return query;
	}
	
	public IAtomContainer getMatch(){
		return match;
	}
	
	public String sizes(){
		return query.getAtomCount()+" "+match.getAtomCount()+" "+overlaps.get(0).getAtomCount();
	}
}
