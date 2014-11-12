package edu.mit.csail.ammolite.compression;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.database.CompressionType;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.FileUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.PubchemID;
import edu.mit.csail.ammolite.utils.SDFUtils;
import edu.mit.csail.ammolite.utils.StructID;
import edu.ucla.sspace.graph.isomorphism.VF2IsomorphismTester;

public class Compressor_2 {
    private static final double MINIMUM_REP_OVERLAP = 0.6;
    private static final int MAX_EXPLORATION = 25*1000;
    private static final int CHUNK_SIZE = 1000;
    private KeyListMap<Integer, MolStruct> structsByFingerprint = new KeyListMap<Integer,MolStruct>(1000);
    private MoleculeStructFactory structFactory;
    private int numMols = 0;
    private int numReps = 0;

    private ExecutorService exService;
    CommandLineProgressBar progressBar;
    
    String dbName;
    String dbFolder;
    String structFolder;
    String sourceFolder;

    public Compressor_2(){
        structFactory = new MoleculeStructFactory(CompressionType.BASIC);
    }
    
    
    public void  compress(List<String> filenames, String filename) throws IOException, CDKException, InterruptedException, ExecutionException{
        compress(filenames, filename, -1);
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
    public void  compress(List<String> filenames, String filename, int numThreads) throws IOException, CDKException, InterruptedException, ExecutionException{
        int estNumMols = 0;
        for(String name: filenames){
            estNumMols += SDFUtils.estimateNumMolsInSDF(name);
        }
        System.out.println("Compressing approximatley "+String.format("%,d", estNumMols)+" molecules.");
        
        String[] splitFile = filename.split(File.separator);
        this.dbName = splitFile[ splitFile.length - 1];
        this.makeDBFolders(filename);
        
        if( numThreads > 0){
            exService = ParallelUtils.buildNewExecutorService(numThreads);
        } else {
            int defaultThreads = Runtime.getRuntime().availableProcessors() / 2;
            exService = ParallelUtils.buildNewExecutorService(defaultThreads);
        }
        
        progressBar = new CommandLineProgressBar("Matching Structures", estNumMols);
        List<String> absoluteFilenames = new ArrayList<String>();
        List<File> files = FileUtils.openFiles(filenames);
        for(File f: files){
            absoluteFilenames.add(f.getPath());
            Iterator<IAtomContainer> unorderedMolecules = SDFUtils.parseSDFOnline(f.getAbsolutePath());
            chunkAndProcessMolecules( unorderedMolecules); 
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
    
    
    private void chunkAndProcessMolecules(Iterator<IAtomContainer> unorderedMolecules) throws CDKException, InterruptedException, ExecutionException{
        List<IAtomContainer> chunk = new ArrayList<IAtomContainer>(CHUNK_SIZE);
        while(unorderedMolecules.hasNext()){
            chunk.add( unorderedMolecules.next());
            if(chunk.size() == CHUNK_SIZE){
                chunk.sort( new Comparator<IAtomContainer>(){

                    @Override
                    public int compare(IAtomContainer o1, IAtomContainer o2) {
                        return o1.getAtomCount() - o2.getAtomCount();
                    }
                    
                });
                checkDatabaseForIsomorphicStructs( chunk.iterator());
                chunk.clear();
            }
        }
        chunk.sort( new Comparator<IAtomContainer>(){

            @Override
            public int compare(IAtomContainer o1, IAtomContainer o2) {
                return o1.getAtomCount() - o2.getAtomCount();
            }
            
        });
        checkDatabaseForIsomorphicStructs( chunk.iterator());
        chunk.clear();
    }

    

    
    /**
     * Go through a file looking for matching elements of clusters
     * 
     * @param molecule_database
     * @param structFactory
     * @throws CDKException
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    private void checkDatabaseForIsomorphicStructs( Iterator<IAtomContainer> orderedMolecules) throws CDKException, InterruptedException, ExecutionException{

        while( orderedMolecules.hasNext() ){
            
            IAtomContainer rootMolecule =  orderedMolecules.next();        
            MolStruct rootStructure = structFactory.makeMoleculeStruct(rootMolecule);
            numMols++;
            int atomsToRemove = 0;
            int minimumRepSize = (int) Math.ceil(MINIMUM_REP_OVERLAP * rootStructure.getAtomCount());
            int maxAtomsToRemove = rootStructure.getAtomCount() - minimumRepSize;
            boolean isomorphFound = false;
            while( atomsToRemove <= maxAtomsToRemove && 
                    MAX_EXPLORATION > numCombinations( rootStructure.getAtomCount(), atomsToRemove)){
                List<MolStruct> candidateStructures = getPartialMols(rootStructure, atomsToRemove);
                isomorphFound = checkForIsomorphs( rootMolecule, candidateStructures);
                if(isomorphFound){
                    break;
                } else {
                    atomsToRemove++;
                }
            }
            
            if(!isomorphFound){
                int rootFingerprint = rootStructure.fingerprint();
                numReps++;
                structsByFingerprint.add(rootFingerprint, rootStructure);
                this.putMolInSourceFile(MolUtils.getStructID(rootStructure), rootMolecule);
             
            }
            progressBar.event();
        }
    }
    
    private boolean checkForIsomorphs(IAtomContainer rootMolecule, List<MolStruct> structureCandidates) throws InterruptedException, ExecutionException{
        for(MolStruct structure: structureCandidates){
            if( structsByFingerprint.containsKey( structure.fingerprint())){
                List<MolStruct> potentialMatches = structsByFingerprint.get( structure.fingerprint() );
                StructID matchID = parrallelIsomorphism( structure, potentialMatches);
                if( matchID != null ){
                    this.putMolInSourceFile(matchID, rootMolecule);
                    return true;
                } 
            }
        }
        return false;
    }
    
    private int numCombinations(int setSize, int choices){
        int larger = choices;
        int smaller = setSize - choices;
        if( smaller > larger){
            smaller = choices;
            larger = setSize - choices;
        }
        int numer = 1;
        for(int i=larger+1; i<setSize+1; i++){
            numer *= i;
        }
        int denom = 1;
        for(int i=2; i<smaller+1; i++){
            denom *= i;
        }
        
        return numer / denom;
    }
    
    private List<MolStruct> getPartialMols(MolStruct root, int depth){
        KeyListMap<Integer, IAtom> atomsByDeg = new KeyListMap<Integer, IAtom>(10);
        for(IAtom atom: root.atoms()){
            int deg = root.getConnectedAtomsCount(atom);
            atomsByDeg.add(deg, atom);
        }
        int onDegree = 1;
        int numRemoved = 0;
        List<MolStruct> partials = new ArrayList<MolStruct>();
        while( numRemoved < depth){
            if( atomsByDeg.containsKey(onDegree)){
                List<IAtom> ofDegree = atomsByDeg.get(onDegree);
                if( (depth - numRemoved) < ofDegree.size()){

                    List<List<IAtom>> combinationsToRemove = atomCombinations(ofDegree, (depth - numRemoved));
                    
                    Map<IAtom, Integer> atomsToVertices = new HashMap<IAtom, Integer>();
                    int i=0;
                    for(IAtom atom: root.atoms()){
                        atomsToVertices.put(atom, i);
                        i++;
                    }
                    
                    for(List<IAtom> combinationToRemove: combinationsToRemove){
                        MolStruct rootCopy = new MolStruct(root);
                        for(IAtom atom: combinationToRemove){
                            rootCopy.removeAtom( atomsToVertices.get(atom));
                        }
                        partials.add(rootCopy);

                    }
                    return partials;
                } else {
                    for(IAtom atom: ofDegree){
                        root.removeAtom(atom);
                        numRemoved++;
                    }
                }
            }
            onDegree++;
        }
        return partials; // only gets here if more atoms are removed than the molecule has
    }
    
    private List<List<IAtom>> atomCombinations(List<IAtom> atoms, int combSize){
        if(combSize == 0){
            List<IAtom> l = new ArrayList<IAtom>();
            return Arrays.asList( l);
        } else {
            int nextCombSize = combSize - 1;
            List<List<IAtom>> allCombinations = new ArrayList<List<IAtom>>();
            for(int i=0; i<(atoms.size() - nextCombSize); i++){
                IAtom pivot = atoms.get(i);
                List<IAtom> subList = atoms.subList(i+1, atoms.size());
                List<List<IAtom>> relevantCombinations = atomCombinations( subList, nextCombSize);
                for(List<IAtom> combination: relevantCombinations){
                    if(combination.size() != nextCombSize){
                        System.out.println("!!!");
                    }
                    combination.add(pivot);
                    allCombinations.add(combination);
                }
                
            }
            return allCombinations;
            
          }
        
        
    }
    
    
    
    /**
     * Checks whether a given structure is isomorphic to any structure in a list.
     * 
     * If an isomorphic structure is found the given structure's corresponding PubChem ID 
     * is added to the list of ids to the isomorphic structure
     * 
     * @param structure
     * @param potentialMatches
     * @return The ID of the matching structure or null if no structure matched. 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private StructID parrallelIsomorphism(MolStruct structure, List<MolStruct> potentialMatches) throws InterruptedException, ExecutionException{

        List<Callable<MolStruct>> callList = new ArrayList<Callable<MolStruct>>(potentialMatches.size());
        final MolStruct fStruct = structure;
        for (final MolStruct candidate: potentialMatches) {
            
            Callable<MolStruct> callable = new Callable<MolStruct>() {
                
                public MolStruct call() throws Exception {
                    VF2IsomorphismTester iso_tester = new VF2IsomorphismTester();
                    boolean iso = candidate.isIsomorphic(fStruct, iso_tester);
                    if( iso ){
                        candidate.addID( MolUtils.getPubID(fStruct));
                        return candidate;
                    } 
                    return null;
                }
            };
            callList.add(callable);

        }
        MolStruct match = ParallelUtils.parallelTimedSingleExecution( callList, 60*1000, exService);
        if(match == null){
            return null;
        } else {
            return MolUtils.getStructID( match);
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
        
        String structTable = writeStructIDFile(structsByFingerprint.valueIterator());
        writeStructSDF( structsByFingerprint.valueIterator());
        writeMetadataFile("AMMOLITE_GENERIC_DATABASE_0_0_0", structTable);
        
    }
    
   private  String writeStructIDFile(Iterator<MolStruct> structs){
        String structName = "structids.yml";
        String structPath = dbFolder + File.separator + structName;
        BufferedWriter writer;
        try{
            FileWriter fw = new FileWriter(structPath);
            writer = new BufferedWriter(fw);
            while( structs.hasNext()){
                MolStruct struct = structs.next();
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
    
   private  List<String> writeStructSDF(Iterator<MolStruct> structs){
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
            
            MolStruct struct = structs.next();
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
                writer.write("CYCLIC");
            } else if (this.structFactory.getCompressionType() == CompressionType.BASIC){
                writer.write("BASIC");
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
