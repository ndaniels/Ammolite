package speedysearch;

import java.io.FileReader;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.interfaces.IAtomContainer;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class StructDatabaseBuilder{
	
	public static void buildStructDatabase( String filename ) throws CDKException, IOException{
		IteratingSDFReader molecule_database =new IteratingSDFReader(
			        													new FileReader( filename ), 
			        													DefaultChemObjectBuilder.getInstance()
			        												);
		String out_filename = "Struct_" + filename;
        SDFWriter writer = new SDFWriter( new BufferedWriter( new FileWriter( out_filename )) );
        
        while( molecule_database.hasNext() ){
        	IAtomContainer molecule = molecule_database.next();
        	MoleculeStruct structure = new MoleculeStruct( molecule );
        	writer.write( structure );
        }
        
        molecule_database.close();
        
        if (writer != null) {
            writer.close();
        }
	}
}

