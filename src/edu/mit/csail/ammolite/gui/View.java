package edu.mit.csail.ammolite.gui;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;


public class View extends JFrame {
    
    private final JButton loadQueryFile;
    private final JLabel qFileLabel;
    private final JFileChooser qfc = new JFileChooser();
    
    private final JButton loadDatabaseFile;
    private final JLabel dFileLabel;
    private final JFileChooser dfc = new JFileChooser();
    
    private final JLabel thresholdLabel;
    private final JTextField threshold;
    private final JButton startSearch;
    private final View  self = this;
    private final  Controller c;
    
    // private final List<JRadioButton> databaseButtons;

    public View(Controller _c){
        
        this.c = _c;
        
        loadQueryFile = new JButton("Pick Query File");
        qFileLabel = new JLabel("File: ");
        FileNameExtensionFilter qFilter = new FileNameExtensionFilter("SDF FILES", "sdf");
        qfc.setFileFilter(qFilter);
        
        loadDatabaseFile = new JButton("Pick Database File");
        dFileLabel = new JLabel("File: ");
        FileNameExtensionFilter dFilter = new FileNameExtensionFilter("ADB FILES", "gad");
        dfc.setFileFilter(dFilter);
        dfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        thresholdLabel = new JLabel("Overlap Threshold: ");
        threshold = new JTextField();
        
        startSearch = new JButton("Start Search");
        
        this.loadQueryFile.addActionListener(new ActionListener(){
            
            public void actionPerformed(ActionEvent e){
                int returnVal = qfc.showOpenDialog(self);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = qfc.getSelectedFile();
                    c.loadQueryFile(file);
                    self.qFileLabel.setText("File: " + file.getName());
                }
            }
        });

        this.loadDatabaseFile.addActionListener(new ActionListener(){
    
            public void actionPerformed(ActionEvent e){
                int returnVal = dfc.showOpenDialog(self);
        
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = dfc.getSelectedFile();
                    c.loadDBFile(file);
                    self.dFileLabel.setText("File: " + file.getName());
                }
            }
        });
        
        startSearch.addActionListener( new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                final String thresh = threshold.getText();
                c.startSearch(thresh);
            }
            
        });
        
        this.buildLayout();
        this.pack();
        
    }
    
    private void buildLayout(){
        GroupLayout layout = new GroupLayout( this.getContentPane());
        this.getContentPane().setLayout(layout);
        
        layout.setHorizontalGroup( layout.createParallelGroup()
                .addGroup( layout.createParallelGroup()
                        .addGroup( layout.createSequentialGroup()
                            .addComponent(loadQueryFile)
                            .addComponent(qFileLabel))
                        .addGroup( layout.createSequentialGroup()
                            .addComponent(loadDatabaseFile)
                            .addComponent(dFileLabel))
                            .addComponent(startSearch))
                .addGroup( layout.createSequentialGroup()
                                .addComponent(thresholdLabel)
                                .addComponent(threshold)
                        ));
        
        layout.setVerticalGroup( layout.createSequentialGroup()
                .addGroup( layout.createParallelGroup()
                        .addComponent(loadQueryFile)
                        .addComponent(qFileLabel))
                .addGroup( layout.createParallelGroup()
                        .addComponent(loadDatabaseFile)
                        .addComponent(dFileLabel))
                .addGroup( layout.createParallelGroup()
                        .addComponent(thresholdLabel)
                        .addComponent(threshold))
                 .addComponent(startSearch));
        
    }
    

    
    

}
