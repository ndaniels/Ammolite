package speedysearch;

import java.util.Iterator;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IBond;

/**
 * A class representing the structure of a molecule.
 * All bonds and atoms are identical and generic.
 * 
 * @author  David Danko
 */
public class MoleculeStruct extends AtomContainer
{
	public MoleculeStruct( IAtomContainer base )
	{
		Iterator<IAtom> atoms = base.atoms().iterator();
		for( IAtom atom = atoms.next(); atoms.hasNext(); atom = atoms.next() )
		{
			this.addAtom( atom );
		}

		Iterator<IBond> bonds = base.bonds().iterator();
		for( IBond bond = bonds.next(); bonds.hasNext(); bond = bonds.next() )
		{
			this.addBond( bond );
		}
	}

	@Override
	public void addAtom(IAtom atom)
	{
		Atom generic_atom = new Atom("C");
		super.addAtom( generic_atom );
	}

	@Override
	public void addBond(IBond bond)
	{
		Bond generic_bond = new Bond( bond.getConnectedAtoms( bond.getAtom(0) ) );	// *Most* organic molecules have bonds only between pairs of molecules
																					// TODO: check the above
		super.addBond( generic_bond );
	}
}