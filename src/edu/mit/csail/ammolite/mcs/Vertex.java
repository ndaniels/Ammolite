package edu.mit.csail.ammolite.mcs;

import org.openscience.cdk.interfaces.IAtom;

public class Vertex {
    String label;

    public Vertex(IAtom atom) {
        this.label = atom.getSymbol();
    }

    public boolean matches(Vertex other) {
        return this.label.equals(other.label());
    }

    public String label() {
        return label;
    }
    

}
