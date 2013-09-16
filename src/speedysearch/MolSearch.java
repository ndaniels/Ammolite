package speedysearch;

import java.io.IOException;

import org.openscience.cdk.exception.CDKException;


public class MolSearch {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.out.println("Compressing database...");
			StructCompressor compressor = new StructCompressor( args[0] );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("File not found");
			e.printStackTrace();
		} catch (CDKException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
