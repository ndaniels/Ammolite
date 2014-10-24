package edu.mit.csail.ammolite.database;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.yaml.snakeyaml.Yaml;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.utils.Logger;
import edu.mit.csail.ammolite.utils.PubchemID;
import edu.mit.csail.ammolite.utils.StructID;

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
			} else if(extension.equals("gad") || extension.equals("gad/")){
			    return decompressGeneric(databasename);
			} else {
				throw new IllegalArgumentException("Cannot build a database from this filetype");
			}

	}
	
	public static IStructDatabase decompressGeneric(String genericName){
	    File dbDir = new File(genericName);
	    FilenameFilter metaFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith("metadata.yml");
            }
        };
	    File meta = dbDir.listFiles( metaFilter)[0];
	    try{
    	    BufferedReader  metaStream= Files.newBufferedReader(meta.toPath());
    	    Yaml yaml = new Yaml();
    	    Map md = (Map) yaml.load(metaStream);
    	    
    	    String name = (String) md.get("NAME");
    	    String version = (String) md.get("VERSION");
    	    String compression = (String) md.get("COMPRESSION_TYPE");
    	    boolean organized = (Boolean) md.get("ORGANIZED");

    	    String structIDTableName = (String) md.get("STRUCT_ID_TABLE");

    	    FilenameFilter idFilter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith("structids.yml");
                }
            };
            File idTable = dbDir.listFiles( idFilter)[0];
    	    BufferedReader  idStream= Files.newBufferedReader(idTable.toPath());
    	    
            Map<String,List<Integer>> rawIds = (Map<String,List<Integer>>) yaml.load(idStream);
            
            KeyListMap<StructID, PubchemID> ids = new KeyListMap<StructID, PubchemID>(10);
            for(String rawKey: rawIds.keySet()){
                StructID key = new StructID( rawKey);
                for(Integer rawVal: rawIds.get(rawKey)){
                    PubchemID val = new PubchemID(rawVal.toString());
                    ids.add(key, val);
                }
                
            }

    	    List<String> rawStructFiles = (List<String>) md.get("STRUCTURE_FILES");
    	    List<String> structFiles = new ArrayList<String>();
    	    
    	    for(String rStructFile: rawStructFiles){
	            String[] split = rStructFile.split(File.separator);
	            String wildcard = split[split.length - 1];
	            String location = rStructFile.substring(0, rStructFile.length() - wildcard.length());
	            if(location.startsWith("./")){
	                if(genericName.endsWith(File.separator)){
	                    location = genericName + location.substring(2);
	                } else {
	                    location = genericName + location.substring(1);
	                }
	            }

	            File dir = new File(location);
	            FileFilter wildFilter = new WildcardFileFilter(wildcard);
	            File[] allFiles = dir.listFiles(wildFilter);
	            for(File f: allFiles){
	                structFiles.add(f.getAbsolutePath());
	            }

    	    }
    	    
    	    List<String> rawSourceFiles = (List<String>) md.get("SOURCE_FILES");
            List<String> sourceFiles = new ArrayList<String>();
            
            for(String rSourceFile: rawSourceFiles){

                String[] split = rSourceFile.split(File.separator);
                String wildcard = split[split.length - 1];
                String location = rSourceFile.substring(0, rSourceFile.length() - wildcard.length());
                if(location.startsWith("./")){
                    if(genericName.endsWith(File.separator)){
                        location = genericName + location.substring(2);
                    } else {
                        location = genericName + location.substring(1);
                    }
                }
                
                File dir = new File(location);
                FileFilter wildFilter = new WildcardFileFilter(wildcard);
                File[] allFiles = dir.listFiles(wildFilter);
                for(File f: allFiles){
                    sourceFiles.add(f.getAbsolutePath());
                }
            }
            
    	    return new GenericStructDatabase( name, version, compression, 
    	                                        organized, ids, structFiles, 
    	                                        sourceFiles);
    	    
	    } catch (IOException ioe){
	        
	    }
	    return null;
	    
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
