package edu.mit.csail.ammolite.mcs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

public class Graph {
    Map<Vertex, Vertex[]> connectionMap = new HashMap<Vertex, Vertex[]>();
    Map<Vertex,Integer> vertexToInd = new HashMap<Vertex, Integer>();
    
    int[][] adjacencyMatrix;
    
    public Graph(IAtomContainer mol) {
        Map<IAtom, Vertex> atomToVertex = new HashMap<IAtom,Vertex>();
        adjacencyMatrix = new int[mol.getAtomCount()][mol.getAtomCount()];
        for(IAtom atom: mol.atoms()){
            Vertex v;
            if(!atomToVertex.containsKey(atom)){
                v = new Vertex(atom);
                atomToVertex.put(atom, v);
                vertexToInd.put(v, vertexToInd.keySet().size());
            } else {
                v = atomToVertex.get(atom);
            }
            int vInd = vertexToInd.get(v);
            List<IAtom> connectedAtoms = mol.getConnectedAtomsList(atom);
            Vertex[] connections = new Vertex[connectedAtoms.size()];
            for(int i=0; i<connections.length; i++){
                IAtom nAtom = connectedAtoms.get(i);
                Vertex nV;
                if(!atomToVertex.containsKey(nAtom)){
                    nV = new Vertex(nAtom);
                    atomToVertex.put(nAtom, nV);
                    vertexToInd.put(nV, vertexToInd.keySet().size());
                    
                } else {
                    nV = atomToVertex.get(nAtom);
                }
                connections[i] = nV;
                int nVInd = vertexToInd.get(nV);
                int bondWeight = mol.getBondNumber(atom, nAtom);
                adjacencyMatrix[vInd][nVInd] = bondWeight;
                adjacencyMatrix[nVInd][vInd] = bondWeight;
            }
            connectionMap.put(v, connections);
        }
        checkRep();
    }
    
    public int weight(Vertex v1, Vertex v2){
        int i = vertexToInd.get(v1);
        int j = vertexToInd.get(v2);
        return adjacencyMatrix[i][j];
    }

    public Vertex[] getConnected(Vertex v){
        return connectionMap.get(v);
    }

    public Vertex[] allVertices() {
        return connectionMap.keySet().toArray(new Vertex[0]);
    }
    
    public boolean contains(Vertex v){
        if(!vertexToInd.containsKey(v)){
            return false;
        }
        return true;
    }
    
    private void checkRep(){
        if( connectionMap.size() != vertexToInd.size() ||
                connectionMap.size() != adjacencyMatrix.length ||
                adjacencyMatrix.length != vertexToInd.size() ){
            throw new RuntimeException("Graph representation invariant not met");
        }
    }

}
