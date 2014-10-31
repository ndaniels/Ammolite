package edu.mit.csail.ammolite.utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.IteratingSDFReader;

/**
 * Iterates through every molecule in a set of sdf files.
 * 
 * @author dcdanko
 *
 */
public class SDFMultiParser implements Iterator<IAtomContainer> {
	
	Iterator<String> filenames;
	IteratingSDFReader currentIterator;

	public SDFMultiParser(List<String> _filenames){
		filenames = _filenames.iterator();
		loadNextIterator();
		
	}
	
	private boolean loadNextIterator(){
		if( filenames.hasNext()){
			String nextFile = filenames.next();
			System.out.println("Loading file "+nextFile);
			currentIterator = (IteratingSDFReader) SDFUtils.parseSDFOnline(nextFile);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean hasNext() {
	    try{ 
    		if( currentIterator.hasNext()){
    			return true;
    		} else if( loadNextIterator()){
    		    currentIterator.close();
    			return true;
    		}
    		currentIterator.close();
    		return false;
	    } catch(IOException ignore){}
	    
    	return false;
	}

	@Override
	public IAtomContainer next() {
		if( hasNext()){
			return currentIterator.next();
		}
		return null;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
		
	}

}
