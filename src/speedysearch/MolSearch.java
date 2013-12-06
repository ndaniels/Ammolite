package speedysearch;

import java.io.IOException;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

public class MolSearch {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Welcome to MolSearch");
		if(args.length == 3 && args[0].equals("--compress")){
			compress_database( args[1], args[2] );
		}
		else if( args.length == 3 && args[0].equals("--testFMCS")){
			StructFinder.testMCS(args[1], args[2]);
		}
		else if( args.length == 3 && args[0].equals("--testSearch")){
			testSearch(args[1], args[2]);
		}

	}
	
	private static void mcsSearch(StructFinder sf, String query_filename){
		sf.mcsQuery(query_filename);
	}
	
	private static void findID(String struct_filename, String id){
		System.out.println("Looking for "+id+" in "+struct_filename);
		System.out.println(StructFinder.getMatchingIDs(struct_filename, id));
	}
	
	
	private static void compress_database(String db_name, String target){
		System.out.println("Preparing to make a compressed version of " + db_name);
		try {
			StructCompressor s = new StructCompressor(new MoleculeStructFactory(new MoleculeStruct()));
			s.compress( db_name, target);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CDKException e) {
			e.printStackTrace();
		}
	}
	
	private static void testSearch(String compressedDBName, String queryFile){
		StructDatabase db = null;
		System.out.println("Decompressing "+compressedDBName);
		try {
			db = new StructDatabaseFactory().decompress(compressedDBName);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Instantiating searcher");
		MoleculeSearcher searcher = new MoleculeSearcher(db, new MoleculeStructFactory(new MoleculeStruct()));
		
		System.out.println("Reading query "+queryFile);
		IAtomContainer query = convertToAtomContainer(queryFile);
		
		System.out.println("Searching...");
		IAtomContainer[] results = searcher.hybridSearch(query);
		System.out.println( results[0].toString());

	}
	
	
	private static IAtomContainer convertToAtomContainer(String filename){
		IAtomContainer mol = null;
		try {
			IteratingSDFReader query_file = new IteratingSDFReader(filename);
			IAtomContainer query = query_file.next();
			query_file.close();
			mol = new AtomContainer(query);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mol;
	}

}
