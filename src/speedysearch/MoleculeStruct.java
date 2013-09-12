package speedysearch;

import java.util.Iterator;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.smsd.BaseMapping;
import org.openscience.smsd.Isomorphism;
import org.openscience.smsd.interfaces.Algorithm;

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
		base = new AtomContainer(AtomContainerManipulator.removeHydrogens(base));
		
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
	
	/**
	 * TODO: Implement
	 * 
	 * @param that
	 * @return
	 */
	public boolean isIsomorphic( IAtomContainer that){
		if( !(that instanceof MoleculeStruct )){
			that = new MoleculeStruct( that );
		}
		
		BaseMapping smsd = new Isomorphism( that, this, Algorithm.VFLibMCS, true, true);
		
		return false;
	}
}