package edu.mit.csail.fmcsj;

import java.util.Map;
import java.util.Set;

public class GraphMCSList extends MCSList<Integer> {
	
	
	public GraphMCSList(GraphMCSList items) {
		super();
		for(int item: items){
			this.push(item);
		}
	}
	
	public GraphMCSList(Set<Integer> items){
		super();
		for(int item: items){
			this.push(item);
		}
	}

	public GraphMCSList() {
		super();
	}

	@Override
	public boolean equals(Object that){
		if(!(that instanceof GraphMCSList)){
			return false;
		}
		GraphMCSList mThat = (GraphMCSList) that;
		if( mThat.size() != this.size()){
			return false;
		}
		return true;
	}
	
	
}
