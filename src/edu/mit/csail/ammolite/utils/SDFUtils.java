package edu.mit.csail.ammolite.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;

import edu.mit.csail.ammolite.IteratingSDFReader;
import edu.mit.csail.ammolite.compression.IMolStruct;
import edu.mit.csail.ammolite.compression.LabeledCyclicStruct;
import edu.mit.csail.ammolite.compression.MoleculeStructFactory;

public class SDFUtils {
    
    public static int countNumMolsInSDF(String filename){
        Iterator<IAtomContainer> f = parseSDFOnline(filename);
        int count = 0;
        while(f.hasNext()){
            count++;
        }
        return count;
    }
    
    /**
     * Estimates the number of molecules in an sdf file using size. Intentionally gives a slightly high estimate.
     * @param filename
     * @return 
     */
    public static int estimateNumMolsInSDF(String filename){
        File f = new File(filename);
        return (int) (f.length() / 6500);
    }
    
    public static List<IMolStruct> parseSDFAsStructs(String filename, MoleculeStructFactory sFactory){
        IteratingSDFReader molecules = null;
        try{
            
        FileInputStream fs = new FileInputStream(filename);
        BufferedReader br = new BufferedReader( new InputStreamReader(fs ));
        molecules =new IteratingSDFReader( br, DefaultChemObjectBuilder.getInstance());
        } catch( IOException e){
            //edu.mit.csail.ammolite.Logger.error("Failed to read file");
            e.printStackTrace();
        }
        
        List<IMolStruct> mols = new ArrayList<IMolStruct>();
        while(molecules.hasNext()){
            mols.add( sFactory.makeMoleculeStruct( molecules.next()));
        }
        
        try {
            molecules.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mols;
    }
	

	
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
		
		try {
            molecules.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
		return mols;
	}
	
	public static List<IAtomContainer> parseSDFSet(List<String> filenames){
	    List<IAtomContainer> allMolecules = new ArrayList<IAtomContainer>();
	    for(String sdf: filenames){
	        allMolecules.addAll( parseSDF(sdf));
	    }
	    return allMolecules;
	}
	
	public static List<IMolStruct> parseSDFSetAsStructs(List<String> filenames, MoleculeStructFactory sFactory){
        List<IMolStruct> allStructs = new ArrayList<IMolStruct>();
        for(String sdf: filenames){
            allStructs.addAll( parseSDFAsStructs(sdf, sFactory));
        }
        return allStructs;
    }
	
	public static Iterator<IAtomContainer> parseSDFOnline(String filename){
		IteratingSDFReader molecules = null;
		try{
			
    		FileInputStream fs = new FileInputStream(filename);
    		BufferedReader br = new BufferedReader( new InputStreamReader(fs ));
    		molecules =new IteratingSDFReader( br, DefaultChemObjectBuilder.getInstance());
		} catch( IOException e){
			Logger.error("Failed to read file: "+filename);
			e.printStackTrace();
		}
		return molecules;
	}
	
	public static void writeToSDF(Collection<? extends IAtomContainer> molecules, String filename){
		try {
			OutputStream stream = new PrintStream(filename);
			SDFWriter writer = new SDFWriter( stream);
			for(IAtomContainer mol: molecules){
					writer.write(mol);
			}
			writer.close();
			stream.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (CDKException e) {
			e.printStackTrace();
			System.exit(1);
		} 
	}
	
	public static String sdfString(IAtomContainer mol){
	    ByteArrayOutputStream stream = new ByteArrayOutputStream();
	    SDFWriter writer = new SDFWriter(stream);
	    try {
            writer.write(mol);
            writer.close();
        } catch (CDKException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
	    
	    return stream.toString();
	}
	
	public static Iterator<IAtomContainer> parseSDFSetOnline(List<String> filenames){
		return new SDFMultiParser(filenames);
	}
	
	public static void printOutSizeTable(List<String> filenames, boolean struct){
	    Iterator<IAtomContainer> mols = parseSDFSetOnline(filenames);
	    
	    while( mols.hasNext()){
	        IAtomContainer mol = mols.next();
	        if(struct){ 
	            mol = new LabeledCyclicStruct(mol);
	        }
	        ID id = null;
	        if( struct){
	            id = MolUtils.getStructID(mol);
	        } else {
	            id = MolUtils.getPubID(mol);
	        }
	        
	        System.out.print(id.toString());
	        System.out.print(":");
	        System.out.println(MolUtils.getAtomCountNoHydrogen(mol));
	    }
	}
}
