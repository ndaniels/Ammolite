package edu.mit.csail.ammolite.database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

public class SDFSet implements Serializable{
	Map<String, SDFWrapper> idToWrapper = new HashMap<String, SDFWrapper>();
	List<String> filenames;
	
	public SDFSet(List<String> _filenames){
		filenames = _filenames;
		for(String f: filenames){
			SDFWrapper wrap = new SDFWrapper(f);
			for(String id: wrap.getIDs()){
				idToWrapper.put(id, wrap);
			}
		}
		
	}
	
	public List<IAtomContainer> getAllMolecules(){
		List<IAtomContainer> out = new ArrayList<IAtomContainer>();
		for(SDFWrapper wrapper: idToWrapper.values()){
			out.addAll(wrapper.getAllMolecules());
		}
		return out;
	}
	
	public List<String> getFiles(){
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
