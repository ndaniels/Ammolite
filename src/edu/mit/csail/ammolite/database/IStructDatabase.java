package edu.mit.csail.ammolite.database;

import java.util.Iterator;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.compression.MoleculeStructFactory;

public interface IStructDatabase {
	
	public CompressionType getCompressionType();
	public IAtomContainer getMolecule(String pubchemID);
	public int numReps();
	public int numMols();
	public String info();
	public double convertThreshold(double threshold, double probability, boolean useTanimoto);
	public MolStruct makeMoleculeStruct(IAtomContainer mol);
	public Iterator<MolStruct> iterator();
	public MoleculeStructFactory getStructFactory();
}
