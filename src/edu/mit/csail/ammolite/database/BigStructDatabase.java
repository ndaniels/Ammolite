package edu.mit.csail.ammolite.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.utils.Logger;

public class BigStructDatabase extends StructDatabase{
	private Map<String, IAtomContainer> idToMolecule = new HashMap<String, IAtomContainer>();
	/**
	 * For testing, like structdatabase but searches an entire file for the appropriate molecule.
	 * @param coredata
	 */
	public BigStructDatabase( StructDatabaseCoreData coredata){
		super(coredata);
	}
	
	public void preloadMolecules(){
		Set<String> filenames = new HashSet<String>();
		for(FilePair fp: fileLocsByID.values()){
			String filename = fp.name();
			if( !filenames.contains(filename)){
				filenames.add(filename);
			}
		}
		for(String filename: filenames){
			
			File f = new File(filename);
			FileInputStream fs;
			try {
				fs = new FileInputStream(f);
				BufferedReader br = new BufferedReader( new InputStreamReader(fs ));
				IteratingSDFReader file =new IteratingSDFReader( br, DefaultChemObjectBuilder.getInstance() );
				while( file.hasNext()){
					IAtomContainer out = file.next();
					idToMolecule.put((String) out.getProperty("PUBCHEM_COMPOUND_CID"), out);
				}
			} catch (IOException e) {
				System.exit(-1);
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public IAtomContainer getMolecule(String pubchemID){
		if(idToMolecule.containsKey(pubchemID)){
			return idToMolecule.get(pubchemID);
		}
		
		else{
			
			FilePair fp = fileLocsByID.get(pubchemID);
			
			String filename = fp.name();
			long byteOffset = fp.location();
			
			File f = new File(filename);
			FileInputStream fs;
			try {
				fs = new FileInputStream(f);
				BufferedReader br = new BufferedReader( new InputStreamReader(fs ));
				if( byteOffset -5 > 0){
					//br.skip(byteOffset-5); // TODO
					/**
					 * Why the -5? The -5 is here because the code that finds the offsets (version 0) 
					 * finds the offset after the delimiter string '$$$$\n' which causes a lot of issues
					 * for the reader. This should be fixed (obv.)
					 * 
					 * Actually not sure this is the issue... not sure what else might be.
					 */
				}
				IteratingSDFReader molecule =new IteratingSDFReader( br, DefaultChemObjectBuilder.getInstance() );
				while( molecule.hasNext()){
					if(molecule.hasNext()){
						IAtomContainer out = molecule.next();
						idToMolecule.put((String) out.getProperty("PUBCHEM_COMPOUND_CID"), out);
					}
				}
			} catch (IOException e) {
				System.exit(-1);
				e.printStackTrace();
			}
		}
		return idToMolecule.get(pubchemID);
	
		}
	
	public Collection<IAtomContainer> getMolecules(){
		return idToMolecule.values();
	}

}
