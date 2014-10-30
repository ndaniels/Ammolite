package edu.mit.csail.ammolite.utils;

import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * Iterates through every molecule in a set of sdf files.
 * 
 * @author dcdanko
 *
 */
public class SDFMultiParser implements Iterator<IAtomContainer> {
	
	Iterator<String> filenames;
	Iterator<IAtomContainer> currentIterator;

	public SDFMultiParser(List<String> _filenames){
		filenames = _filenames.iterator();
		loadNextIterator();
		
	}
	
	private boolean loadNextIterator(){
		if( filenames.hasNext()){
			String nextFile = filenames.next();
			System.out.println("Loading file "+nextFile);
			currentIterator = SDFUtils.parseSDFOnline(nextFile);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean hasNext() {
		if( currentIterator.hasNext()){
			return true;
		} else if( loadNextIterator()){
			return true;
		}
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
