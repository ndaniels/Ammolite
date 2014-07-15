package edu.mit.csail.ammolite.aggregation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.mcs.AbstractMCS;
import edu.mit.csail.ammolite.mcs.FMCS;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.mcs.MCSFinder;
import edu.mit.csail.ammolite.utils.Logger;
import edu.mit.csail.ammolite.utils.MCSUtils;


public class Cluster implements Serializable{


	private MolStruct rep;
	private List<Cluster> members= new LinkedList<Cluster>();
	private double repBound;
	private int order = 0;
	
	public Cluster(MolStruct initialMember, double _repBound){
		repBound = _repBound;
		rep = initialMember;
	}
	
	public Cluster(Cluster initialMember, double _repBound){
		repBound = _repBound;
		rep = initialMember.getRep();
		
		members.add(initialMember);
		
		order = initialMember.order() + 1;
	}
	
	/**
	 * Return the representative molecule-structure for the cluster
	 * @return
	 */
	public MolStruct getRep(){
		return rep;
	}
	
	/**
	 * Returns the maximum distance to the base of the tree.
	 * 
	 * Leaves have an order of zero.
	 * 
	 * @return
	 */
	public int order(){
		return order;
	}
	
	
	/**
	 * Return the sub-clusters which are part of this cluster
	 * @return
	 */
	public List<Cluster> getMembers(){
		return members;
	}
	
	/**
	 * Attempt to add a candidate cluster to this cluster. Return true if successful.
	 * @param candidate
	 * @return
	 */
	public boolean addCandidate(Cluster candidate){
		if( this == candidate){
			return false;
		}
		if(candidate.order() > order()){
			return candidate.addCandidate(this);
		}		
		if( !MCS.beatsOverlapThresholdIsoRank(candidate.getRep(), rep, 0.9 * repBound)){
			return false;
		}
		
		IAtomContainer mcs = MCS.getMCS(candidate.getRep(), rep);
		int mcsSize = mcs.getAtomCount();
		double newOverlapCoeff = MCSUtils.overlapCoeff(mcsSize, rep, candidate.getRep());
		
		if( newOverlapCoeff < repBound){
			return false;
		}
		

		for(int i=0; i<members.size(); ++i){
			Cluster member = members.get(i);
			double memberOverlap = (1.0*mcsSize) /  member.getRep().getAtomCount();
			if( memberOverlap < repBound ){
				return false;
			}
		}
		
		
		rep = new MolStruct( mcs);
		if( candidate.order() + 1 > order()){
			order = candidate.order() + 1;
		}
		
		return true;
	}
	
	public boolean matchesCluster(MolStruct candidate, double myRepBound){
		
		if( !MCS.beatsOverlapThresholdIsoRank(candidate, rep, 0.9 * repBound)){
			return false;
		}
		
		IAtomContainer mcs = MCS.getMCS(candidate, rep);
		int mcsSize = mcs.getAtomCount();
		double newOverlapCoeff = MCSUtils.overlapCoeff(mcsSize, rep, candidate);
		
		if( newOverlapCoeff < myRepBound){
			return false;
		}
		
		for(int i=0; i<members.size(); ++i){
			Cluster member = members.get(i);
			double memberOverlap = (1.0*mcsSize) /  member.getRep().getAtomCount();
			if( memberOverlap < myRepBound ){
				return false;
			}
		}
		
		return true;
	}
	

	public String toString(){
		return rep.getID();
	}
}
