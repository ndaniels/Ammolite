package edu.mit.csail.ammolite.database;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;

import edu.mit.csail.ammolite.Logger;

public class StructDatabaseDecompressor {
	
	
	public static StructDatabase decompress(String databasename){
			Object database;
			try {
				database = deserialize( new File(databasename));
				if( !( database instanceof StructDatabase)){
					throw new IOException();
				}
				return new StructDatabase( (StructDatabaseCoreData) database);
			} catch (ClassNotFoundException e) {
				Logger.error("Failed to open database "+databasename);
				e.printStackTrace();
			} catch (IOException e) {
				Logger.error("Failed to open database "+databasename);
				e.printStackTrace();
			}
			System.exit(1);
			return null;

	}
	
	private static Object deserialize(File f) throws ClassNotFoundException, IOException{
        InputStream file = new FileInputStream(f);
        InputStream buffer = new BufferedInputStream(file);
        ObjectInput obInput = new ObjectInputStream (buffer);

        Object recovered = obInput.readObject();
        obInput.close();
        return recovered;
	}
}
