package edu.mit.csail.ammolite.database;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.AmmoliteID;
import edu.mit.csail.ammolite.utils.PubchemID;
import edu.mit.csail.ammolite.utils.StructID;

public class SDFSet implements Serializable, ISDFSet {

	private static final long serialVersionUID = 7582074972607192820L;
	protected Map<AmmoliteID, SDFWrapper> idToWrapper = new HashMap<AmmoliteID, SDFWrapper>();
	protected List<SDFWrapper> sdfs = new ArrayList<SDFWrapper>();
	protected Map<StructID,SDFWrapper> structIDToSDF = new HashMap<StructID,SDFWrapper>();
	
	public SDFSet(List<String> filenames){
		for(String f: filenames){
			addFile(f);
		}
		
	}
	
	public SDFSet(String foldername){
	    File f = new File(foldername);
	    if( f.isDirectory()){
	        if( f.isFile()){
    	        for(File subF: f.listFiles()){
    	            addFile(f.getAbsolutePath());
    	        }
	        }
	    }
	}
	
	public boolean isOrganized(){
		return false;
	}
	
	
	public void addFile(String filename){
		SDFWrapper wrap = new SDFWrapper(filename);
		addFile(wrap);
	}
	
	public void addFile(SDFWrapper wrap){
		sdfs.add(wrap);
		for(AmmoliteID id: wrap.getIDs()){
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
		List<String> out = new ArrayList<String>();
		for(SDFWrapper sdf: sdfs){
			out.add(sdf.getFilename());
		}
		return out;
	}
	
	public List<String> getFilepaths(){
		List<String> out = new ArrayList<String>();
		for(SDFWrapper sdf: sdfs){
			out.add(sdf.getFilepath());
		}
		return out;
	}
	
	
	public Set<AmmoliteID> getMoleculeIDs(){
		return idToWrapper.keySet();
	}
	
	public IAtomContainer getMol(AmmoliteID pubID){
		if(idToWrapper.containsKey(pubID)){
			SDFWrapper wrap = idToWrapper.get(pubID);
			return wrap.getMol(pubID);
		}
		throw new NullPointerException("AmmoliteID "+pubID+" not in this SDFset");
	}
	
	public String listSourceFiles(){
		StringBuilder sb = new StringBuilder();
		for(SDFWrapper sdf: sdfs){
			sb.append(sdf.getFilename());
			sb.append(" molecules: ");
			sb.append(String.format("%,d", sdf.numMols()));
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public boolean checkAllFiles(){
	    for(SDFWrapper wrap: sdfs){
	        if(!wrap.check()){
	            return false;
	        }
	    }
	    return true;
	}

}
