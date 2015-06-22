package edu.mit.csail.ammolite.utils;

public class CommandLineProgressBar {
	int maxEvents;
	int currentEvent = 0;
	String name;
	
	public CommandLineProgressBar(String _name, int _maxEvents){
		name = _name;
		maxEvents = _maxEvents;
		display();
	}
	
	public synchronized void event(){
		currentEvent++;
		display();
	}
	
	public void display(){
		display(80);
	}
	
	private void display(int width){
		int percentageDone = asPercent(currentEvent, maxEvents);
		int percentageWorking = asPercent(currentEvent+1, maxEvents);
		System.out.print("\r");
		System.out.print(name);
		System.out.print(" [");
		int barWidth = width - name.length() - 8;
		for(int i=0; i<barWidth; i++){
			int barPercentage = asPercent(i, barWidth);
			if( barPercentage <= percentageDone){
				System.out.print("=");
			} else if (barPercentage <= percentageWorking && percentageWorking <= 100){
				int nextPercentage = asPercent(i+1, barWidth);
				if( nextPercentage > percentageWorking){
					System.out.print("|");
				} else {
					System.out.print("-");
				}
			}  else {
				System.out.print(" ");
			}
		}
		System.out.print("] ");
		System.out.print(String.format( "%,d / %,d", currentEvent, maxEvents));
		
		
	}
	
	public void done(){
		System.out.print("\n");
	}
	
	private int asPercent(int numerator, int denominator){
		return  (int) ((100.0 * numerator) / denominator + 0.5);
	}

}
