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
import edu.mit.csail.ammolite.utils.StructID;

public class GenericStructDatabase implements IStructDatabase {
    String name;
    String version;
    CompressionType compression;
    MoleculeStructFactory sFactory;
    List<MolStruct> structs;
    ISDFSet sourceFiles;
    
    
    
    public GenericStructDatabase(String name, String version, String compression, boolean organized,
            Map<StructID, List<PubchemID>> idMap, List<String> structFiles, List<String> sourceFiles) {
        
        this.name = name;
        this.version = version;
        this.compression = CompressionType.valueOf(compression);
        this.sFactory = new MoleculeStructFactory(this.compression);
        
        structs = new ArrayList<MolStruct>();
        SDFMultiParser structParser = new SDFMultiParser(structFiles);
        while( structParser.hasNext()){
            IAtomContainer rawStruct = structParser.next();
            MolStruct struct = this.sFactory.makeMoleculeStruct(rawStruct);
            for(PubchemID pID: idMap.get(MolUtils.getStructID(struct))){
                struct.addID(pID);
            }
            structs.add(struct);
        }
        
        if( organized) {
            this.sourceFiles = new OrganizedSDFSet(sourceFiles);
        } else {
            this.sourceFiles = new SDFSet( sourceFiles);
        }
        
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
        return this.structs.size();
    }

    @Override
    public int numMols() {
        int numMols = 0;
        for(MolStruct rep: this.structs){
            numMols += rep.getIDNums().size();
        }
        return numMols;
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
        sb.append("Number of molecules: "+String.format("%,d", numMols())+"\n");
        sb.append("Number of representatives: "+String.format("%,d", numReps())+"\n");
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
        return structs.iterator();
    }

    @Override
    public MoleculeStructFactory getStructFactory() {
        return sFactory;
    }

    @Override
    public List<MolStruct> getStructs() {
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

}
