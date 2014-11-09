package edu.mit.csail.ammolite.database;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.compression.MoleculeStructFactory;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.PubchemID;
import edu.mit.csail.ammolite.utils.SDFMultiParser;
import edu.mit.csail.ammolite.utils.SDFMultiStructParser;
import edu.mit.csail.ammolite.utils.SDFUtils;
import edu.mit.csail.ammolite.utils.StructID;

public class GenericStructDatabase implements IStructDatabase {
    String name;
    String version;
    CompressionType compression;
    MoleculeStructFactory sFactory;
    List<MolStruct> structs = null;
    List<String> structFiles;
    Map<StructID, List<PubchemID>> idMap;
    ISDFSet sourceFiles;
    int numMols = -1;
    int numReps = -1;
    
    
    
    public GenericStructDatabase(String name, String version, String compression, boolean organized,
            Map<StructID, List<PubchemID>> idMap, List<String> structFiles, List<String> sourceFiles) {
        
        this.name = name;
        this.version = version;
        this.compression = CompressionType.valueOf(compression);
        this.sFactory = new MoleculeStructFactory(this.compression);
        this.idMap = idMap;
        this.structFiles = structFiles;
        
        if( organized) {
            this.sourceFiles = new OrganizedSDFSet(sourceFiles);
        } else {
            this.sourceFiles = new SDFSet( sourceFiles);
        }
        
    }
    
    private void cacheStructs(){
        if( this.structs != null){
            return;
        }
        System.out.print("Caching Representatives... ");
        structs = new ArrayList<MolStruct>();
        SDFMultiStructParser structParser = new SDFMultiStructParser( sourceFiles.getFilepaths(), sFactory);

        while( structParser.hasNext()){
            MolStruct struct = structParser.next();

            StructID key = MolUtils.getStructID(struct);

            List<PubchemID> pIDs = idMap.get(key);

            for(PubchemID pID: pIDs){
                struct.addID(pID);
            }
            structs.add(struct);
        }
        System.out.println("Done.");
    }
    

    @Override
    public CompressionType getCompressionType() {
        return this.compression;
    }

    @Override
    public IAtomContainer getMolecule(PubchemID pubchemID) {
        return this.sourceFiles.getMol(pubchemID);
    }

    @Override
    public int numReps() {
        if( numReps == -1){
            int num = 0;
            for(String filename: this.structFiles){
                num = SDFUtils.countNumMolsInSDF(filename);
            }
            this.setNumReps(num);
            return num;
        } else {
            return numReps;
        }
    }

    @Override
    public int numMols() {
        if( numMols == -1){
            int num = 0;
            for(MolStruct rep: this.getStructs()){
                num += rep.getIDNums().size();
            }
            this.setNumMols(num);
            return num;
        } else {
            return numMols;
        }

    }

    @Override
    public String info() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ammolite Database Info\n");
        sb.append("Database Name: "+name+"\n");
        sb.append("Database version: "+version+"\n");
        if( isOrganized() ){
            sb.append("This database has been organized.\n");
        } else {
            sb.append("This database has NOT been organized.\n");
        }
        sb.append(String.format("Number of molecules: %,d\n", numMols()));
        sb.append(String.format("Number of representatives: %,d\n", numReps()));
        sb.append("Compression Type: "+compression+"\n");
        if(sourceFiles.getFilenames().size() < 50){
            sb.append("Source Files:\n");
            sb.append(sourceFiles.listSourceFiles());
        } else {
            sb.append("Number of source files: ");
            sb.append(sourceFiles.getFilenames().size());
        }
        return sb.toString();
    }

    @Override
    public String asTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN_TABLE\n");
        for(MolStruct struct: getStructs()){
            sb.append(MolUtils.getPubID(struct));
            sb.append(", ");
            for(PubchemID id: struct.getIDNums()){
                sb.append(id.toString());
                sb.append(", ");
            }
            sb.append("\n");
        }
        sb.append("END_TABLE\n");
        return sb.toString();
    }

    @Override
    public double convertThreshold(double threshold, double probability,
            boolean useTanimoto) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public MolStruct makeMoleculeStruct(IAtomContainer mol) {
        return sFactory.makeMoleculeStruct(mol);
    }

    @Override
    public Iterator<MolStruct> iterator() {
        if( structs != null){
            return structs.iterator();
        }
        SDFMultiStructParser structParser = new SDFMultiStructParser( structFiles, sFactory);
        return structParser;
    }

    @Override
    public MoleculeStructFactory getStructFactory() {
        return sFactory;
    }

    @Override
    public List<MolStruct> getStructs() {
        if(structs == null){
            this.cacheStructs();
        }
        return structs;
    }

    @Override
    @Deprecated
    public IDatabaseCoreData getCoreData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ISDFSet getSourceFiles() {
        return sourceFiles;
    }

    @Override
    public List<IAtomContainer> getMatchingMolecules(StructID structID) {
        if(sourceFiles instanceof OrganizedSDFSet){
            return ((OrganizedSDFSet) sourceFiles).getMatchingMols(structID);
        } else {
            throw new UnsupportedOperationException("Database must be organized for this operation to work.");
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isOrganized() {
        return (sourceFiles instanceof OrganizedSDFSet);
    }
    
    public void setNumMols(int num){
        this.numMols = num;
    }
    
    public void setNumReps(int num){
        this.numReps = num;
    }

    @Override
    public double guessCoarseThreshold(double fineThreshold) {
        // TODO Auto-generated method stub
        return 0;
    }

}
