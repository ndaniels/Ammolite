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
import edu.mit.csail.ammolite.utils.Logger;

public class StructDatabase implements IStructDatabase{
	


	protected KeyListMap<Integer, MolStruct> structsByHash;
	protected ISDFSet sdfFiles;
	protected MoleculeStructFactory structFactory;
	protected CompressionType compressionType;
	protected List<MolStruct> linearStructs = null;
	protected int numReps = -1;
	protected int numMols = -1;
	protected String VERSION;
	protected IDatabaseCoreData coreData;
	
	

	public StructDatabase(	IDatabaseCoreData _coreData){
		coreData = _coreData;
		structsByHash = coreData.getFingerprintTable();
		sdfFiles = coreData.getSDFSet();
		compressionType = coreData.getCompressionType();
		structFactory = new MoleculeStructFactory( compressionType);
		VERSION = coreData.getVersion();
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
	
	public List<MolStruct> getStructsByHash(int hash){
		return structsByHash.get(hash);
	}
	
	public KeyListMap<Integer, MolStruct> getStructsByHash(){
		return this.structsByHash;
	}
	
//	public HashMap<String, FilePair> getFileLocsByID(){
//		return this.fileLocsByID;
//	}
	
	public IAtomContainer getMolecule(String pubchemID){
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
		for(List<MolStruct> repSet: structsByHash.values()){
			linearStructs.addAll(repSet);
		}

	}
	
	public int numReps(){
		if( numReps == -1){
			numReps = 0;
			for(List<MolStruct> repSet: structsByHash.values()){
				numReps += repSet.size();
			}
		}
		return numReps;
		
	}
	
	public int numMols(){
		if( numMols == -1){
			numMols = 0;
			for(List<MolStruct> repSet: structsByHash.values()){
				for(MolStruct rep: repSet){
					numMols += rep.getIDNums().length;
				}
			}
		}
		return numMols;
	}
	
	public String info(){
		StringBuilder sb = new StringBuilder();
		sb.append("Ammolite Database Info\n");
		sb.append("Database version: "+VERSION+"\n");
		sb.append("Number of molecules: "+String.format("%,d", numMols())+"\n");
		sb.append("Number of representatives: "+String.format("%,d", numReps())+"\n");
		sb.append("Compression Type: "+compressionType+"\n");
		return sb.toString();
	}
	
	public String asTable(){
		StringBuilder sb = new StringBuilder();
		sb.append("BEGIN_TABLE\n");
		for(MolStruct struct: getStructs()){
			sb.append(struct.getProperty("PUBCHEM_COMPOUND_CID"));
			sb.append(", ");
			for(String id: struct.getIDNums()){
				sb.append(id);
				sb.append(", ");
			}
			sb.append("\n");
		}
		sb.append("END_TABLE\n");
		return sb.toString();
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
	
	
}
