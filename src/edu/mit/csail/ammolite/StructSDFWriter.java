package edu.mit.csail.ammolite;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.io.SDFWriter;

import edu.mit.csail.ammolite.compression.MoleculeStruct;

public class StructSDFWriter{
	private BufferedWriter meta_writer;
	private SDFWriter sdfwriter;

	public StructSDFWriter(String filename) throws IOException{
		sdfwriter = new SDFWriter(new BufferedWriter( new FileWriter( filename + ".st.sdf" )));
		meta_writer = new BufferedWriter( new FileWriter( filename + ".st.meta"));	
	}

	

	public void write(IChemObject object) throws CDKException{
		if(object instanceof MoleculeStruct){
			this.writeStructure((MoleculeStruct) object);
		}
		sdfwriter.write(object);
		
	}
	
	private void writeStructure(MoleculeStruct struct) throws CDKException{
		try{
			meta_writer.newLine();

			for(String id: struct.getIDNums()){
				if( id != null){
					meta_writer.write( id );
					meta_writer.write(" ");
				}
			}
		} catch (IOException exception){
			throw new CDKException("Error while writing structural metadata: " + exception.getMessage(), exception );
		}
		
	}
	
	public void close() throws IOException{
		sdfwriter.close();
		meta_writer.newLine();
		meta_writer.close();
	}


}
