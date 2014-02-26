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
import edu.mit.csail.ammolite.compression.MoleculeStruct;
import edu.mit.csail.ammolite.compression.MoleculeStructFactory;

public class StructDatabase{
	


	private KeyListMap<Integer, MoleculeStruct> structsByHash;
	private HashMap<String, FilePair> fileLocsByID;
	private MoleculeStructFactory structFactory;
	private CompressionType compressionType;
	private List<MoleculeStruct> linearStructs = null;
	private int numReps = -1;
	
	

	public StructDatabase(	StructDatabaseCoreData coreData){
		structsByHash = coreData.structsByHash;
		fileLocsByID = coreData.fileLocsByID;
		compressionType = coreData.compressionType;
		structFactory = new MoleculeStructFactory( compressionType);
	}
	
	public CompressionType getCompressionType(){
		return compressionType;
	}
	
	public MoleculeStructFactory getMoleculeStructFactory(){
		return structFactory;
	}
	
	public List<MoleculeStruct> getStructsByHash(int hash){
		return structsByHash.get(hash);
	}
	
	public KeyListMap<Integer, MoleculeStruct> getStructsByHash(){
		return this.structsByHash;
	}
	
	public HashMap<String, FilePair> getFileLocsByID(){
		return this.fileLocsByID;
	}
	
	public IAtomContainer getMolecule(String pubchemID){
		
		FilePair fp = fileLocsByID.get(pubchemID);
		String filename = fp.name();
		long byteOffset = fp.location();
		
		File f = new File(filename);
		FileInputStream fs;
		try {
			fs = new FileInputStream(f);
			fs.skip(byteOffset);
			BufferedReader br = new BufferedReader( new InputStreamReader(fs ));
			IteratingSDFReader molecule =new IteratingSDFReader( br, DefaultChemObjectBuilder.getInstance() );
			IAtomContainer out = molecule.next();
			
			fs.close();
			br.close();
			molecule.close();
			
			return out;
			
		} catch (IOException e) {
			System.exit(-1);
			e.printStackTrace();
		}
		return null;
		

	}
	
	public Iterator<MoleculeStruct> iterator(){
		if( linearStructs == null){
			buildLinearSet();
		}
		return linearStructs.iterator();		
	}
	
	private void buildLinearSet(){
		linearStructs = new ArrayList<MoleculeStruct>( numReps());
		for(List<MoleculeStruct> repSet: structsByHash.values()){
			linearStructs.addAll(repSet);
		}

	}
	
	public int numReps(){
		if( numReps == -1){
			numReps = 0;
			for(List<MoleculeStruct> repSet: structsByHash.values()){
				numReps += repSet.size();
			}
		}
		return numReps;
		
	}
	
	public double convertThreshold(double threshold, double probability, boolean useTanimoto){
		if( compressionType == CompressionType.CYCLIC){
			return 0.5 * threshold + 0.3 - 0.2 * probability + 0.2; 
		}
		return 0.0;
	}
	
	public MoleculeStruct makeMoleculeStruct(IAtomContainer mol){
		return structFactory.makeMoleculeStruct(mol);
	}
	
	
}
