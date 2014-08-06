package edu.mit.csail.ammolite.database;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

import edu.mit.csail.ammolite.utils.Logger;

public class StructDatabaseDecompressor {
	
	public static IStructDatabase decompress(String databasename){
		return decompress(databasename, false);
	}
	
	
	public static IStructDatabase decompress(String databasename, boolean useCaching){
			Logger.log("Decompressing "+databasename, 1);
			String extension = "";
			int i = databasename.lastIndexOf('.');
			if (i > 0) {
			    extension = databasename.substring(i+1);
			}
			
			if(extension.equals("adb")){
				if( useCaching){	
					return new CachingStructDatabase( ammoliteCoreDatabase( databasename));
				
				
				} else {
					return new StructDatabase( ammoliteCoreDatabase( databasename));
				}
			} else {
				throw new IllegalArgumentException("Cannot build a database from this filetype");
			}

	}
	
	public static IDatabaseCoreData decompressToCoreData(String databasename){
		Logger.log("Decompressing "+databasename, 1);
		String extension = "";
		int i = databasename.lastIndexOf('.');
		if (i > 0) {
		    extension = databasename.substring(i+1);
		}
		
		if(extension.equals("adb")){
			return ammoliteCoreDatabase( databasename);
		} else {
			throw new IllegalArgumentException("Cannot build a database from this filetype");
		}
	}
	
	private static IDatabaseCoreData ammoliteCoreDatabase(String databasename){
		Object database;
		try {
			database = deserialize( new File(databasename));
			if( !( database instanceof StructDatabaseCoreData)){
				throw new IOException();
			}
			StructDatabaseCoreData structDB = (StructDatabaseCoreData) database;
			return structDB;
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
