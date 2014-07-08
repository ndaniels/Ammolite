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
import edu.mit.csail.ammolite.utils.Logger;

public class StructDatabase implements IStructDatabase{
	


	protected KeyListMap<Integer, MoleculeStruct> structsByHash;
	protected HashMap<String, FilePair> fileLocsByID;
	protected MoleculeStructFactory structFactory;
	protected CompressionType compressionType;
	protected List<MoleculeStruct> linearStructs = null;
	protected int numReps = -1;
	protected int numMols = -1;
	
	

	public StructDatabase(	StructDatabaseCoreData coreData){
		structsByHash = coreData.structsByHash;
		fileLocsByID = coreData.fileLocsByID;
		compressionType = coreData.compressionType;
		structFactory = new MoleculeStructFactory( compressionType);
		buildLinearSet();
	}
	
	public MoleculeStructFactory getStructFactory(){
		return structFactory;
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
	
//	public HashMap<String, FilePair> getFileLocsByID(){
//		return this.fileLocsByID;
//	}
	
	public IAtomContainer getMolecule(String pubchemID){
		
		FilePair fp = fileLocsByID.get(pubchemID);
		
		String filename = fp.name();
		long byteOffset = fp.location();
		
		File f = new File(filename);
		FileInputStream fs;
		try {
			fs = new FileInputStream(f);
			BufferedReader br = new BufferedReader( new InputStreamReader(fs ));
			if( byteOffset -5 > 0){
				br.skip(byteOffset-5); // TODO
				/**
				 * Why the -5? The -5 is here because the code that finds the offsets (version 0) 
				 * finds the offset after the delimiter string '$$$$\n' which causes a lot of issues
				 * for the reader. This should be fixed (obv.)
				 */
			}
			IteratingSDFReader molecule =new IteratingSDFReader( br, DefaultChemObjectBuilder.getInstance() );
			IAtomContainer out = null;
			if(molecule.hasNext()){
				out = molecule.next();
			} else {
				Logger.debug("\n");
				Logger.debug("Missing molecule, here are some lines from the file");
				Logger.debug("----------------------------------------------------\n");
//				FileInputStream fs2 = new FileInputStream(f);
//				BufferedReader br2 = new BufferedReader( new InputStreamReader(fs2 ));
				br.skip(byteOffset);
				for(int i=0; i<30; ++i){
					Logger.debug(br.readLine());
				}
				Logger.debug("\n----------------------------------------------------\n");
				Logger.debug("Trying to load it anyways...");
				out = molecule.next();
			}
			
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
	
	protected void buildLinearSet(){
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
	
	public int numMols(){
		if( numMols == -1){
			numMols = 0;
			for(List<MoleculeStruct> repSet: structsByHash.values()){
				for(MoleculeStruct rep: repSet){
					numMols += rep.getIDNums().length;
				}
			}
		}
		return numMols;
	}
	
	public String info(){
		StringBuilder sb = new StringBuilder();
		sb.append("Ammolite Database Info\n");
		sb.append("Number of molecules: "+numMols()+"\n");
		sb.append("Number of representatives: "+numReps()+"\n");
		sb.append("Compression Type: "+compressionType+"\n");
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
	
	public MoleculeStruct makeMoleculeStruct(IAtomContainer mol){
		return structFactory.makeMoleculeStruct(mol);
	}
	
	
}
