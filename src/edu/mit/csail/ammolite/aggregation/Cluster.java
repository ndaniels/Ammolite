package edu.mit.csail.ammolite.aggregation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.Logger;
import edu.mit.csail.ammolite.compression.MoleculeStruct;
import edu.mit.csail.fmcsj.MCS;


public class Cluster implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4558559462758246483L;
	private MoleculeStruct rep;
	private List<Cluster> members= new LinkedList<Cluster>();
	private double repBound;
	private int order = 0;
	
	public Cluster(MoleculeStruct initialMember, double _repBound){
		repBound = _repBound;
		rep = initialMember;
	}
	
	public Cluster(Cluster initialMember, double _repBound){
		repBound = _repBound;
		rep = initialMember.getRep();
		
		members.add(initialMember);
		
		order = initialMember.order() + 1;
	}
	
	public MoleculeStruct getRep(){
		return rep;
	}
	public int order(){
		return order;
	}
	
	public List<Cluster> getMembers(){
		return members;
	}
	
	public boolean addCandidate(Cluster candidate){
		if( this == candidate){
			return false;
		}
		if(candidate.order() > order()){
			return candidate.addCandidate(this);
		}
		
		MCS myMCS = new MCS(candidate.getRep(), rep, true);
		myMCS.calculate();
		
		int newRepSize = myMCS.size();
		
		
		
		if( (overlap( newRepSize, candidate.getRep().getAtomCount()) < repBound) ){
			return false;
		}
		

		for(int i=0; i<members.size(); ++i){
			
			Cluster member = members.get(i);
			if( overlap(newRepSize, member.getRep().getAtomCount()) < repBound ){
				return false;
			}
		}
		
		members.add(candidate);
		List<IAtomContainer> solutions = myMCS.getSolutions();
		
		rep = new MoleculeStruct( solutions.get(0));
		if( candidate.order() + 1 > order()){
			order = candidate.order() + 1;
		}
		
		return true;
	}
	
	public boolean matchesCluster(MoleculeStruct candidate, double myRepBound){
		
		MCS myMCS = new MCS(candidate, rep, true);
		myMCS.calculate();
		int newRepSize = myMCS.size();
		
		if( (overlap( newRepSize, candidate.getAtomCount()) < myRepBound) ){
			return false;
		}
		
		for(Cluster member: members){
			if( overlap(newRepSize, member.getRep().getAtomCount()) < myRepBound ){
				return false;
			}
		}
		

		return true;
	}
	
	private double overlap(int overlap, int original){
		return ( (double) overlap) / original;
	}
	
	public String toString(){
		return rep.getID();
	}
}
