package edu.mit.csail.ammolite.database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.PubchemID;
import edu.mit.csail.ammolite.utils.StructID;

public class SDFSet implements Serializable, ISDFSet {

	private static final long serialVersionUID = 7582074972607192820L;
	protected Map<PubchemID, SDFWrapper> idToWrapper = new HashMap<PubchemID, SDFWrapper>();
	protected List<String> filenames = new ArrayList<String>();
	protected Map<StructID,SDFWrapper> structIDToSDF = new HashMap<StructID,SDFWrapper>();
	
	public SDFSet(List<String> filenames){
		for(String f: filenames){
			addFile(f);
		}
		
	}
	
	public boolean isOrganized(){
		return false;
	}
	
	
	public void addFile(String filename){
		filenames.add(filename);
		SDFWrapper wrap = new SDFWrapper(filename);
		addFile(wrap);
	}
	
	public void addFile(SDFWrapper wrap){
		for(PubchemID id: wrap.getIDs()){
			idToWrapper.put(id, wrap);
		}
	}
	
	public List<IAtomContainer> getAllMolecules(){
		List<IAtomContainer> out = new ArrayList<IAtomContainer>();
		for(SDFWrapper wrapper: idToWrapper.values()){
			out.addAll(wrapper.getAllMolecules());
		}
		return out;
	}
	
	public List<String> getFilenames(){
		return filenames;
	}
	
	
	public Set<PubchemID> getMoleculeIDs(){
		return idToWrapper.keySet();
	}
	
	public IAtomContainer getMol(PubchemID pubID){
		if(idToWrapper.containsKey(pubID)){
			SDFWrapper wrap = idToWrapper.get(pubID);
			return wrap.getMol(pubID);
		}
		throw new NullPointerException("PubchemID "+pubID+" not in this SDFset");
	}


}
