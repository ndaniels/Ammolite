package edu.mit.csail.ammolite.utils;

import java.util.Iterator;
import java.util.List;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.compression.MoleculeStructFactory;

/**
 * Iterates through every molecule in a set of sdf files
 * yielding their structural representations.
 * 
 * @author dcdanko
 *
 */
public class SDFMultiStructParser implements Iterator<MolStruct>{
    SDFMultiParser molIterator;
    MoleculeStructFactory factory;
    
    public SDFMultiStructParser(List<String> filenames, MoleculeStructFactory factory) {
        molIterator = new SDFMultiParser( filenames);
    }

    @Override
    public boolean hasNext() {
        return molIterator.hasNext();
    }

    @Override
    public MolStruct next() {
        if( hasNext()){
            return factory.makeMoleculeStruct( molIterator.next());
        }
        return null;
    }

}
