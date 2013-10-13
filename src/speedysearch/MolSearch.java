package speedysearch;

import java.io.IOException;

import org.openscience.cdk.exception.CDKException;


public class MolSearch {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Welcome to MolSearch");
		if(args.length > 0 && args[0].equals("--compress")){
			
			compress_database( args[1] );
		}

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
