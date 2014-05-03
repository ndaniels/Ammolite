package edu.mit.csail.ammolite.aggregation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

import edu.mit.csail.ammolite.utils.Logger;

public class ClusterDatabaseDecompressor {

	public static ClusterDatabase decompress(String clusterDBName){
		try {
			return new ClusterDatabase( (ClusterDatabaseCoreData) deserialize( new File( clusterDBName)));
		} catch (ClassNotFoundException e) {
			Logger.error("Not a valid cluster database. Aborting");
			e.printStackTrace();
		} catch (IOException e) {
			Logger.error("Failed to open cluster database. Aborting");
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
