package edu.mit.csail.ammolite.database;

import java.util.Iterator;

import edu.mit.csail.ammolite.KeyListMap;
import edu.mit.csail.ammolite.compression.MolStruct;

public class StructIterator implements Iterator<MolStruct> {
	Iterator<MolStruct> currentListIterator;
	Iterator<Integer> keyIterator;
	KeyListMap<Integer, MolStruct> in;
	
	public StructIterator(KeyListMap<Integer, MolStruct> _in){
		in = _in;
		keyIterator = in.keySet().iterator();
		if(keyIterator.hasNext()){
			currentListIterator = in.get(keyIterator.next()).iterator();
		}
	}

	@Override
	public boolean hasNext() {
		if( currentListIterator.hasNext() ){
			return true;
		} else {
			if( keyIterator.hasNext()){
				currentListIterator = in.get(keyIterator.next()).iterator();
				return this.hasNext();
				
			} else {
				return false;
			}
		}
	}

	@Override
	public MolStruct next() {
		if( this.hasNext()){
			return currentListIterator.next();
		} else{
			return null;
		}
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}

}
