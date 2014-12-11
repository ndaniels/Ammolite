package edu.mit.csail.ammolite.gui;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

public class TextAreaOutputStream extends OutputStream {
    private JTextArea textControl;
    
    /**
     * Creates a new instance of TextAreaOutputStream which writes
     * to the specified instance of javax.swing.JTextArea control.
     *
     * @param control   A reference to the javax.swing.JTextArea
     *                  control to which the output must be redirected
     *                  to.
     */
    public TextAreaOutputStream( JTextArea control ) {
        textControl = control;
    }
    
    /**
     * Writes the specified byte as a character to the 
     * javax.swing.JTextArea.
     *
     * @param   b   The byte to be written as character to the 
     *              JTextArea.
     */
    public void write( int b ) throws IOException {
        char c = (char) b;
        if( c == '\r'){
            try {
                textControl.setText( textControl.getText(0, textControl.getLineStartOffset( textControl.getLineCount()- 1)));
            } catch (BadLocationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        textControl.append( String.valueOf( ( char )b ) );
    }   
}