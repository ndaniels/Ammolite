package edu.mit.csail.ammolite.compression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.database.ISDFSet;
import edu.mit.csail.ammolite.database.SDFSet;
import edu.mit.csail.ammolite.database.StructDatabaseCompressor;
import edu.mit.csail.ammolite.database.StructDatabaseCoreData;
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.ucla.sspace.graph.isomorphism.VF2IsomorphismTester;

public class DatabaseCompression {
	
	
	/**
	 * Finds redundant structures in a set of molecules. Useful for compressing query sets.
	 * 
	 * @param molecules
	 * @param sF
	 * @return
	 */
	public static KeyListMap<MolStruct, IAtomContainer> compressMoleculeSet(Collection<IAtomContainer> molecules, MoleculeStructFactory sF){
		KeyListMap<Integer, MolStruct> structsByFinger = new KeyListMap<Integer,MolStruct>(molecules.size());
		KeyListMap<MolStruct, IAtomContainer> out = new KeyListMap<MolStruct, IAtomContainer>(molecules.size());
		for(IAtomContainer q: molecules){
			MolStruct sq = sF.makeMoleculeStruct(q);
			int fingerprint = sq.fingerprint();
			if(structsByFinger.containsKey(fingerprint)){
				VF2IsomorphismTester isoTester = new VF2IsomorphismTester();
				boolean foundMatch = false;
				for(MolStruct candidate:structsByFinger.get(fingerprint)){
					boolean iso = candidate.isIsomorphic(sq, isoTester);
					if( iso){
						foundMatch = true;
						out.add(candidate, q);
						break;
					}
				}
				if(!foundMatch){
					structsByFinger.add(fingerprint, sq);
					out.add(sq, q);
				}
				
			} else {
				structsByFinger.add(fingerprint, sq);
				out.add(sq, q);
			}
		}
		return out;
	}
	
	public static void mergeDatabases(String d1Name, String d2Name, String newDBName){
		StructDatabaseCoreData d1 = StructDatabaseDecompressor.decompressToCoreData(d1Name);
		StructDatabaseCoreData d2 = StructDatabaseDecompressor.decompressToCoreData(d2Name);
		StructDatabaseCoreData newDB = mergeDatabases(d1,d2);
		StructDatabaseCompressor.compress(newDBName, newDB);
	}
	
	public static StructDatabaseCoreData mergeDatabases(StructDatabaseCoreData d1, StructDatabaseCoreData d2){
		if(!d1.VERSION.equals(d2.VERSION)){
			throw new RuntimeException("Databases are not the same version and cannot be merged.");
		}
		if(d1.compressionType != d2.compressionType){
			throw new RuntimeException("Databases do not use the same type of compression and cannot ber merged.");
		}
		
		KeyListMap<Integer, MolStruct> newStructsByFingerprint = mergeStructureTables( d1.structsByFingerprint, d2.structsByFingerprint);
		ISDFSet newSDFSet = mergeSDFSets(d1.files, d2.files);
		StructDatabaseCoreData newDB = new StructDatabaseCoreData(newStructsByFingerprint, newSDFSet, d1.compressionType, d1.VERSION);
		return newDB;
	}
	
	private static ISDFSet mergeSDFSets(ISDFSet files1, ISDFSet files2) {
		List<String> allFiles = new ArrayList<String>();
		for(String filename: files1.getFilenames()){
			if( !allFiles.contains(filename)){
				allFiles.add(filename);
			}
		}
		for(String filename: files2.getFilenames()){
			if( !allFiles.contains(filename)){
				allFiles.add(filename);
			}
		}
		ISDFSet newSDFSet = new SDFSet(allFiles);
		return newSDFSet;
	}

	private static KeyListMap<Integer, MolStruct> mergeStructureTables(KeyListMap<Integer, MolStruct> t1, 
																	KeyListMap<Integer, MolStruct> t2){
		
		KeyListMap<Integer, MolStruct> newStructsByFingerprint = new KeyListMap<Integer, MolStruct>(t1.size());
		
		for(int fingerprint: t1.keySet()){
			if(t2.containsKey(fingerprint)){
				List<MolStruct> mergedList = mergeStructLists(t1.get(fingerprint), 
																t2.get(fingerprint));
				newStructsByFingerprint.put(fingerprint, mergedList);
				
			} else {
				newStructsByFingerprint.put(fingerprint, t1.get(fingerprint));
			}
		}
		
		for(int fingerprint: t2.keySet()){
			if(!t1.containsKey(fingerprint)){
				newStructsByFingerprint.put(fingerprint, t2.get(fingerprint));
			}
		}
		return newStructsByFingerprint;
		
	}

	private static List<MolStruct> mergeStructLists(List<MolStruct> list1, List<MolStruct> list2) {
		List<MolStruct> outList = new ArrayList(list2);
		for(MolStruct struct: list1){
			boolean matched = parallelIsomorphism(struct, list2);
			if(!matched){
				outList.add(struct);
			}
		}
		return outList;
	}
	
	private static boolean parallelIsomorphism(MolStruct structure, List<MolStruct> potentialMatches){

	    List<Callable<Boolean>> callList = new ArrayList<Callable<Boolean>>(potentialMatches.size());
	    final MolStruct fStruct = structure;
	    for (final MolStruct candidate: potentialMatches) {
	    	
	        Callable<Boolean> callable = new Callable<Boolean>() {
	        	
	            public Boolean call() throws Exception {
	            	VF2IsomorphismTester iso_tester = new VF2IsomorphismTester();
	            	boolean iso = candidate.isIsomorphic(fStruct, iso_tester);
	            	if( iso ){
	            		for(String id: fStruct.getIDNums()){
	            			candidate.addID( id);
	            		}
	            		return iso;
	            	}
	                return null;
	            }
	        };
	        callList.add(callable);

	    }
	    Boolean success = ParallelUtils.parallelTimedSingleExecution( callList, 60*1000);
	    if(success == null){
	    	return false;
	    } else {
	    	return true;
	    }

	}

}
