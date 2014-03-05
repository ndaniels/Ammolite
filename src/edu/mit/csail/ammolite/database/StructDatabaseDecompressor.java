package edu.mit.csail.ammolite.database;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

import edu.mit.csail.ammolite.Logger;

public class StructDatabaseDecompressor {
	
	
	public static IStructDatabase decompress(String databasename){			
			String extension = "";
			int i = databasename.lastIndexOf('.');
			if (i > 0) {
			    extension = databasename.substring(i+1);
			}
			
			if(extension.equals("adb")){
				return ammoliteDatabase( databasename);
			} else if( extension.equals("sdf")) {
				return new MoleculeDatabase( databasename);
			} else {
				throw new IllegalArgumentException("Cannot build a database from this filetype");
			}

	}
	
	private static StructDatabase ammoliteDatabase(String databasename){
		Object database;
		try {
			database = deserialize( new File(databasename));
			if( !( database instanceof StructDatabaseCoreData)){
				throw new IOException();
			}
			StructDatabaseCoreData structDB = (StructDatabaseCoreData) database;
			return new StructDatabase( structDB);
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
