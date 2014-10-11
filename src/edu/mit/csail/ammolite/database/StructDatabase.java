package edu.mit.csail.ammolite.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.compression.CyclicStruct;
import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.compression.MoleculeStructFactory;
import edu.mit.csail.ammolite.utils.FileUtils;
import edu.mit.csail.ammolite.utils.Logger;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.PubchemID;
import edu.mit.csail.ammolite.utils.StructID;

public class StructDatabase implements IStructDatabase{
	


	protected KeyListMap<Integer, MolStruct> structsByFingerprint;
	protected ISDFSet sdfFiles;
	protected MoleculeStructFactory structFactory;
	protected CompressionType compressionType;
	protected List<MolStruct> linearStructs = null;
	protected int numReps = -1;
	protected int numMols = -1;
	protected String VERSION;
	protected String dbName;
	protected IDatabaseCoreData coreData;
	
	

	public StructDatabase(	IDatabaseCoreData _coreData){
		coreData = _coreData;
		structsByFingerprint = coreData.getFingerprintTable();
		sdfFiles = coreData.getSDFSet();
		compressionType = coreData.getCompressionType();
		structFactory = new MoleculeStructFactory( compressionType);
		VERSION = coreData.getVersion();
		dbName = coreData.getName();
		buildLinearSet();
	}
	
	public StructDatabase(IStructDatabase db) {
		this(db.getCoreData());
	}

	public MoleculeStructFactory getStructFactory(){
		return structFactory;
	}
	
	public IDatabaseCoreData getCoreData(){
		return coreData;
	}
	
	public CompressionType getCompressionType(){
		return compressionType;
	}
	
	public MoleculeStructFactory getMoleculeStructFactory(){
		return structFactory;
	}
	
	public List<MolStruct> getstructsByFingerprint(int hash){
		return structsByFingerprint.get(hash);
	}
	
	public KeyListMap<Integer, MolStruct> getstructsByFingerprint(){
		return this.structsByFingerprint;
	}
	
//	public HashMap<String, FilePair> getFileLocsByID(){
//		return this.fileLocsByID;
//	}
	
	public IAtomContainer getMolecule(PubchemID pubchemID){
		return sdfFiles.getMol(pubchemID);
	}
	
	public Iterator<MolStruct> iterator(){
		if( linearStructs == null){
			buildLinearSet();
		}
		return linearStructs.iterator();		
	}
	
	public List<MolStruct> getStructs(){
		if( linearStructs == null){
			buildLinearSet();
		}
		return linearStructs;
	}
	protected void buildLinearSet(){
		linearStructs = new ArrayList<MolStruct>( numReps());
		for(List<MolStruct> repSet: structsByFingerprint.values()){
			linearStructs.addAll(repSet);
		}

	}
	
	public int numReps(){
		if( numReps == -1){
			numReps = 0;
			for(List<MolStruct> repSet: structsByFingerprint.values()){
				numReps += repSet.size();
			}
		}
		return numReps;
		
	}
	
	public int numMols(){
		if( numMols == -1){
			numMols = 0;
			for(List<MolStruct> repSet: structsByFingerprint.values()){
				for(MolStruct rep: repSet){
					numMols += rep.getIDNums().size();
				}
			}
		}
		return numMols;
	}
	
	public String info(){
		StringBuilder sb = new StringBuilder();
		sb.append("Ammolite Database Info\n");
		sb.append("Database Name: "+dbName+"\n");
		sb.append("Database version: "+VERSION+"\n");
		if( isOrganized() ){
			sb.append("This database has been organized.\n");
		} else {
			sb.append("This database has NOT been organized.\n");
		}
		sb.append("Number of molecules: "+String.format("%,d", numMols())+"\n");
		sb.append("Number of representatives: "+String.format("%,d", numReps())+"\n");
		sb.append("Compression Type: "+compressionType+"\n");
		if(sdfFiles.getFilenames().size() < 50){
			sb.append("Source Files:\n");
			sb.append(sdfFiles.listSourceFiles());
		} else {
		    sb.append("Number of source files: ");
		    sb.append(sdfFiles.getFilenames().size());
		}
		return sb.toString();
	}
	
	public String asTable(){
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
	
	public boolean isOrganized(){
		if( sdfFiles instanceof OrganizedSDFSet){
			return true;
		}
		return false;
	}
	
	
	
	public double convertThreshold(double threshold, double probability, boolean useTanimoto){
		if( compressionType.equals( CompressionType.CYCLIC)){
			return (0.5 * threshold) - (0.2 * probability) + 0.2; 
		} else {
			Logger.debug(compressionType);
		}
		return 0.0;
	}
	
	public MolStruct makeMoleculeStruct(IAtomContainer mol){
		return structFactory.makeMoleculeStruct(mol);
	}

	public ISDFSet getSourceFiles() {
		return sdfFiles;
	}


	public List<IAtomContainer> getMatchingMolecules(StructID structID) {
		if(sdfFiles instanceof OrganizedSDFSet){
			return ((OrganizedSDFSet) sdfFiles).getMatchingMols(structID);
		} else {
			throw new UnsupportedOperationException("Database must be organized for this operation to work.");
		}
	}
	
	/** @return the name of the database, usually the file-root */
	public String getName() {
		return dbName;
	}
	
	/** 
	 * Save this database with any modified information 
	 * @param baseDirectory, the location to save the modified database
	 */
	public void save(String baseDirectory){
	    try {
            FileUtils.writeObjectToFile(baseDirectory, dbName, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	
}
