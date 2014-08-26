package edu.mit.csail.ammolite.database;

import java.io.Serializable;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.utils.StructID;


public class OrganizedSDFSet extends SDFSet implements Serializable {
	
	private static final long serialVersionUID = 1391777337735911123L;

	public OrganizedSDFSet(List<String> _filenames){
		super(_filenames);
	}
	
	@Override
	public boolean isOrganized(){
		return true;
	}
	
	public void addFile(SDFWrapper sdf){
		super.addFile(sdf);
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


	

}
