package edu.mit.csail.ammolite.utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

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
	String curFile;

	public SDFMultiParser(List<String> _filenames){
	    
		filenames = _filenames.iterator();
		loadNextIterator();
		
	}
	
	private boolean loadNextIterator(){
		if( filenames.hasNext()){
			String nextFile = filenames.next();
			curFile = nextFile;
			currentIterator = (IteratingSDFReader) SDFUtils.parseSDFOnline(nextFile);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean hasNext() {
	    try{ 
    		if( currentIterator.hasNext()){
    			return currentIterator.hasNext();
    		} else {
    		    currentIterator.close();
    		    boolean isNextIterator = loadNextIterator();
    		    if( isNextIterator){
    		        return this.hasNext();
    		    } else {
    		        return false;
    		    }
    		}
    		
	    } catch(IOException ignore){
	        return false;
	    }
	}

	@Override
	public IAtomContainer next() {
		if( this.hasNext()){
		    IAtomContainer nextOne;
		    nextOne = currentIterator.next();
		    return nextOne;
		}
		return null;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
		
	}
	
	@Override
	public String toString(){
	    return currentIterator.toString();
	}

}
