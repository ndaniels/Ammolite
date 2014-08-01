package edu.mit.csail.ammolite.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;

import edu.mit.csail.ammolite.IteratingSDFReader;

public class SDFUtils {
	

	
	public static List<IAtomContainer> parseSDF(String filename){
		IteratingSDFReader molecules = null;
		try{
			
		FileInputStream fs = new FileInputStream(filename);
		BufferedReader br = new BufferedReader( new InputStreamReader(fs ));
		molecules =new IteratingSDFReader( br, DefaultChemObjectBuilder.getInstance());
		} catch( IOException e){
			//edu.mit.csail.ammolite.Logger.error("Failed to read file");
			e.printStackTrace();
		}
		
		List<IAtomContainer> mols = new ArrayList<IAtomContainer>();
		while(molecules.hasNext()){
			mols.add( molecules.next());
		}
		return mols;
	}
	
	public static Iterator<IAtomContainer> parseSDFOnline(String filename){
		IteratingSDFReader molecules = null;
		try{
			
		FileInputStream fs = new FileInputStream(filename);
		BufferedReader br = new BufferedReader( new InputStreamReader(fs ));
		molecules =new IteratingSDFReader( br, DefaultChemObjectBuilder.getInstance());
		} catch( IOException e){
			//edu.mit.csail.ammolite.Logger.error("Failed to read file");
			e.printStackTrace();
		}
		return molecules;
	}
	
	public static void writeToSDF(List<? extends IAtomContainer> molecules, String filename){
		OutputStream stream = null;
		try {
			stream = new PrintStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		SDFWriter writer = new SDFWriter( stream);
		for(IAtomContainer mol: molecules){
			try {
				writer.write(mol);
			} catch (CDKException e) {
				e.printStackTrace();
			}
		}
		try {
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
