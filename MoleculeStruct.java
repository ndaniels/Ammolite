package speedySMSD;

import org.openscience.cdk.Atom;
import org.openscience.cdk.annotations.IAtom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.annotations.IBond

/**
 * A class representing the structure of a molecule.
 * All bonds and atoms are identical and generic.
 * 
 * @author  David Danko
 */
public Class MoleculeStruct extends AtomContainer
{

	public MoleculeStruct( AtomContainer base )
	{
		Iterator atoms = base.atoms()
		for( IAtom atom = atoms.next(); atoms.hasNext(); atom = atoms.next() )
		{
			this.addAtom( atom );
		}

		Iterator bonds = base.bonds()
		for( IBond bond = bonds.next(); bonds.hasNext(); bond = bonds.next() )
		{
			this.addBond( bond );
		}
	}

	@Overrides 
	public void addAtom(IAtom atom)
	{
		generic_atom = new Atom();
		super.addAtom( generic_atom );
	}

	@Overrides
	public void addBond(IBond bond)
	{
		generic_bond = new Bond();
		super.addBond( generic_bond );
	}
}