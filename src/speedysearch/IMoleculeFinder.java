package speedysearch;

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtomContainer;

public interface IMoleculeFinder {
	
	/* Coarse Search Methods */

	public ArrayList<IAtomContainer> exactStructuralMatches( IAtomContainer query );
	
	public ArrayList<IAtomContainer> exactStructuralMatches( String filename );
	
	public String exactStructuralMatchIDs( IAtomContainer query );
	
	public String exactStructuralMatchIDs( String filename );
	
	public ArrayList<IAtomContainer> maximalCommonStructures( IAtomContainer query );
	
	public ArrayList<IAtomContainer> maximalCommonStructures( String filename );
	
	public String maximalCommonStructureIDs( IAtomContainer query );
	
	public String maximalCommonStructureIDs( String filename );
	
	/* Fine Search Methods */
	
	public IAtomContainer exactMolecularMatch( IAtomContainer query, ArrayList<IAtomContainer> set );
	
	public String exactMolecularMatchID( IAtomContainer query, ArrayList<IAtomContainer> set );
	
	public ArrayList<IAtomContainer> maximalCommonMolecules( IAtomContainer query, ArrayList<IAtomContainer> set );
	
	public String maximalCommonMoleculeIDs( IAtomContainer query, ArrayList<IAtomContainer> set );
	
	/* Other Methods */
	
	public void loadDatabase(String filename);
	
	
	
}
