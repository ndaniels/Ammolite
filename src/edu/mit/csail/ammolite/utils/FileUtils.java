package edu.mit.csail.ammolite.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

public class FileUtils {
	
	/**
	 * Turns a folder or file name into a list of file names (or just one name)
	 * @param folder_name
	 * @return
	 */
	public static File[] getContents(String folder_name ){
		File directory = new File( folder_name );
		File[] contents = {directory};
		if( directory.isDirectory()){
			contents = directory.listFiles();
		}
		return contents;
	}
	
	public static File[] expandWildcard(String wildcard){
		 File dir = new File(".");
		 FileFilter fileFilter = new WildcardFileFilter(wildcard);
		 File[] files = dir.listFiles(fileFilter);
		 return files;
	}
	
	public static List<File> openFiles(List<String> filenames){
		List<File> out = new ArrayList<File>();
		for(String name: filenames){
			File f = new File(name);
			out.add(f);
		}
		return out;
	}
	
   public static void writeObjectToFile(String path, Object o) throws IOException{
       OutputStream file = new FileOutputStream( path );
       OutputStream buffer = new BufferedOutputStream( file );
       ObjectOutput output = new ObjectOutputStream( buffer );
       output.writeObject(o);
       output.close();
   }
   
   public static void writeObjectToFile(String baseDirectory, String filename, Object o) throws IOException{
       String path = FilenameUtils.concat(baseDirectory, filename);
       OutputStream file = new FileOutputStream( path );
       OutputStream buffer = new BufferedOutputStream( file );
       ObjectOutput output = new ObjectOutputStream( buffer );
       output.writeObject(o);
       output.close();
   }

}
