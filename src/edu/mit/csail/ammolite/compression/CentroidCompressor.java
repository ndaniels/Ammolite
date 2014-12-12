package edu.mit.csail.ammolite.compression;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;

import edu.mit.csail.ammolite.database.CompressionType;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.FileUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.PubchemID;
import edu.mit.csail.ammolite.utils.SDFUtils;
import edu.mit.csail.ammolite.utils.StructID;

public class CentroidCompressor {
    private Set<IMolStruct> centroids = new HashSet<IMolStruct>();
    private MoleculeStructFactory structFactory;
    private int numMols = 0;
    private int numReps = 0;
    private ExecutorService exService;
    CommandLineProgressBar progressBar;

    String dbName;
    String dbFolder;
    String structFolder;
    String sourceFolder;
    
    public CentroidCompressor(CompressionType compType){
        structFactory = new MoleculeStructFactory( compType);
    }
    
    public void  compress(List<String> filenames, double clusterThreshold, String filename) throws IOException, CDKException, InterruptedException, ExecutionException{
        compress(filenames, filename, clusterThreshold, -1);
    }
    
    /**
     * Scans through an sdf library and compresses it.
     * 
     * @param folder_name location of the database to be compressed
     * @param filename name for the compressed database
     * @throws IOException
     * @throws CDKException
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    public void  compress(List<String> filenames, String filename, double clusterThreshold, int numThreads) throws IOException, CDKException, InterruptedException, ExecutionException{
        int numMols = 0;
        for(String name: filenames){
            numMols += SDFUtils.estimateNumMolsInSDF(name);
        }
        System.out.println("Compressing approximatley "+String.format("%,d", numMols)+" molecules.");
        
        String[] splitFile = filename.split(File.separator);
        this.dbName = splitFile[ splitFile.length - 1];
        this.makeDBFolders(filename);
        
        if( numThreads > 0){
            exService = ParallelUtils.buildNewExecutorService(numThreads);
        } else {
            int defaultThreads = Runtime.getRuntime().availableProcessors() / 2;
            exService = ParallelUtils.buildNewExecutorService(defaultThreads);
        }
        
        progressBar = new CommandLineProgressBar("Matching Structures", numMols);
        List<String> absoluteFilenames = new ArrayList<String>();
        List<File> files = FileUtils.openFiles(filenames);
        for(File f: files){
            absoluteFilenames.add(f.getPath());
            Iterator<IAtomContainer> molecules = SDFUtils.parseSDFOnline(f.getAbsolutePath());
            findClusters(molecules, clusterThreshold);
        }
        
        progressBar.done();

        System.out.print("Writing database... ");
        produceClusteredDatabase( filename );
        System.out.println("Done.");
        
        System.out.print("Shutting down threads... ");
        exService.shutdown();
        System.out.println("Done.");
        
        System.out.println("Total number of molecules: " +numMols);
        System.out.println("Total number of representatives: " +numReps);
    }

    private void findClusters(Iterator<IAtomContainer> molecules, double clusterThreshold) {
        while(molecules.hasNext()){
            IMolStruct struct = structFactory.makeMoleculeStruct( molecules.next());
            StructID sID = MolUtils.getStructID(struct);
            boolean matched = false;
            for(IMolStruct centroid: this.centroids){
                int mcs = MCS.getSMSDOverlap(struct, centroid);
                double bigMolSize = Math.max(MolUtils.getAtomCountNoHydrogen(struct), MolUtils.getAtomCountNoHydrogen(centroid));
                double distance = 1.0 - (mcs / bigMolSize);
                if( distance <= clusterThreshold){
                    centroid.addID( MolUtils.getPubID(struct));
                    sID = MolUtils.getStructID(centroid);
                    matched = true;
                    break;
                }
            }
            if(!matched){
                centroids.add(struct);
            }
            
            putMolInSourceFile(sID, struct);
        }
        
    }
    
    private void makeDBFolders(String filename){
        this.dbFolder = filename+ ".gad";
        File dir = new File(dbFolder);
        dir.mkdir();
        this.structFolder = dbFolder + File.separator + "struct_files";
        File structdir = new File(structFolder);
        structdir.mkdir();
        this.sourceFolder = dbFolder + File.separator + "source_files";
        File sourcedir = new File(sourceFolder);
        sourcedir.mkdir();
    }
    
    
    private void putMolInSourceFile(StructID id, IAtomContainer mol){
        String filename = sourceFolder + File.separator + id.toString() +".sdf";
        try {
            FileWriter fw = new FileWriter(filename, true);
            SDFWriter writer = new SDFWriter(fw);
            writer.write(mol);
            writer.close();
            fw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (CDKException ce) {
            ce.printStackTrace();
        }
        
    }
    
    
    /**
     * Produce the files representing the clustered database.
     * 
     * @param filename
     * @throws CDKException
     * @throws IOException
     */
    private void produceClusteredDatabase( String name ){
        
        String structTable = writeStructIDFile(centroids.iterator());
        writeStructSDF( centroids.iterator());
        writeMetadataFile("AMMOLITE_GENERIC_DATABASE_0_0_0", structTable);
        
    }
    
   private  String writeStructIDFile(Iterator<IMolStruct> structs){
        String structName = "structids.yml";
        String structPath = dbFolder + File.separator + structName;
        BufferedWriter writer;
        try{
            FileWriter fw = new FileWriter(structPath);
            writer = new BufferedWriter(fw);
            while( structs.hasNext()){
                IMolStruct struct = structs.next();
                writer.write( MolUtils.getStructID(struct).toString());
                writer.write(" : [");
                for(PubchemID pId: struct.getIDNums() ){
             
                    writer.write( pId.toString());
                    writer.write(", ");
                }
                writer.write("]");
                writer.newLine();
            }
            writer.close();
        } catch(IOException ioe){
            ioe.printStackTrace();
        }
        return structName;
    }
    
   private  List<String> writeStructSDF(Iterator<IMolStruct> structs){
        int CHUNK_SIZE = 25*1000;
        int fileNum = -1;
        int structsInFile = 0;

        String structBaseName = "struct_%d.sdf";
        OutputStream stream = null;
        SDFWriter writer = null;
        List<String> allFiles = new ArrayList<String>();
        
        while( structs.hasNext()){
            if(structsInFile == CHUNK_SIZE || fileNum == -1){
                fileNum++;
                structsInFile = 0;
                try{
                    try{
                        writer.close();
                        stream.close();
                    } catch( NullPointerException npe) {}
                    
                    String structName = String.format(structBaseName, fileNum);
                    String structPath = structFolder + File.separator + structName;
                    allFiles.add(structName);
                    stream = new PrintStream(structPath);
                    writer = new SDFWriter( stream);
                } catch( IOException ioe){
                    ioe.printStackTrace();
                } 
                
            }
            
            IMolStruct struct = structs.next();
            struct.setProperty("AMMOLITE_STRUCTURE_ID", MolUtils.getUnknownOrID(struct));
            try {
                writer.write(struct);
                structsInFile++;
            } catch (CDKException e) {
                e.printStackTrace();
            }
        }
        try {
            writer.close();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allFiles;
    }
   
   
   private void writeMetadataFile(String version, String structTable){

        String metaPath = dbFolder + File.separator +  "metadata.yml";
        BufferedWriter writer;
        try{
            FileWriter fw = new FileWriter(metaPath);
            writer = new BufferedWriter(fw);
        
            writer.write("NAME: ");
            writer.write(dbName);
            writer.newLine();
        
            writer.write("VERSION: ");
            writer.write(version);
            writer.newLine();
        
            writer.write("COMPRESSION_TYPE: ");
            if( this.structFactory.getCompressionType() == CompressionType.CYCLIC){
                writer.write("CYCLIC_CENTROIDS");
            } else if (this.structFactory.getCompressionType() == CompressionType.BASIC){
                writer.write("BASIC_CENTROIDS");
            } else if (this.structFactory.getCompressionType() == CompressionType.LABELED){
                writer.write("LABELED_CENTROIDS");
            } else if (this.structFactory.getCompressionType() == CompressionType.WEIGHTED){
                writer.write("WEIGHTED_CENTROIDS");
            } else if (this.structFactory.getCompressionType() == CompressionType.NONE){
                writer.write("NONE_CENTROIDS");
            } else {
                writer.write("OTHER");
            }
            writer.newLine();
        
            writer.write("ORGANIZED: ");
            writer.write("TRUE");
            writer.newLine();
            
            writer.write("NUM_MOLS: ");
            writer.write(String.format("%d", this.numMols));
            writer.newLine();
            
            writer.write("NUM_REPS: ");
            writer.write(String.format("%d", this.numReps));
            writer.newLine();
        
            writer.write("STRUCT_ID_TABLE: ");
            writer.write(structTable);
            writer.newLine();
            
            writer.write("STRUCTURE_FILES:");
            writer.newLine();
            writer.write("  - ");
            writer.write("./struct_files/*");
            writer.newLine();
            
            
            writer.write("SOURCE_FILES: ");
            writer.newLine();
            writer.write("  - ");
            writer.write("./source_files/*");
            writer.newLine();
            
            writer.close();
            
        } catch(IOException ioe){
        ioe.printStackTrace();
        }
    }
   

}
