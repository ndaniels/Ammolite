package edu.mit.csail.ammolite.database;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

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
}
