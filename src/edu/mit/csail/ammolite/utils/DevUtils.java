package edu.mit.csail.ammolite.utils;

import java.util.ArrayList;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.CyclicStruct;
import edu.mit.csail.ammolite.compression.MolStruct;

public class DevUtils {

	public static List<MolStruct> convertToStructures(List<IAtomContainer> mols){
		List<MolStruct> structs = new ArrayList<MolStruct>(mols.size());
		for(IAtomContainer mol: mols){
			MolStruct struct = new CyclicStruct(mol);
			structs.add(struct);
		}
		return structs;
	}
	
	public static void makeStructFile(String in, String filename){
		List<IAtomContainer> mols = SDFUtils.parseSDF(in);
		List<? extends IAtomContainer> structs = convertToStructures(mols);
		SDFUtils.writeToSDF(structs, filename);
	}
}
