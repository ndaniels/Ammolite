package speedysearch;

import java.io.IOException;

import org.openscience.cdk.exception.CDKException;


public class SpeedySearch {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
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
