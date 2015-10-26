package edu.mit.csail.ammolite.database;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.yaml.snakeyaml.Yaml;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.utils.AmmoliteID;
import edu.mit.csail.ammolite.utils.Logger;
import edu.mit.csail.ammolite.utils.PubchemID;
import edu.mit.csail.ammolite.utils.StructID;

public class StructDatabaseDecompressor {
    
    private static final String[] DB_EXTENSIONS = new String[] {"gad", "gad/", "adb", "adb/"};
	
	public static IStructDatabase decompress(String databasename){
		return decompress(databasename, false);
	}
	
	
	
	
	public static IStructDatabase decompress(String databasename, boolean useCaching){
			System.out.print("Decompressing "+databasename);
			String extension = "";
			int i = databasename.lastIndexOf('.');
			if (i > 0) {
			    extension = databasename.substring(i+1);
			}
		
			if(Arrays.asList(DB_EXTENSIONS).contains(extension)){
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
	        
    	    BufferedReader  metaStream = new BufferedReader(new FileReader(meta));
    	    Yaml yaml = new Yaml();
    	    Map md = (Map) yaml.load(metaStream);
    	    
    	    System.out.print("Grabbing metadata...");
    	    String name = (String) md.get("NAME");
    	    String version = (String) md.get("VERSION");
    	    String compression = (String) md.get("COMPRESSION_TYPE");
    	    boolean organized = (Boolean) md.get("ORGANIZED");
    	    System.out.println("Done.");
    	    String structIDTableName = (String) md.get("STRUCT_ID_TABLE");

    	    FilenameFilter idFilter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith("structids.yml");
                }
            };
            File idTable = dbDir.listFiles( idFilter)[0];
    	    BufferedReader  idStream = new BufferedReader(new FileReader(idTable));
    	    
    	    System.out.print("Building map...");
            Map<String,List<Integer>> rawIds = (Map<String,List<Integer>>) yaml.load(idStream);
            
            KeyListMap<StructID, AmmoliteID> ids = new KeyListMap<StructID, AmmoliteID>(10);
            for(String rawKey: rawIds.keySet()){
                StructID key = new StructID( rawKey);
                for(Integer rawVal: rawIds.get(rawKey)){
                    AmmoliteID val = new AmmoliteID(rawVal.toString());
                    ids.add(key, val);
                }
                
            }
            System.out.println("Done.");

            System.out.print("Finding representatives...");
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
	            Collection<File> allFiles = FileUtils.listFiles(dir, (IOFileFilter) wildFilter, TrueFileFilter.INSTANCE);
	            
	            for(File f: allFiles){
	                structFiles.add(f.getAbsolutePath());
	            }

    	    }
    	    System.out.println("Done.");
    	    
    	    System.out.print("Finding sources...");
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
                Collection<File> allFiles = FileUtils.listFiles(dir, (IOFileFilter) wildFilter, TrueFileFilter.INSTANCE);
                
                for(File f: allFiles){
                    sourceFiles.add(f.getAbsolutePath());
                }
            }
            System.out.println("Done.");
            
            System.out.print("Building database...");
    	    GenericStructDatabase db =  new GenericStructDatabase( name, version, compression, 
    	                                        organized, ids, structFiles, 
    	                                        sourceFiles);
    	    System.out.println("Done.");
    	    
    	    if( md.containsKey("NUM_MOLS")){
    	        
    	        Object rawNumMols = md.get("NUM_MOLS");
    	        
    	        int numMols = -1;
    	        if( rawNumMols instanceof Integer){
    	            
    	            numMols = (Integer) rawNumMols;
    	        } else if( rawNumMols instanceof String){
    	            
    	            numMols = Integer.parseInt((String) rawNumMols );
    	        }
    	        db.setNumMols(numMols);
    	    }
    	    
    	    if( md.containsKey("NUM_REPS")){
    	        
    	        Object rawNumReps = md.get("NUM_REPS");
    	        
                int numReps = -1;
                if( rawNumReps instanceof Integer){
                    
                    numReps = (Integer) rawNumReps;
                } else if( rawNumReps instanceof String){
                    
                    numReps = Integer.parseInt((String) rawNumReps);
                }
                db.setNumReps(numReps);
            }
    	    
    	    return db; 
    	    
	    } catch (IOException ioe){
	        
	    }
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
