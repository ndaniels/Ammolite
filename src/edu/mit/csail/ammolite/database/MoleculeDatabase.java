package edu.mit.csail.ammolite.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.compression.MoleculeStruct;

public class MoleculeDatabase implements IStructDatabase {
	private Map<String,IAtomContainer> idToMolecule = new HashMap<String,IAtomContainer>();
	private List<IAtomContainer> molecules = new LinkedList<IAtomContainer>();
	
	public MoleculeDatabase(String filename){
		IteratingSDFReader sdf = getReader( filename);
		while( sdf.hasNext()){
			IAtomContainer mol = sdf.next();
			molecules.add(mol);
			idToMolecule.put(mol.getID(), mol);
		}
	}
	private IteratingSDFReader getReader(String filename){
		try {
			File f = new File(filename);
			FileInputStream fs = new FileInputStream(f);
			BufferedReader br = new BufferedReader( new InputStreamReader(fs ));
			return new IteratingSDFReader( br, DefaultChemObjectBuilder.getInstance() );
		} catch (IOException e) {
			System.exit(-1);
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public CompressionType getCompressionType() {
		return CompressionType.NONE;
	}

	@Override
	public IAtomContainer getMolecule(String pubchemID) {
		return idToMolecule.get(pubchemID);
	}

	@Override
	public int numReps() {
		return numMols();
	}

	@Override
	public int numMols() {
		return molecules.size();
	}

	@Override
	public String info() {
		StringBuilder sb = new StringBuilder();
		sb.append("Molecule Database Info\n");
		sb.append("Number of molecules: "+numMols()+"\n");
		sb.append("No compression.");
		return sb.toString();
	}

	@Override
	public double convertThreshold(double threshold, double probability, boolean useTanimoto) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IAtomContainer makeMoleculeStruct(IAtomContainer mol) {
		return mol;
	}

	@Override
	public Iterator<IAtomContainer> iterator() {
		return molecules.iterator();
	}

}
