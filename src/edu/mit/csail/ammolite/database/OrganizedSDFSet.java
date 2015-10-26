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


public class OrganizedSDFSet implements ISDFSet, Serializable {
	
	private static final long serialVersionUID = 1391777337735911123L;
	protected List<SDFWrapper> sdfs = new ArrayList<SDFWrapper>();
    protected Map<StructID,SDFWrapper> structIDToSDF = new HashMap<StructID,SDFWrapper>();
    
	public OrganizedSDFSet(List<String> filenames){
	    for(String f: filenames){
            addFile(f);
        }
	}
	
	   public OrganizedSDFSet(String foldername){
	       File f = new File(foldername);
	        if( f.isDirectory()){
	            if( f.isFile()){
	                addFile(f.getAbsolutePath());
	            } else {
	                for(File subF: f.listFiles()){
	                    addFile(f.getAbsolutePath());
	                }
	            }
	        }
	    }
	
	public boolean isOrganized(){
		return true;
	}

	
	public void addFile(String filename){
        SDFWrapper wrap = new SDFWrapper(filename, false);
        addFile(wrap);
    }

	
	public void addFile(SDFWrapper sdf){
	    sdfs.add(sdf);
		StructID structID = filenameToStructID( sdf.getFilename());
		mapStructToFile(structID, sdf);
	}

	private StructID filenameToStructID(String filename){
		if( filename.endsWith("_STRUCT.sdf")){
			int idLen = filename.length() - 4;
			return new StructID( filename.substring(0, idLen));
		}
		throw new IllegalArgumentException("Provided filename cannot be resolved to a structID");
	}
	
	private void mapStructToFile(StructID structID, SDFWrapper sdf){
		
		structIDToSDF.put(structID, sdf);
	}
	
	public List<IAtomContainer> getMatchingMols(StructID structID){
		if( structIDToSDF.containsKey(structID)){
			return structIDToSDF.get(structID).getAllMolecules();
		} else {
			throw new NullPointerException("No such struct ID in database");
		}
	}

    @Override
    public List<IAtomContainer> getAllMolecules() {
        List<IAtomContainer> out = new ArrayList<IAtomContainer>();
        for(SDFWrapper wrapper: sdfs){
            out.addAll(wrapper.getAllMolecules());
        }
        return out;
    }

    @Override
    public List<String> getFilenames() {
        List<String> out = new ArrayList<String>();
        for(SDFWrapper sdf: sdfs){
            out.add(sdf.getFilename());
        }
        return out;
    }

    @Override
    public List<String> getFilepaths() {
        List<String> out = new ArrayList<String>();
        for(SDFWrapper sdf: sdfs){
            out.add(sdf.getFilepath());
        }
        return out;
    }

    @Override
    public Set<AmmoliteID> getMoleculeIDs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IAtomContainer getMol(AmmoliteID pubID) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String listSourceFiles() {
        StringBuilder sb = new StringBuilder();
        for(SDFWrapper sdf: sdfs){
            sb.append(sdf.getFilename());
            sb.append(" molecules: ");
            sb.append(String.format("%,d", sdf.numMols()));
            sb.append("\n");
        }
        return sb.toString();
    }


	

}
