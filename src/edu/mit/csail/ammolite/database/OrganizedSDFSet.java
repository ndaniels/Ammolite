package edu.mit.csail.ammolite.database;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.SDFUtils;


public class OrganizedSDFSet extends SDFSet implements Serializable {
	

	public OrganizedSDFSet(List<String> _filenames){
		super(_filenames);
	}
	
	@Override
	public boolean isOrganized(){
		return true;
	}
	
	public void addFile(SDFWrapper sdf){
		super.addFile(sdf);
		String structID = filenameToStructID( sdf.getFilename());
		mapStructToFile(structID, sdf);
	}

	private String filenameToStructID(String filename){
		if( filename.endsWith("_STRUCT.sdf")){
			int idLen = filename.length() - 4;
			return filename.substring(0, idLen);
		}
		throw new IllegalArgumentException("Provided filename cannot be resolved to a structID");
	}
	
	private void mapStructToFile(String structID, SDFWrapper sdf){
		if( structID == null){
			throw new NullPointerException("Null structID.");
		} else if( sdf == null){
			throw new NullPointerException("Null sdf wrapper.");
		} else if( structIDToSDF == null){
			throw new NullPointerException("Null id to sdf map.");
		}
		structIDToSDF.put(structID, sdf);
	}
	
	public List<IAtomContainer> getMatchingMols(String structID){
		if( structIDToSDF.containsKey(structID)){
			return structIDToSDF.get(structID).getAllMolecules();
		} else {
			throw new NullPointerException("No such struct ID in database");
		}
	}


	

}
