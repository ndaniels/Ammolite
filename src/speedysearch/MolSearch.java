package speedysearch;

import java.io.IOException;

import org.openscience.cdk.exception.CDKException;


public class MolSearch {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Welcome to MolSearch");
		if(args.length == 2 && args[0].equals("--compress")){
			compress_database( args[1] );
		} else if( args.length == 3 && args[0].equals("--matchID") ){
			findID(args[1], args[2]);
		} else if( args.length == 4 && args[0].equals("--matchMol") ){
			StructFinder sf = new StructFinder(args[1], args[2]);
			System.out.println("Looking for exact structural matches to "+args[3]+" in "+args[1]);
			System.out.println(sf.exactQuery(args[3]));
		}

	}
	
	
	private static void findID(String struct_filename, String id){
		System.out.println("Looking for "+id+" in "+struct_filename);
		System.out.println(StructFinder.getMatchingIDs(struct_filename, id));
	}
	private static void compress_database(String db_name){
		System.out.println("Preparing to make a compressed version of " + db_name);
		try {
			StructCompressor.compress(db_name);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CDKException e) {
			e.printStackTrace();
		}
	}

}
