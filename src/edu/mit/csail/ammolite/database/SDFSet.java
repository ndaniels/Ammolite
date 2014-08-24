package edu.mit.csail.ammolite.database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

public class SDFSet implements Serializable, ISDFSet {

	private static final long serialVersionUID = 7582074972607192820L;
	protected Map<String, SDFWrapper> idToWrapper = new HashMap<String, SDFWrapper>();
	protected List<String> filenames = new ArrayList<String>();
	protected Map<String,SDFWrapper> structIDToSDF = new HashMap<String,SDFWrapper>();
	
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
		for(String id: wrap.getIDs()){
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
	
	
	public Set<String> getMoleculeIDs(){
		return idToWrapper.keySet();
	}
	
	public IAtomContainer getMol(String pubID){
		SDFWrapper wrap = idToWrapper.get(pubID);
		return wrap.getMol(pubID);
	}


}
