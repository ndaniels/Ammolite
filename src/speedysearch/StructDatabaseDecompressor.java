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

public class StructDatabaseDecompressor {
	
	public StructDatabaseDecompressor(){

	}
	
	public StructDatabase decompress(String databasename) throws ClassNotFoundException, IOException{
		Object database = deserialize( new File(databasename));
		if( !( database instanceof StructDatabase)){
			throw new IOException();
		}
		return (StructDatabase) database;
	}
	
	private Object deserialize(File f) throws ClassNotFoundException, IOException{
        InputStream file = new FileInputStream(f);
        InputStream buffer = new BufferedInputStream(file);
        ObjectInput obInput = new ObjectInputStream (buffer);

        Object recovered = obInput.readObject();
        obInput.close();
        return recovered;
	}
}
