package edu.mit.csail.ammolite.spark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;
import edu.mit.csail.ammolite.search.Ammolite;
import edu.mit.csail.ammolite.search.IResultHandler;
import edu.mit.csail.ammolite.search.SDFWritingResultHandler;
import edu.mit.csail.ammolite.search.SMSDSearcher;
import edu.mit.csail.ammolite.search.SearchMatch;
import edu.mit.csail.ammolite.search.SimpleResultHandler;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.SDFUtils;

public class SparkSearchHandler {
    
    private static final int CHUNK_SIZE = 100;
    
    public static void handleDistributedSearch(List<String> queryFiles, List<String> databaseNames, String outName, 
                                                double fineThresh, boolean writeSDF, boolean linearSearch){
        
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
        
        SparkConf sparkConf = new SparkConf().setAppName("AmmoliteDistributedSearch");
        JavaSparkContext ctx = new JavaSparkContext(sparkConf);
        
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
            
   
            
            while( queries.hasNext()){
                query = queries.next();
                
                Iterator<IAtomContainer> targetIterator = SDFUtils.parseSDFSetOnline(sdfFiles);
                List<SearchMatch> matches = SMSDSpark.distributedChunkyLinearSearch(query, targetIterator, ctx, resultHandler, fineThresh, CHUNK_SIZE);
                for(SearchMatch match: matches){
                    resultHandler.handleFine(match);
                }

                resultHandler.finishOneQuery();
            }
            
            
        } else {
            if(databaseNames.size() != 1){
                System.err.println("Ammolite-Error: Exactly one database must be specified for compressive search.");
                System.exit(1);
            } else {
                IStructDatabase db = StructDatabaseDecompressor.decompress(databaseNames.get(0));
                double coarseThresh = fineThresh - 0.1;
                
                while( queries.hasNext()){
                    query = queries.next();
                    for(SearchMatch match: AmmoliteSpark.distributedAmmoliteSearch(query, db, ctx, resultHandler, fineThresh, coarseThresh, CHUNK_SIZE)){
                        resultHandler.handleFine(match);
                    } 
                    resultHandler.finishOneQuery();
                }
                
            }
        }
        
        ctx.close();
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
