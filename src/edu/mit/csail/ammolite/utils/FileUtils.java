package edu.mit.csail.ammolite.utils;

import java.io.File;
import java.io.FileFilter;

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

}