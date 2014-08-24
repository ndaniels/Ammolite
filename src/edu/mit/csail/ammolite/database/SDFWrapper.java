package edu.mit.csail.ammolite.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.input.CountingInputStream;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.SDFUtils;

public class SDFWrapper implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1027281343854616798L;
	String filepath;
	String filename;
	Map<String,Long> idsToOffsets = new HashMap<String, Long>();
	
	public SDFWrapper(String _filepath){
		filepath = _filepath;
		String[] splitPath = filepath.split("/");
		filename = splitPath[splitPath.length - 1];
		List<IAtomContainer> molecules = SDFUtils.parseSDF(filepath);
		List<Long> offsets = findOffsets();
		
		for(int i=0; i<molecules.size(); i++){
			IAtomContainer mol = molecules.get(i);
			long off = offsets.get(i);
			String pubID = MolUtils.getPubID(mol);
			idsToOffsets.put(pubID, off);
		}

	}
	
	public String getFilename(){
		return filename;
	}
	
	public String getFilepath(){
		return filepath;
	}
	
	public List<IAtomContainer> getAllMolecules(){
		return SDFUtils.parseSDF(filepath);
	}
	
	private List<Long> findOffsets(){
		List<Long> offsets = new ArrayList<Long>();
		try{
			BufferedReader br = getBR();
			long pos = 0;
			offsets.add((long) 0); //First molecule is always at 0
			String line = br.readLine();
			
			while( line != null){
				pos += line.length() + 1;
				if(line.contains("$$$$")){
	
					offsets.add(pos);
					
				}
				line = br.readLine();
			}
			
			br.close();
		} catch( IOException e){
			e.printStackTrace();
			System.exit(1);
		}
		return offsets;
		
		
	}
	
	public IAtomContainer getMol(String pubID){
		long off = idsToOffsets.get(pubID);
		BufferedReader br = getBR();
		try {
			br.skip(off);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		IteratingSDFReader molecules = new IteratingSDFReader( br, DefaultChemObjectBuilder.getInstance());
		if(molecules.hasNext()){
			return molecules.next();
		}
		return null;
	}
	
	public Set<String> getIDs(){
		return idsToOffsets.keySet();
	}
	
	private BufferedReader getBR(){
		FileInputStream fs = null;
		try {
			fs = new FileInputStream(filepath);
		} catch (FileNotFoundException e) {
			System.out.println("Could not build buffered reader");
			e.printStackTrace();
			System.exit(1);
		}
		BufferedReader br = new BufferedReader( new InputStreamReader(fs ));
		return br;
	}

}
