package edu.mit.csail.ammolite.mcs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;


public abstract class AbstractMCS {
	protected IAtomContainer smallCompound;
	protected IAtomContainer bigCompound;
	protected boolean calculated = false;
	protected boolean timedOut = false;

	
	public AbstractMCS(IAtomContainer _compoundOne, IAtomContainer _compoundTwo){
		// Compound One is the smaller of the two compounds, by definition.
		if( _compoundOne.getAtomCount() < _compoundTwo.getAtomCount() ){
			smallCompound = _compoundOne;
			bigCompound = _compoundTwo;
		} else {
			smallCompound = _compoundTwo;
			bigCompound = _compoundOne;
		}
		stripHydrogens();
	}
	
	private void stripHydrogens(){
		smallCompound = new AtomContainer(AtomContainerManipulator.removeHydrogens(smallCompound));
		bigCompound = new AtomContainer(AtomContainerManipulator.removeHydrogens(bigCompound));
	}
	
	public static long standardTimeoutInMillis(){
		return 2*1000;
	}
	
	public boolean calculationTimedOut(){
		return timedOut;
	}
	
	abstract protected void myCalculate();
	abstract protected int mySize();
	abstract protected List<IAtomContainer> myGetSolutions();
	abstract protected IAtomContainer myGetFirstSolution();
	
	public List<IAtomContainer> getSolutions(){
		if(timedOut){
			return new ArrayList<IAtomContainer>();
		}
		return myGetSolutions();
	}
	
	public IAtomContainer getFirstSolution(){
		if(timedOut){
			return new AtomContainer();
		}
		return myGetFirstSolution();
	}
	
	
	
	public long calculate(){
		long startTime = System.currentTimeMillis();
		myCalculate(); 
		return System.currentTimeMillis() - startTime;
	}
	
	public long timedCalculate(){
		return timedCalculate( standardTimeoutInMillis());
	}
	
	public long timedCalculate(long timeoutInMillis){
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
			future.get(timeoutInMillis, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
			System.exit(1);
		} catch (ExecutionException ee) {
			ee.printStackTrace();
			System.exit(1);
		} catch ( TimeoutException te) {
			timedOut = true;
		}
		if (!executor.isTerminated()){
		    executor.shutdownNow();
		}
		return System.currentTimeMillis() - startTime;
	}
	
	public int size(){
		if( timedOut){
			return 0;
		}
		return mySize();
	}
	
	public IAtomContainer getCompoundOne(){
		return smallCompound;
	}
	
	public IAtomContainer getCompoundTwo(){
		return bigCompound;
	}
	
}
