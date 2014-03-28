package edu.mit.csail.fmcsj;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openscience.cdk.interfaces.IAtomContainer;


public abstract class AbstractMCS {
	protected IAtomContainer smallCompound;
	protected IAtomContainer bigCompound;

	
	public AbstractMCS(IAtomContainer _compoundOne, IAtomContainer _compoundTwo){
		// Compound One is the smaller of the two compounds, by definition.
		if( _compoundOne.getAtomCount() < _compoundTwo.getAtomCount() ){
			smallCompound = _compoundOne;
			bigCompound = _compoundTwo;
		} else {
			smallCompound = _compoundTwo;
			bigCompound = _compoundOne;
		}
	}
	
	public static long getTimeoutMillis(){
		return 1000;
	}
	
	abstract protected void myCalculate();
	abstract protected int mySize();
	abstract public List<IAtomContainer> getSolutions();
	
	public long calculate() throws TimeoutException{
		long startTime = System.currentTimeMillis();
		final Runnable stuffToDo = new Thread() {
		    @Override 
		    public void run() { 		
		    	myCalculate(); 
		    }
		};
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future future = executor.submit(stuffToDo);
		executor.shutdown(); // This does not cancel the already-scheduled task.
		try {
			future.get(getTimeoutMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
			System.exit(1);
		} catch (ExecutionException ee) {
			ee.printStackTrace();
			System.exit(1);
		}
		if (!executor.isTerminated()){
		    executor.shutdownNow();
		}
		return System.currentTimeMillis() - startTime;
	}
	
	public int size(){
		return mySize();
	}
	
	public IAtomContainer getCompoundOne(){
		return smallCompound;
	}
	
	public IAtomContainer getCompoundTwo(){
		return bigCompound;
	}
	
}
