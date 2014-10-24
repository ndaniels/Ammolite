package edu.mit.csail.ammolite.database;

import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.compression.MoleculeStructFactory;
import edu.mit.csail.ammolite.utils.PubchemID;
import edu.mit.csail.ammolite.utils.StructID;

public interface IStructDatabase {
	
	public CompressionType getCompressionType();
	public IAtomContainer getMolecule(PubchemID pubchemID);
	public int numReps();
	public int numMols();
	public String info();
	public String asTable();
	public double convertThreshold(double threshold, double probability, boolean useTanimoto);
	public MolStruct makeMoleculeStruct(IAtomContainer mol);
	public Iterator<MolStruct> iterator();
	public MoleculeStructFactory getStructFactory();
	public List<MolStruct> getStructs();
	@Deprecated
	public IDatabaseCoreData getCoreData();
	public ISDFSet getSourceFiles();
	public List<IAtomContainer> getMatchingMolecules(StructID structID);
	public String getName();
	public boolean isOrganized();

}
