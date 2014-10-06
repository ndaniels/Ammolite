package edu.mit.csail.ammolite.utils;

public class DoubleProgressBar {
    int maxEvents1;
    int currentEvent1 = 0;
    String name1;
    int maxEvents2;
    int currentEvent2 = 0;
    String name2;

    public DoubleProgressBar(String _name1, int _maxEvents1,String _name2, int _maxEvents2) {
        name1 = _name1.substring(0, Math.min(40, _name1.length()));
        maxEvents1 = _maxEvents1;
        name2 = _name2.substring(0, Math.min(40, _name2.length()));
        maxEvents2 = _maxEvents2;
        display();
    }
    
    public void firstEvent(){
        currentEvent1++;
        display();
    }
    
    public void secondEvent(){
        currentEvent2++;
        display();
    }
    
    public void display(){
        System.out.print("\r");
        displayOne(60, currentEvent1, maxEvents1, name1);
        System.out.print(" ");
        displayOne(60, currentEvent2, maxEvents2, name2);
    }
    
    private void displayOne(int width, int currentEvent, int maxEvents, String name){
        int percentageDone = asPercent(currentEvent, maxEvents);
        int percentageWorking = asPercent(currentEvent+1, maxEvents);
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
        System.out.print("]  "+percentageDone+"%");
        
    }
    
    private int asPercent(int numerator, int denominator){
        return  (int) ((100.0 * numerator) / denominator + 0.5);
    }

}
