package speedysearch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;

public class StructDatabaseFactory {
	
	public StructDatabaseFactory(){

	}
	
	public StructDatabase decompress(String foldername) throws ClassNotFoundException, IOException{
		File folder = new File(foldername);
		File[] contents = folder.listFiles();
		String filename;
		KeyListMap<Integer, MoleculeStruct> structsByHash = null;
		HashMap<String, FilePair> moleculeLocationsByID = null;
		for(File f: contents){
			filename  = f.getName();
			if( filename.equals("structsByHash.ser")){
				structsByHash = (KeyListMap<Integer, MoleculeStruct>) deserialize(f);	
				
			} else if( filename.equals("moleculeLocationsByID.ser")){
				moleculeLocationsByID = (HashMap<String, FilePair>) deserialize(f);
				
			}
		}
		
		return new StructDatabase(structsByHash, moleculeLocationsByID);
	}
	
	private Object deserialize(File f) throws ClassNotFoundException, IOException{
        InputStream file = new FileInputStream(f);
        InputStream buffer = new BufferedInputStream(file);
        ObjectInput obInput = new ObjectInputStream (buffer);

        Object recovered = obInput.readObject();
        return recovered;
	}
}
