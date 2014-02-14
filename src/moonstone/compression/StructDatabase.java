package moonstone.compression;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;

import speedysearch.IteratingSDFReader;
import speedysearch.KeyListMap;

public class StructDatabase implements Serializable{

	private static final long serialVersionUID = 1L;
	private KeyListMap<Integer, MoleculeStruct> structsByHash;
	private HashMap<String, FilePair> fileLocsByID;
	private MoleculeStructFactory structFactory;
	
	public StructDatabase(	KeyListMap<Integer, MoleculeStruct> _structsByHash, 
							HashMap<String, FilePair> _fileLocsByID, 
							MoleculeStructFactory _structfactory){
		structsByHash = _structsByHash;
		fileLocsByID = _fileLocsByID;
		structFactory = _structfactory;
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
		return new StructIterator( structsByHash);
	}
	
	public int numReps(){
		int numReps = 0;
		for(int key: this.structsByHash.keySet()){
			numReps += structsByHash.get(key).size();
		}
		return numReps;
		
	}
	
	
}
