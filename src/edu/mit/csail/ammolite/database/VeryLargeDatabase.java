package edu.mit.csail.ammolite.database;

import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.compression.MoleculeStructFactory;
import edu.mit.csail.ammolite.utils.PubchemID;
import edu.mit.csail.ammolite.utils.StructID;

public class VeryLargeDatabase implements IStructDatabase {
	private SDFSet sourceFiles;
	private KeyListMap<StructID, PubchemID> compressionMapping;
	private MoleculeStructFactory structFactory;
	private List<String> structFiles;

	@Override
	public CompressionType getCompressionType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IAtomContainer getMolecule(PubchemID pubchemID) {
		return sourceFiles.getMol(pubchemID);
	}

	@Override
	public int numReps() {
		return compressionMapping.size();
	}

	@Override
	public int numMols() {
		int count = 0;
		for(List<PubchemID> l: compressionMapping.values()){
			count += l.size();
		}
		return count;
	}

	@Override
	public String info() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String asTable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double convertThreshold(double threshold, double probability,
			boolean useTanimoto) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public MolStruct makeMoleculeStruct(IAtomContainer mol) {
		return structFactory.makeMoleculeStruct(mol);
	}

	@Override
	public Iterator<MolStruct> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MoleculeStructFactory getStructFactory() {
		return structFactory;
	}

	@Override
	public List<MolStruct> getStructs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IDatabaseCoreData getCoreData() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISDFSet getSourceFiles() {
		return sourceFiles;
	}

	@Override
	public List<IAtomContainer> getMatchingMolecules(StructID structID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isOrganized() {
		// TODO Auto-generated method stub
		return false;
	}

}
