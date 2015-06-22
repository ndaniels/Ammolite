package edu.mit.csail.ammolite.utils;

public class WallClock {
	long startTime;
	long endTime;
	boolean ended = false;
	String name;

	
	public WallClock(String name){
		this(name, false);
	}
	
	public WallClock(String _name, boolean delayStart){
		name = _name;
		if(!delayStart){
			start();
		}
	}
	
	public void start(){
		startTime = System.currentTimeMillis();
	}
	
	public void start(long _startTime){
		startTime = _startTime;
	}
	
	public void end(){
		ended = true;
		endTime = System.currentTimeMillis();
	}
	
	public void end(long _endTime){
		ended = true;
		endTime = _endTime;
	}
	
	public long elapsedTime(){
		return endTime - startTime;
	}
	
	public String getElapsedString(){
		if(!ended){
			end();
		}
		return "WALL_CLOCK "+name+" "+elapsedTime()+"ms" ;
	}
	public void printElapsed(){

		System.out.println(getElapsedString());
	}
}
