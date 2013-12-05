package speedysearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;

public class StructDatabase {
	private KeyListMap<Integer, MoleculeStruct> structsByHash;
	private HashMap<String, FilePair> fileLocsByID;
	
	public StructDatabase(KeyListMap<Integer, MoleculeStruct> _structsByHash, HashMap<String, FilePair> _fileLocsByID){
		structsByHash = _structsByHash;
		fileLocsByID = _fileLocsByID;
	}
	
	public List<MoleculeStruct> getStructsByHash(int hash){
		return structsByHash.get(hash);
	}
	
	public IAtomContainer getMolecule(String pubchemID) throws IOException{
		FilePair fp = fileLocsByID.get(pubchemID);
		String filename = fp.name();
		long byteOffset = fp.location();
		
		File f = new File(filename);
		FileInputStream fs = new FileInputStream(f);
		fs.skip(byteOffset);
		BufferedReader br = new BufferedReader( new InputStreamReader(fs ));
		IteratingSDFReader molecule =new IteratingSDFReader( br, DefaultChemObjectBuilder.getInstance() );
		IAtomContainer out = molecule.next();
		
		fs.close();
		br.close();
		molecule.close();
		
		return out;
	}
	
	public Iterator<MoleculeStruct> iterator(){
		return StructIterator( structsByHash);
	}
	
	
}
