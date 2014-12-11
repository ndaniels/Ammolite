package edu.mit.csail.ammolite.gui;

import java.io.File;

import javax.swing.SwingUtilities;

import edu.mit.csail.ammolite.search.AmmoliteSearcher;

public class Controller {
    
    private String dbFilename;
    private String queryFilename;
    
    

    public void startSearch(final String sThresh) {
        new Thread(new Runnable(){
            
            public void run(){
                double thresh = Double.parseDouble(sThresh);
                AmmoliteSearcher.search(queryFilename, dbFilename, thresh, -1);
            }
        }).start();
        
    }

    public void loadDBFile(File file) {
        this.dbFilename = file.getAbsolutePath();
        
    }

    public void loadQueryFile(File file) {
        this.queryFilename = file.getAbsolutePath();
        
    }
    
    public static void main(final String[] args){
        
        final Controller c = new Controller();
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                View main = new View(c);

                main.setVisible(true);
            }
        });
    }

}
