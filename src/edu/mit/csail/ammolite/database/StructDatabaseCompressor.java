package edu.mit.csail.ammolite.database;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.PubchemID;


public class StructDatabaseCompressor {
	
    public static void compress(String filename, IDatabaseCoreData db){

        try {
            writeObjectToFile(filename+ ".adb", db);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
	
	private static void writeObjectToFile(String filepath, Object o) throws IOException{
			OutputStream file = new FileOutputStream( filepath );
			OutputStream buffer = new BufferedOutputStream( file );
			ObjectOutput output = new ObjectOutputStream( buffer );
			output.writeObject(o);
			output.close();
		
	}
	
	public static void compressGeneric(String filename, IDatabaseCoreData dbCD){
	    String exFilename = filename+ ".agd";
	    File dir = new File(exFilename);
	    dir.mkdir();
	    IStructDatabase db = new StructDatabase(dbCD);
	    String structTable = writeStructIDFile(exFilename, db.iterator());
	    List<String> structFiles = writeStructSDF( exFilename, db.iterator());
	    writeMetadataFile( exFilename, db.getName(), "AMMOLITE_GENERIC_DATABASE_0_0_0", 
	                        structTable, db.getCompressionType(), false, structFiles, db.getSourceFiles().getFilepaths());
	    
	}
	
	private static String writeStructIDFile(String folder, Iterator<MolStruct> structs){
	    String structName = "structids.af";
	    String structPath = folder + File.separator + structName;
	    BufferedWriter writer;
	    try{
	        FileWriter fw = new FileWriter(structPath);
	        writer = new BufferedWriter(fw);
    	    while( structs.hasNext()){
    	        MolStruct struct = structs.next();
    	        writer.write( MolUtils.getStructID(struct).toString());
    	        writer.write(" : ");
    	        for(PubchemID pId: struct.getIDNums() ){
    	            writer.write( pId.toString());
    	            writer.write(" ");
    	        }
    	        writer.newLine();
    	    }
    	    writer.close();
	    } catch(IOException ioe){
    	    ioe.printStackTrace();
    	}
	    return structName;
	}
	
	private static List<String> writeStructSDF(String folder, Iterator<MolStruct> structs){
	    int CHUNK_SIZE = 25*1000;
	    int fileNum = -1;
	    int structsInFile = 0;
	    String structBaseName = "struct_%d.af";
	    OutputStream stream = null;
	    SDFWriter writer = null;
	    List<String> allFiles = new ArrayList<String>();
	    
	    while( structs.hasNext()){
	        if(structsInFile == CHUNK_SIZE || fileNum == -1){
	            fileNum++;
	            structsInFile = 0;
	            try{
	                try{
    	                writer.close();
    	                stream.close();
	                } catch( NullPointerException npe) {}
	                
	                String structName = String.format(structBaseName, fileNum);
	                String structPath = folder + File.separator + structName;
	                allFiles.add(structName);
	                stream = new PrintStream(structPath);
	                writer = new SDFWriter( stream);
	            } catch( IOException ioe){
	                ioe.printStackTrace();
	            } 
	            
	        }
	        
	        MolStruct struct = structs.next();
	        struct.setProperty("AMMOLITE_STRUCTURE_ID", MolUtils.getUnknownOrID(struct));
            try {
                writer.write(struct);
                structsInFile++;
            } catch (CDKException e) {
                e.printStackTrace();
            }
	    }
	    try {
            writer.close();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	    return allFiles;
	}
	
	private static void writeMetadataFile(String folder, String name, String version, String structTable, CompressionType compression, 
	                                    boolean organized, List<String> structFiles, List<String> sourceFiles){
	    String metaName = "metadata.af";
        String metaPath = folder+ File.separator + metaName;
        BufferedWriter writer;
        try{
            FileWriter fw = new FileWriter(metaPath);
            writer = new BufferedWriter(fw);
            
            writer.write("NAME: ");
            writer.write(name);
            writer.newLine();
            
            writer.write("VERSION: ");
            writer.write(version);
            writer.newLine();
            
            writer.write("COMPRESSION_TYPE: ");
            if( compression == CompressionType.CYCLIC){
                writer.write("CYCLIC");
            } else if (compression == CompressionType.BASIC){
                writer.write("BASIC");
            } else {
                writer.write("OTHER");
            }
            writer.newLine();
            
            writer.write("ORGANIZED: ");
            if( organized){
                writer.write("TRUE");
            } else {
                writer.write("FALSE");
            }
            writer.newLine();
            
            writer.write("STRUCT_ID_TABLE: ");
            writer.write(structTable);
            writer.newLine();
            
            writer.write("STRUCTURE_FILES: ");
            writer.newLine();
            for(String sFilename: structFiles){
                writer.write(sFilename);
                writer.newLine();
            }
            writer.newLine();
            
            writer.write("SOURCE_FILES: ");
            writer.newLine();
            for(String filename: sourceFiles){
                writer.write(filename);
                writer.newLine();
            }
            writer.newLine();
            
            writer.close();
        } catch(IOException ioe){
            ioe.printStackTrace();
        }
	}
	
	
	
}
