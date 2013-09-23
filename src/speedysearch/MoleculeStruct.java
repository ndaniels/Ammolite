package speedysearch;

import java.util.Iterator;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * A class representing the structure of a molecule.
 * All bonds and atoms are identical and generic.
 * 
 * @author  David Danko
 */
public class MoleculeStruct extends AtomContainer
{
	private int hash_code;
	
	private static final long serialVersionUID = 1L;

	public MoleculeStruct( IAtomContainer base )
	{	
		super( new AtomContainer(AtomContainerManipulator.removeHydrogens(base)) );
		
		Iterator<IAtom> atoms = this.atoms().iterator();
		while( atoms.hasNext() ){
			IAtom atom = atoms.next();
			atom.setAtomTypeName("C");
		}
		
		int max_atom_count = 0;
		int min_atom_count = 1000;
		Iterator<IBond> bonds = this.bonds().iterator();
		while( bonds.hasNext() ){
			IBond bond = bonds.next();
			if(bond.getAtomCount() > max_atom_count){
				max_atom_count = bond.getAtomCount();
			}
			if(bond.getAtomCount() < min_atom_count){
				min_atom_count = bond.getAtomCount();
			}
			bond.setOrder(IBond.Order.SINGLE);
		}
		
		hash_code = 1000000 * max_atom_count + 10000 * min_atom_count + 100 * this.bondCount + this.atomCount;

	}
	

	
	@Override
	public int hashCode(){
		return hash_code;
	}
}