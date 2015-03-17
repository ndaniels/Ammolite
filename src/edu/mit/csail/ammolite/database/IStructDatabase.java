package edu.mit.csail.ammolite.database;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.IMolStruct;
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
	public double guessCoarseThreshold(double fineThreshold);
	public IMolStruct makeMoleculeStruct(IAtomContainer mol);
	public Iterator<IMolStruct> iterator();
	public MoleculeStructFactory getStructFactory();
	public List<IMolStruct> getStructs();
	public ISDFSet getSourceFiles();
	public List<IAtomContainer> getMatchingMolecules(StructID structID);
	public String getName();
	public boolean isOrganized();
	public int  countFineHits(Collection<StructID> coarseHits);
	

}
