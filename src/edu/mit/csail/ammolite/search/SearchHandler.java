package edu.mit.csail.ammolite.search;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;
import edu.mit.csail.ammolite.utils.SDFUtils;

public class SearchHandler {
    
    public static void handleSearch(List<String> queryFiles, List<String> databaseNames, String outName, double fineThresh, boolean writeSDF, boolean linearSearch){
        IResultHandler resultHandler = null;
        
        if( writeSDF){
            if( outName.equals("-")){
                resultHandler = new SDFWritingResultHandler("search-results");
            } else {
                resultHandler = new SDFWritingResultHandler(outName);
            }
        } else { // Simple
            if( outName.equals("-")){
                resultHandler = new SimpleResultHandler( System.out);
            } else {
                
                resultHandler = new SimpleResultHandler( getPrintStream(outName +".csv"));
            } 
        }
        
        Iterator<IAtomContainer> queries = SDFUtils.parseSDFSetOnline(queryFiles);
        IAtomContainer query = null;
        if( linearSearch){
            List<String> sdfFiles = new ArrayList<String>();
            for(String dbName: databaseNames){
                String ext;
                if(dbName.charAt(dbName.length() -1) == File.separatorChar){
                    ext = FilenameUtils.getExtension(dbName.substring(0, dbName.length() -1));
                } else {
                    ext = FilenameUtils.getExtension(dbName);
                }

                if(ext.equals("adb") || ext.equals("gad")){
                    IStructDatabase db = StructDatabaseDecompressor.decompress(dbName);
                    sdfFiles.addAll( db.getSourceFiles().getFilepaths());
                } else {
                    sdfFiles.add(dbName);
                }
            }
            
            SMSDSearcher searcher = new SMSDSearcher();
            while( queries.hasNext()){
                query = queries.next();
                searcher.search(query, sdfFiles, fineThresh, resultHandler);  
                resultHandler.finishOneQuery();
            }
            
            
        } else {
            if(databaseNames.size() != 1){
                System.err.println("Ammolite-Error: Exactly one database must be specified for compressive search.");
                System.exit(1);
            } else {
                IStructDatabase db = StructDatabaseDecompressor.decompress(databaseNames.get(0));
                Ammolite searcher = new Ammolite();
                
                while( queries.hasNext()){
                    query = queries.next();
                    searcher.search(query, db, fineThresh, fineThresh - 0.1, resultHandler);  
                    resultHandler.finishOneQuery();
                }
                
            }
        }

    }
    
    private static PrintStream getPrintStream(String outName){
        PrintStream writer= null;
        try {
            File f = new File(outName);
            FileOutputStream fos = new FileOutputStream(f, true);
            writer = new PrintStream(fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(writer == null){
            System.exit(1);
        }
        return writer;
        
    }

}
